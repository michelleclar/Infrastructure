package db

import (
	"context"
	_ "im/ent/migrate"
	"log"
	"testing"
)

func TestCtx(t *testing.T) {
	ctx := context.Background()
	allX := Ctx().client.Client.Messages.Query().AllX(ctx)
	log.Println(allX)
}

func TestQueryContext(t *testing.T) {
	ctx := context.Background()
	q := "select database()"
	queryContext, err := Ctx().QueryContext(ctx, q)
	if err != nil {
		log.Println(err)
	}
	type result struct {
		database string
		table    string
	}
	for queryContext.Next() {
		r := new(result)
		_ = queryContext.Scan(&r.database)
		log.Println(r.database)
	}
	rows, err := Ctx().QueryContext(ctx, "show tables")
	if err != nil {
		log.Println(err)
	}
	for rows.Next() {
		r := new(result)
		_ = rows.Scan(&r.table)
		log.Println(r.table)
	}
}

func TestCreateTable(t *testing.T) {
	//ctx := context.Background()
	//if err := Ctx().Schema.Create(ctx); err != nil {
	//	logger.Fatal("failed creating schema resources", err)
	//}
}

func TestAutomatic(t *testing.T) {
	//if err := Ctx().Schema.Create(context.Background(), migrate.WithDropColumn(true), migrate.WithDropIndex(true), migrate.WithForeignKeys(false)); err != nil {
	//	logger.Fatalf("failed creating schema resources: %v", err)
	//}
}

func TestCreate(t *testing.T) {
	//ctx := context.Background()
	//u, err := Ctx().User.Create().SetAge(30).SetName("a8m").Save(ctx)
	//if err != nil {
	//	logger.Fatalf("failed creating user: %s", err)
	//}
	//logger.Println(u)
}

func TestSearch(t *testing.T) {
	//ctx := context.Background()
	//items, err := Ctx().User.Query().Select(user.FieldUserID).Where(user.AgeEQ(30)).All(ctx)
	//if err != nil {
	//	logger.Fatalf("failed searching users: %s", err)
	//}
	//logger.Println(items)
}

func TestUpdate(t *testing.T) {
	//ctx := context.Background()
	//
	//items := Ctx().User.Query().Select(user.FieldUserID).Where(user.AgeEQ(30)).AllX(ctx)
	//for _, i := range items {
	//	u := Ctx().User.Update().Where(user.ID(i.ID)).SetAge(18).SaveX(ctx)
	//	logger.Println(u)
	//}
}

func TestDelete(t *testing.T) {
	//ctx := context.Background()
	//affected := Ctx().User.Delete().Where(user.ID(123)).ExecX(ctx)
	//logger.Println(affected)
}
