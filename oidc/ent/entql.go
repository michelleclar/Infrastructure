// Code generated by ent, DO NOT EDIT.

package ent

import (
	"oidc/ent/oidcprovider"

	"entgo.io/ent/dialect/sql"
	"entgo.io/ent/dialect/sql/sqlgraph"
	"entgo.io/ent/entql"
	"entgo.io/ent/schema/field"
)

// schemaGraph holds a representation of ent/schema at runtime.
var schemaGraph = func() *sqlgraph.Schema {
	graph := &sqlgraph.Schema{Nodes: make([]*sqlgraph.Node, 1)}
	graph.Nodes[0] = &sqlgraph.Node{
		NodeSpec: sqlgraph.NodeSpec{
			Table:   oidcprovider.Table,
			Columns: oidcprovider.Columns,
			ID: &sqlgraph.FieldSpec{
				Type:   field.TypeInt,
				Column: oidcprovider.FieldID,
			},
		},
		Type: "OidcProvider",
		Fields: map[string]*sqlgraph.FieldSpec{
			oidcprovider.FieldOidcProviderID:          {Type: field.TypeInt64, Column: oidcprovider.FieldOidcProviderID},
			oidcprovider.FieldOidcProviderName:        {Type: field.TypeString, Column: oidcprovider.FieldOidcProviderName},
			oidcprovider.FieldOidcProviderDescription: {Type: field.TypeString, Column: oidcprovider.FieldOidcProviderDescription},
			oidcprovider.FieldIssuer:                  {Type: field.TypeString, Column: oidcprovider.FieldIssuer},
			oidcprovider.FieldProxyURL:                {Type: field.TypeString, Column: oidcprovider.FieldProxyURL},
			oidcprovider.FieldClientID:                {Type: field.TypeString, Column: oidcprovider.FieldClientID},
			oidcprovider.FieldClientSecret:            {Type: field.TypeString, Column: oidcprovider.FieldClientSecret},
			oidcprovider.FieldRedirectURI:             {Type: field.TypeString, Column: oidcprovider.FieldRedirectURI},
			oidcprovider.FieldEndpoints:               {Type: field.TypeJSON, Column: oidcprovider.FieldEndpoints},
		},
	}
	return graph
}()

// predicateAdder wraps the addPredicate method.
// All update, update-one and query builders implement this interface.
type predicateAdder interface {
	addPredicate(func(s *sql.Selector))
}

// addPredicate implements the predicateAdder interface.
func (opq *OidcProviderQuery) addPredicate(pred func(s *sql.Selector)) {
	opq.predicates = append(opq.predicates, pred)
}

// Filter returns a Filter implementation to apply filters on the OidcProviderQuery builder.
func (opq *OidcProviderQuery) Filter() *OidcProviderFilter {
	return &OidcProviderFilter{config: opq.config, predicateAdder: opq}
}

// addPredicate implements the predicateAdder interface.
func (m *OidcProviderMutation) addPredicate(pred func(s *sql.Selector)) {
	m.predicates = append(m.predicates, pred)
}

// Filter returns an entql.Where implementation to apply filters on the OidcProviderMutation builder.
func (m *OidcProviderMutation) Filter() *OidcProviderFilter {
	return &OidcProviderFilter{config: m.config, predicateAdder: m}
}

// OidcProviderFilter provides a generic filtering capability at runtime for OidcProviderQuery.
type OidcProviderFilter struct {
	predicateAdder
	config
}

// Where applies the entql predicate on the query filter.
func (f *OidcProviderFilter) Where(p entql.P) {
	f.addPredicate(func(s *sql.Selector) {
		if err := schemaGraph.EvalP(schemaGraph.Nodes[0].Type, p, s); err != nil {
			s.AddError(err)
		}
	})
}

// WhereID applies the entql int predicate on the id field.
func (f *OidcProviderFilter) WhereID(p entql.IntP) {
	f.Where(p.Field(oidcprovider.FieldID))
}

// WhereOidcProviderID applies the entql int64 predicate on the oidc_provider_id field.
func (f *OidcProviderFilter) WhereOidcProviderID(p entql.Int64P) {
	f.Where(p.Field(oidcprovider.FieldOidcProviderID))
}

// WhereOidcProviderName applies the entql string predicate on the oidc_provider_name field.
func (f *OidcProviderFilter) WhereOidcProviderName(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldOidcProviderName))
}

// WhereOidcProviderDescription applies the entql string predicate on the oidc_provider_description field.
func (f *OidcProviderFilter) WhereOidcProviderDescription(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldOidcProviderDescription))
}

// WhereIssuer applies the entql string predicate on the issuer field.
func (f *OidcProviderFilter) WhereIssuer(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldIssuer))
}

// WhereProxyURL applies the entql string predicate on the proxy_url field.
func (f *OidcProviderFilter) WhereProxyURL(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldProxyURL))
}

// WhereClientID applies the entql string predicate on the client_id field.
func (f *OidcProviderFilter) WhereClientID(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldClientID))
}

// WhereClientSecret applies the entql string predicate on the client_secret field.
func (f *OidcProviderFilter) WhereClientSecret(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldClientSecret))
}

// WhereRedirectURI applies the entql string predicate on the redirect_uri field.
func (f *OidcProviderFilter) WhereRedirectURI(p entql.StringP) {
	f.Where(p.Field(oidcprovider.FieldRedirectURI))
}

// WhereEndpoints applies the entql json.RawMessage predicate on the endpoints field.
func (f *OidcProviderFilter) WhereEndpoints(p entql.BytesP) {
	f.Where(p.Field(oidcprovider.FieldEndpoints))
}
