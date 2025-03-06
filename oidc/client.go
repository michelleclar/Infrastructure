package main

import (
	"crypto/rand"
	"encoding/json"
	"fmt"
	"golang.org/x/net/context"
	"golang.org/x/oauth2"
	"io"
	"net/http"
	"oidc/core"
	"oidc/db"
	log "oidc/log"
	"oidc/utils"
	"strings"
	"time"
)

// 1:context 2: oidc provider 3: verifier 4: auth2 client
var providers map[string]*providerConfig

// TODO: read provider clientId/secret by `database`

type claims struct {
	ScopesSupported []string `json:"scopes_supported"`
	ClaimsSupported []string `json:"claims_supported"`
}

type providerConfig struct {
	ctx      context.Context
	provider *core.Provider
	verifier *core.IDTokenVerifier
	config   *oauth2.Config
}

func init() {
	providers = make(map[string]*providerConfig)
	ctx := context.Background()
	oidcs, err := db.Ctx().OidcProvider.Query().All(ctx)
	if err != nil {
		log.Error(ctx).Err(err).Msg("Filed to load OIDC providers")
	}
	for _, oidc := range oidcs {
		_ctx := context.Background()
		// check is need proxy
		if proxyUrl, ok := utils.ParseUrl(oidc.ProxyURL); ok {
			_ctx = context.WithValue(_ctx, "proxy", proxyUrl)
		}
		// create oidc provider
		provider, err := core.NewProvider(_ctx, oidc.Issuer)

		if err != nil {
			log.Warn(ctx).Err(err).Msgf("init failed %v error detail %v", oidc.Issuer, err)
			continue
		}
		oidcConfig := &core.Config{
			ClientID: oidc.ClientID,
		}

		verifier := provider.Verifier(oidcConfig)
		var p claims
		if err := provider.Claims(&p); err != nil {
			log.Info(ctx).Msgf("Failed to fetch claims for %s: %v", oidc.Issuer, err)
		}
		//TODO: `ip port` read by env
		redirectUrl := "http://127.0.0.1:5556/oidc/callback" + "?platform=" + oidc.OidcProviderName
		config := oauth2.Config{
			ClientID:     oidc.ClientID,
			ClientSecret: oidc.ClientSecret,
			Endpoint:     provider.Endpoint(),
			RedirectURL:  redirectUrl,
			Scopes:       p.ScopesSupported,
		}

		providers[oidc.OidcProviderName] = &providerConfig{
			ctx:      _ctx,
			provider: provider,
			verifier: verifier,
			config:   &config,
		}
	}

}

func randString(nByte int) (string, error) {
	b := make([]byte, nByte)
	if _, err := io.ReadFull(rand.Reader, b); err != nil {
		return "", err
	}
	return fmt.Sprintf("%x", b), nil
	//return base64.RawURLEncoding.EncodeToString(b), nil
}

func setCallbackCookie(w http.ResponseWriter, r *http.Request, name, value string) {
	c := &http.Cookie{
		Name:     name,
		Value:    value,
		MaxAge:   int(time.Hour.Seconds()),
		Secure:   r.TLS != nil,
		HttpOnly: true,
		SameSite: http.SameSiteLaxMode,
	}
	http.SetCookie(w, c)
}

func main() {

	http.HandleFunc("/oidc", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != "GET" {
			http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
			return
		}
		state, err := randString(16)
		platform := strings.TrimSpace(r.URL.Query().Get("platform"))
		if platform == "" {
			http.Error(w, "platform is null", http.StatusBadRequest)
			return
		}
		provider, ok := providers[platform]
		if !ok {
			http.Error(w, fmt.Sprintf("not support %v oidc", platform), http.StatusBadRequest)
			return
		}

		if err != nil {
			http.Error(w, "Internal error", http.StatusInternalServerError)
			return
		}
		nonce, err := randString(16)
		if err != nil {
			http.Error(w, "Internal error", http.StatusInternalServerError)
			return
		}
		setCallbackCookie(w, r, "state", state)
		setCallbackCookie(w, r, "nonce", nonce)

		config := provider.config
		http.Redirect(w, r, config.AuthCodeURL(state, core.Nonce(nonce)), http.StatusFound)
	})

	http.HandleFunc("/oidc/callback", func(w http.ResponseWriter, r *http.Request) {
		state, err := r.Cookie("state")
		if err != nil {
			http.Error(w, "state not found", http.StatusBadRequest)
			return
		}
		if r.URL.Query().Get("state") != state.Value {
			log.Info(context.Background()).Msgf("State mismatch: expected %s, got %s", state.Value, r.URL.Query().Get("state"))
			http.Error(w, "state did not match", http.StatusBadRequest)
			return
		}

		platform := strings.TrimSpace(r.URL.Query().Get("platform"))
		if platform == "" {
			http.Error(w, "platform is null", http.StatusBadRequest)
			return
		}
		provider, ok := providers[platform]
		if !ok {
			http.Error(w, fmt.Sprintf("not support %v oidc", platform), http.StatusBadRequest)
			return
		}

		config := provider.config
		verifier := provider.verifier
		ctx := provider.ctx

		code := r.URL.Query().Get("code")
		if code == "" {
			http.Error(w, "Missing authorization code", http.StatusBadRequest)
			return
		}

		oauth2Token, err := config.Exchange(ctx, code)
		if err != nil {
			http.Error(w, "Failed to exchange token: "+err.Error(), http.StatusInternalServerError)
			return
		}

		rawIDToken, ok := oauth2Token.Extra("id_token").(string)
		if !ok || rawIDToken == "" {
			log.Info(context.Background()).Msgf("ID Token missing from OAuth2 response for platform: %s", platform)
			http.Error(w, "No id_token field in oauth2 token.", http.StatusInternalServerError)
			return
		}
		idToken, err := verifier.Verify(ctx, rawIDToken)
		if err != nil {
			http.Error(w, "Failed to verify ID Token: "+err.Error(), http.StatusInternalServerError)
			return
		}

		nonce, err := r.Cookie("nonce")
		if err != nil {
			http.Error(w, "nonce not found", http.StatusBadRequest)
			return
		}
		if idToken.Nonce != nonce.Value {
			http.Error(w, "nonce did not match", http.StatusBadRequest)
			return
		}

		oauth2Token.AccessToken = "*REDACTED*"

		resp := struct {
			OAuth2Token   *oauth2.Token
			IDTokenClaims *json.RawMessage // ID Token payload is just JSON.
		}{oauth2Token, new(json.RawMessage)}

		if err := idToken.Claims(&resp.IDTokenClaims); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		data, err := json.MarshalIndent(resp, "", "    ")
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		w.Write(data)
	})

	log.Info(context.Background()).Msgf("listening on http://%s/", "127.0.0.1:5556")
	log.Fatal(context.Background()).Err(http.ListenAndServe("127.0.0.1:5556", nil))
	//log.Fatal(http.ListenAndServeTLS("127.0.0.1:5556", "server.crt", "server.key", nil))
}
