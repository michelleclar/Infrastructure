package main

import (
	"golang.org/x/net/context"
	"log"
	"oidc/core"
)

// TODO: read provider clientId/secret by `database`
func init() {

	ctx := context.Background()
	provider, err := core.NewProvider(ctx, "https://accounts.google.com")

	if err != nil {
		log.Fatal(err)
	}
	println(provider)
	//oidcConfig := &core.Config{
	//	ClientID: clientID,
	//}
}
func main() {
}
