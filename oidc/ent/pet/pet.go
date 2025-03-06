// Code generated by ent, DO NOT EDIT.

package pet

import (
	"entgo.io/ent/dialect/sql"
)

const (
	// Label holds the string label denoting the pet type in the database.
	Label = "pet"
	// FieldID holds the string denoting the id field in the database.
	FieldID = "id"
	// FieldPetID holds the string denoting the pet_id field in the database.
	FieldPetID = "pet_id"
	// Table holds the table name of the pet in the database.
	Table = "pets"
)

// Columns holds all SQL columns for pet fields.
var Columns = []string{
	FieldID,
	FieldPetID,
}

// ValidColumn reports if the column name is valid (part of the table columns).
func ValidColumn(column string) bool {
	for i := range Columns {
		if column == Columns[i] {
			return true
		}
	}
	return false
}

// OrderOption defines the ordering options for the Pet queries.
type OrderOption func(*sql.Selector)

// ByID orders the results by the id field.
func ByID(opts ...sql.OrderTermOption) OrderOption {
	return sql.OrderByField(FieldID, opts...).ToFunc()
}

// ByPetID orders the results by the pet_id field.
func ByPetID(opts ...sql.OrderTermOption) OrderOption {
	return sql.OrderByField(FieldPetID, opts...).ToFunc()
}
