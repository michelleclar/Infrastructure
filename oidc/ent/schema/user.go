package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/schema/field"
	"time"
)

// User holds the schema definition for the User entity.
type User struct {
	ent.Schema
}

//func (User) Annotations() []schema.Annotation {
//	return []schema.Annotation{
//		entsql.Schema("db3"),
//	}
//}

// Fields of the User.
func (User) Fields() []ent.Field {
	return []ent.Field{field.Int64("user_id").Unique().DefaultFunc(func() int64 {
		return time.Now().Unix() << 8
	}), field.Int("age").Positive(), field.String("name").Default("unknown")}
}

// Edges of the User.
func (User) Edges() []ent.Edge {
	return nil
}
