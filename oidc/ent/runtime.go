// Code generated by ent, DO NOT EDIT.

package ent

import (
	"oidc/ent/oidcprovider"
	"oidc/ent/schema"
	"oidc/ent/user"
)

// The init function reads all schema descriptors with runtime code
// (default values, validators, hooks and policies) and stitches it
// to their package variables.
func init() {
	oidcproviderFields := schema.OidcProvider{}.Fields()
	_ = oidcproviderFields
	// oidcproviderDescOidcProviderID is the schema descriptor for oidc_provider_id field.
	oidcproviderDescOidcProviderID := oidcproviderFields[0].Descriptor()
	// oidcprovider.DefaultOidcProviderID holds the default value on creation for the oidc_provider_id field.
	oidcprovider.DefaultOidcProviderID = oidcproviderDescOidcProviderID.Default.(func() int64)
	// oidcproviderDescOidcProviderName is the schema descriptor for oidc_provider_name field.
	oidcproviderDescOidcProviderName := oidcproviderFields[1].Descriptor()
	// oidcprovider.DefaultOidcProviderName holds the default value on creation for the oidc_provider_name field.
	oidcprovider.DefaultOidcProviderName = oidcproviderDescOidcProviderName.Default.(string)
	// oidcproviderDescIssuer is the schema descriptor for issuer field.
	oidcproviderDescIssuer := oidcproviderFields[3].Descriptor()
	// oidcprovider.IssuerValidator is a validator for the "issuer" field. It is called by the builders before save.
	oidcprovider.IssuerValidator = oidcproviderDescIssuer.Validators[0].(func(string) error)
	userFields := schema.User{}.Fields()
	_ = userFields
	// userDescUserID is the schema descriptor for user_id field.
	userDescUserID := userFields[0].Descriptor()
	// user.DefaultUserID holds the default value on creation for the user_id field.
	user.DefaultUserID = userDescUserID.Default.(func() int64)
	// userDescAge is the schema descriptor for age field.
	userDescAge := userFields[1].Descriptor()
	// user.AgeValidator is a validator for the "age" field. It is called by the builders before save.
	user.AgeValidator = userDescAge.Validators[0].(func(int) error)
	// userDescName is the schema descriptor for name field.
	userDescName := userFields[2].Descriptor()
	// user.DefaultName holds the default value on creation for the name field.
	user.DefaultName = userDescName.Default.(string)
}
