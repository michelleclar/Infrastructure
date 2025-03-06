package db

import (
	"context"
	"log"
	"oidc/ent/migrate"
	"oidc/ent/user"
	"testing"
)

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

func TestAutomatic(t *testing.T) {
	if err := Ctx().Schema.Create(context.Background(), migrate.WithDropColumn(true), migrate.WithDropIndex(true), migrate.WithForeignKeys(false)); err != nil {
		log.Fatalf("failed creating schema resources: %v", err)
	}
}

func TestCreate(t *testing.T) {
	ctx := context.Background()
	u, err := Ctx().User.Create().SetAge(30).SetName("a8m").Save(ctx)
	if err != nil {
		log.Fatalf("failed creating user: %s", err)
	}
	log.Println(u)
}

func TestSearch(t *testing.T) {
	ctx := context.Background()
	items, err := Ctx().User.Query().Select(user.FieldUserID).Where(user.AgeEQ(30)).All(ctx)
	if err != nil {
		log.Fatalf("failed searching users: %s", err)
	}
	log.Println(items)
}

func TestUpdate(t *testing.T) {
	ctx := context.Background()

	items := Ctx().User.Query().Select(user.FieldUserID).Where(user.AgeEQ(30)).AllX(ctx)
	for _, i := range items {
		u := Ctx().User.Update().Where(user.ID(i.ID)).SetAge(18).SaveX(ctx)
		log.Println(u)
	}
}

func TestDelete(t *testing.T) {
	ctx := context.Background()
	affected := Ctx().User.Delete().Where(user.ID(123)).ExecX(ctx)
	log.Println(affected)
}
