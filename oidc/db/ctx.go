package db

import (
	"database/sql"
	"fmt"
	"oidc/ent"
	"os"
	"time"

	"entgo.io/ent/dialect"
	entsql "entgo.io/ent/dialect/sql"
	_ "github.com/jackc/pgx/v5/stdlib"
)

var db *sql.DB

var (
	//databaseHost     = os.Getenv("DATABASE_HOST")
	//databasePort     = os.Getenv("DATABASE_PORT")
	//databaseUsername = os.Getenv("DATABASE_USERNAME")
	//databasePassword = os.Getenv("DATABASE_PASSWORD")
	//databaseDbname   = os.Getenv("DATABASE_DBNAME")
	databaseUrl = If(os.Getenv("DATABASE_URL") == "", "postgresql://root:root@127.0.0.1:15432/db", os.Getenv("DATABASE_URL")).(string) //postgresql://user:password@127.0.0.1/database
)

func If(condition bool, trueVal, falseVal interface{}) interface{} {
	if condition {
		return trueVal
	}
	return falseVal
}

func initDB() {
	var err error
	db, err = sql.Open("pgx", databaseUrl)
	if err != nil {
		panic(fmt.Sprintf("failed init database : %v", err))
	}
	db.SetMaxIdleConns(10)
	db.SetMaxOpenConns(100)
	db.SetConnMaxLifetime(time.Hour)
}

func getDB() *sql.DB {
	if db == nil {
		initDB()
	}
	return db
}

func getClient() *ent.Client {
	db := getDB()
	drv := entsql.OpenDB(dialect.Postgres, db)
	schema := ent.AlternateSchema(ent.SchemaConfig{
		OidcProvider: "oidc",
	})
	return ent.NewClient(ent.Driver(drv), schema)
}

func Ctx() *ent.Client {
	client := getClient()
	return client.Debug()
	//return client
}
