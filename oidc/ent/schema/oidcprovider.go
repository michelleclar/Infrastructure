package schema

import (
	"time"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
)

// OidcProvider holds the schema definition for the OidcProvider entity.
type OidcProvider struct {
	ent.Schema
}

// Fields of the OidcProvider.
func (OidcProvider) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("oidc_provider_id").Comment("OidcProvider ID").Unique().DefaultFunc(func() int64 {
			//TODO:Clock back
			return time.Now().Unix() << 8
		}).Immutable(),
		field.String("oidc_provider_name").Default("unknown").Comment("OidcProvider Name"),
		field.String("oidc_provider_description").Comment("OidcProvider Description").Optional(),
		field.String("issuer").NotEmpty().Comment("issuer"),
		field.String("proxy_url").Comment("proxy URL").Optional(),
		field.String("client_id").Comment("Client ID"),
		field.String("client_secret").Comment("Client Secret"),
		// TODO: need sport many redirect
		field.String("redirect_uri").Comment("Redirect URI"),
		field.Strings("endpoints").Comment("OidcProvider Endpoints").Optional(),
	}
}

// Annotations of the OidcProvider.
func (OidcProvider) Annotations() []schema.Annotation {
	return []schema.Annotation{
		entsql.Annotation{Table: "oidc provider", Schema: "oidc"},
		entsql.Schema("oidc"),
		entsql.WithComments(true),
	}
}

//func (OidcProvider) Annotations() []schema.Annotation {
//	return []schema.Annotation{
//		entsql.WithComments(true),
//		schema.Comment("OidcProvider"),
//	}
//}

// Edges of the OidcProvider.
func (OidcProvider) Edges() []ent.Edge {
	return nil
}

func (OidcProvider) Indexes() []ent.Index {
	return []ent.Index{index.Fields("oidc_provider_id").Unique()}
}
