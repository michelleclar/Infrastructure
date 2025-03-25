package db

import (
	"context"
	"database/sql"
	"entgo.io/ent/dialect"
	entsql "entgo.io/ent/dialect/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"github.com/uptrace/opentelemetry-go-extra/otelsql"
	semconv "go.opentelemetry.io/otel/semconv/v1.10.0"
	"im/ent"
	"im/ent/migrate"
	"log"
	"os"
	"time"
)

var (
	//databaseHost     = os.Getenv("DATABASE_HOST")
	//databasePort     = os.Getenv("DATABASE_PORT")
	//databaseUsername = os.Getenv("DATABASE_USERNAME")
	//databasePassword = os.Getenv("DATABASE_PASSWORD")
	//databaseDbname   = os.Getenv("DATABASE_DBNAME")
	dsn = If(os.Getenv("DSN") == "", "root:dzsdatabase@2024!@tcp(192.168.111.32:13306)/xjrc365?parseTime=true", os.Getenv("DSN")).(string) //postgresql://user:password@127.0.0.1/database
)

var client *Client
var ctx Context

// Context TODO: need test thread is safe
type Context struct {
	client *Client
}

type Client struct {
	*ent.Client
	*sql.DB
}

func init() {
	client = open(dsn)
	ctx = Context{client: client}
	if err := client.Schema.Create(context.Background(), migrate.WithDropIndex(true), migrate.WithDropColumn(true), migrate.WithForeignKeys(false)); err != nil {
		log.Fatalf("failed creating schema resources: %v", err)
	}
}

func If(condition bool, trueVal, falseVal interface{}) interface{} {
	if condition {
		return trueVal
	}
	return falseVal
}
func (ctx Context) GetClient() ent.Client {
	return *ctx.client.Client
}

func (ctx Context) QueryContext(context context.Context, sql string, args ...any) (*sql.Rows, error) {
	result, err := ctx.client.QueryContext(context, sql)
	if err != nil {
		return nil, err
	}

	return result, nil

}

func (ctx Context) ExecContext(context context.Context, q string) (interface{}, interface{}) {
	result, err := ctx.client.ExecContext(context, q)
	if err != nil {
		return nil, err
	}
	return result, nil
}

func (ctx Context) Tx(_ctx context.Context) *ent.Tx {
	tx, err := ctx.client.Tx(_ctx)
	if err != nil {
		panic(err)
	}
	return tx
}

func (ctx Context) WithTx(_ctx context.Context, fn func(tx *ent.Tx) error) error {
	tx, err := ctx.client.Tx(_ctx)
	if err != nil {
		return err
	}
	defer func() {
		if v := recover(); v != nil {
			tx.Rollback()
			panic(v)
		}
	}()
	if err := fn(tx); err != nil {
		if rerr := tx.Rollback(); rerr != nil {
			err = fmt.Errorf("%w: rolling back transaction: %v", err, rerr)
		}
		return err
	}
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("committing transaction: %w", err)
	}
	return nil
}

func Ctx() Context {
	return ctx
}

func open(dsn string) *Client {
	db, err := otelsql.Open("mysql", dsn, otelsql.WithAttributes(semconv.DBSystemMSSQL), otelsql.WithDBName("db"))
	if err != nil {
		panic(err)
	}
	db.SetMaxIdleConns(10)
	db.SetMaxOpenConns(100)
	db.SetConnMaxLifetime(time.Hour)
	drv := entsql.OpenDB(dialect.MySQL, db)
	client := &Client{
		Client: ent.NewClient(ent.Driver(drv)).Debug(),
		DB:     db,
	}
	return client
}
