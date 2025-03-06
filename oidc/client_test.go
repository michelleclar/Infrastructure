package main

import (
	"golang.org/x/net/context"
	"oidc/db"
	"testing"
)

func TestCreateProvider(t *testing.T) {
	db.Ctx().OidcProvider.Create().SetIssuer("https://accounts.google.com").SetClientID("dsadasdasd").SetClientSecret("dsadasdas").SetRedirectURI("dsad").SaveX(context.Background())

}
