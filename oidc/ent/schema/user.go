package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
)

// User holds the schema definition for the User entity.
type User struct {
	ent.Schema
}

func (User) Annotations() []schema.Annotation {
	return []schema.Annotation{
		entsql.Schema("db3"),
	}
}

// Fields of the User.
func (User) Fields() []ent.Field {
	return []ent.Field{field.Int32("user_id").Unique(), field.Int("age").Positive(), field.String("name").Default("unknown")}
}

// Edges of the User.
func (User) Edges() []ent.Edge {
	return nil
}
