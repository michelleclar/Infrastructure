package db

import (
	"context"
	"log"
	"testing"
)

func TestProcessString(t *testing.T) {
	//str := ProcessString("hello %v", "workd")
	//println(str)
}
func TestCtx(t *testing.T) {
	ctx := context.Background()
	users, err := Ctx().User.Query().All(ctx)
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
