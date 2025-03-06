// Code generated by ent, DO NOT EDIT.

package ent

import (
	"context"
	"oidc/ent/oidcprovider"
	"oidc/ent/predicate"

	"entgo.io/ent/dialect/sql"
	"entgo.io/ent/dialect/sql/sqlgraph"
	"entgo.io/ent/schema/field"
)

// OidcProviderDelete is the builder for deleting a OidcProvider entity.
type OidcProviderDelete struct {
	config
	hooks    []Hook
	mutation *OidcProviderMutation
}

// Where appends a list predicates to the OidcProviderDelete builder.
func (opd *OidcProviderDelete) Where(ps ...predicate.OidcProvider) *OidcProviderDelete {
	opd.mutation.Where(ps...)
	return opd
}

// Exec executes the deletion query and returns how many vertices were deleted.
func (opd *OidcProviderDelete) Exec(ctx context.Context) (int, error) {
	return withHooks(ctx, opd.sqlExec, opd.mutation, opd.hooks)
}

// ExecX is like Exec, but panics if an error occurs.
func (opd *OidcProviderDelete) ExecX(ctx context.Context) int {
	n, err := opd.Exec(ctx)
	if err != nil {
		panic(err)
	}
	return n
}

func (opd *OidcProviderDelete) sqlExec(ctx context.Context) (int, error) {
	_spec := sqlgraph.NewDeleteSpec(oidcprovider.Table, sqlgraph.NewFieldSpec(oidcprovider.FieldID, field.TypeInt))
	if ps := opd.mutation.predicates; len(ps) > 0 {
		_spec.Predicate = func(selector *sql.Selector) {
			for i := range ps {
				ps[i](selector)
			}
		}
	}
	affected, err := sqlgraph.DeleteNodes(ctx, opd.driver, _spec)
	if err != nil && sqlgraph.IsConstraintError(err) {
		err = &ConstraintError{msg: err.Error(), wrap: err}
	}
	opd.mutation.done = true
	return affected, err
}

// OidcProviderDeleteOne is the builder for deleting a single OidcProvider entity.
type OidcProviderDeleteOne struct {
	opd *OidcProviderDelete
}

// Where appends a list predicates to the OidcProviderDelete builder.
func (opdo *OidcProviderDeleteOne) Where(ps ...predicate.OidcProvider) *OidcProviderDeleteOne {
	opdo.opd.mutation.Where(ps...)
	return opdo
}

// Exec executes the deletion query.
func (opdo *OidcProviderDeleteOne) Exec(ctx context.Context) error {
	n, err := opdo.opd.Exec(ctx)
	switch {
	case err != nil:
		return err
	case n == 0:
		return &NotFoundError{oidcprovider.Label}
	default:
		return nil
	}
}

// ExecX is like Exec, but panics if an error occurs.
func (opdo *OidcProviderDeleteOne) ExecX(ctx context.Context) {
	if err := opdo.Exec(ctx); err != nil {
		panic(err)
	}
}
