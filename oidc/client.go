package main

import (
	"context"
	"log"
	"oidc/db"
)

// TODO: read provider clientId/secret by `database`
func init() {
	ctx := context.Background()
	users, err := db.Ctx().User.Query().All(ctx)
	if err != nil {
		log.Fatal(err)
	}
	log.Println(users)
}
func main() {
}
