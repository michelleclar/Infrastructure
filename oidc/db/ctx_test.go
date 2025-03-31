package db

import (
	"context"
	"log"
	"oidc/ent/migrate"
	"testing"
)

func TestCtx(t *testing.T) {
	ctx := context.Background()
	users, err := Ctx().OidcProvider.Query().All(ctx)
	if err != nil {
		log.Fatal(err)
	}
	log.Println(users)
}

func TestCreateTable(t *testing.T) {
	ctx := context.Background()
	if err := Ctx().Schema.Create(ctx); err != nil {
		log.Fatal("failed creating schema resources", err)
	}
}

func TestAutomatic(t *testing.T) {
	if err := Ctx().Schema.Create(context.Background(), migrate.WithDropColumn(true), migrate.WithDropIndex(true), migrate.WithForeignKeys(false)); err != nil {
		log.Fatalf("failed creating schema resources: %v", err)
	}
}
