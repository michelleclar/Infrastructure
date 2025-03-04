// Code generated by ent, DO NOT EDIT.

package migrate

import (
	"entgo.io/ent/dialect/sql/schema"
	"entgo.io/ent/schema/field"
)

var (
	// OidcProvidersColumns holds the columns for the "oidc_providers" table.
	OidcProvidersColumns = []*schema.Column{
		{Name: "id", Type: field.TypeInt, Increment: true},
	}
	// OidcProvidersTable holds the schema information for the "oidc_providers" table.
	OidcProvidersTable = &schema.Table{
		Name:       "oidc_providers",
		Columns:    OidcProvidersColumns,
		PrimaryKey: []*schema.Column{OidcProvidersColumns[0]},
	}
	// PetsColumns holds the columns for the "pets" table.
	PetsColumns = []*schema.Column{
		{Name: "id", Type: field.TypeInt, Increment: true},
	}
	// PetsTable holds the schema information for the "pets" table.
	PetsTable = &schema.Table{
		Name:       "pets",
		Columns:    PetsColumns,
		PrimaryKey: []*schema.Column{PetsColumns[0]},
	}
	// UsersColumns holds the columns for the "users" table.
	UsersColumns = []*schema.Column{
		{Name: "id", Type: field.TypeInt, Increment: true},
		{Name: "user_id", Type: field.TypeInt32, Unique: true},
		{Name: "age", Type: field.TypeInt},
		{Name: "name", Type: field.TypeString, Default: "unknown"},
	}
	// UsersTable holds the schema information for the "users" table.
	UsersTable = &schema.Table{
		Name:       "users",
		Columns:    UsersColumns,
		PrimaryKey: []*schema.Column{UsersColumns[0]},
	}
	// Tables holds all the tables in the schema.
	Tables = []*schema.Table{
		OidcProvidersTable,
		PetsTable,
		UsersTable,
	}
)

func init() {
}
