package db

import (
	"database/sql"
	"entgo.io/ent/dialect"
	entsql "entgo.io/ent/dialect/sql"
	"fmt"
	_ "github.com/jackc/pgx/v5/stdlib"
	"oidc/ent"
	"os"
	"time"
)

var db *sql.DB

var (
	//databaseHost     = os.Getenv("DATABASE_HOST")
	//databasePort     = os.Getenv("DATABASE_PORT")
	//databaseUsername = os.Getenv("DATABASE_USERNAME")
	//databasePassword = os.Getenv("DATABASE_PASSWORD")
	//databaseDbname   = os.Getenv("DATABASE_DBNAME")
	databaseUrl = If(os.Getenv("DATABASE_URL") == "", "postgresql://root:root@192.168.111.34:15432/db", os.Getenv("DATABASE_URL")).(string) //postgresql://user:password@127.0.0.1/database
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
	return ent.NewClient(ent.Driver(drv))
}

func Ctx() *ent.Client {
	client := getClient()
	return client.Debug()
	//return client
}

//
//func ProcessString(str string, vars interface{}) string {
//	tmpl, err := template.New("tmpl").Parse(str)
//
//	if err != nil {
//		panic(err)
//	}
//	return process(tmpl, vars)
//}
//func process(t *template.Template, vars interface{}) string {
//	var tmplBytes bytes.Buffer
//
//	err := t.Execute(&tmplBytes, vars)
//	if err != nil {
//		panic(err)
//	}
//	return tmplBytes.String()
//}
