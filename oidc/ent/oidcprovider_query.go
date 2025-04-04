// Code generated by ent, DO NOT EDIT.

package ent

import (
	"context"
	"fmt"
	"math"
	"oidc/ent/internal"
	"oidc/ent/oidcprovider"
	"oidc/ent/predicate"

	"entgo.io/ent"
	"entgo.io/ent/dialect/sql"
	"entgo.io/ent/dialect/sql/sqlgraph"
	"entgo.io/ent/schema/field"
)

// OidcProviderQuery is the builder for querying OidcProvider entities.
type OidcProviderQuery struct {
	config
	ctx        *QueryContext
	order      []oidcprovider.OrderOption
	inters     []Interceptor
	predicates []predicate.OidcProvider
	modifiers  []func(*sql.Selector)
	// intermediate query (i.e. traversal path).
	sql  *sql.Selector
	path func(context.Context) (*sql.Selector, error)
}

// Where adds a new predicate for the OidcProviderQuery builder.
func (opq *OidcProviderQuery) Where(ps ...predicate.OidcProvider) *OidcProviderQuery {
	opq.predicates = append(opq.predicates, ps...)
	return opq
}

// Limit the number of records to be returned by this query.
func (opq *OidcProviderQuery) Limit(limit int) *OidcProviderQuery {
	opq.ctx.Limit = &limit
	return opq
}

// Offset to start from.
func (opq *OidcProviderQuery) Offset(offset int) *OidcProviderQuery {
	opq.ctx.Offset = &offset
	return opq
}

// Unique configures the query builder to filter duplicate records on query.
// By default, unique is set to true, and can be disabled using this method.
func (opq *OidcProviderQuery) Unique(unique bool) *OidcProviderQuery {
	opq.ctx.Unique = &unique
	return opq
}

// Order specifies how the records should be ordered.
func (opq *OidcProviderQuery) Order(o ...oidcprovider.OrderOption) *OidcProviderQuery {
	opq.order = append(opq.order, o...)
	return opq
}

// First returns the first OidcProvider entity from the query.
// Returns a *NotFoundError when no OidcProvider was found.
func (opq *OidcProviderQuery) First(ctx context.Context) (*OidcProvider, error) {
	nodes, err := opq.Limit(1).All(setContextOp(ctx, opq.ctx, ent.OpQueryFirst))
	if err != nil {
		return nil, err
	}
	if len(nodes) == 0 {
		return nil, &NotFoundError{oidcprovider.Label}
	}
	return nodes[0], nil
}

// FirstX is like First, but panics if an error occurs.
func (opq *OidcProviderQuery) FirstX(ctx context.Context) *OidcProvider {
	node, err := opq.First(ctx)
	if err != nil && !IsNotFound(err) {
		panic(err)
	}
	return node
}

// FirstID returns the first OidcProvider ID from the query.
// Returns a *NotFoundError when no OidcProvider ID was found.
func (opq *OidcProviderQuery) FirstID(ctx context.Context) (id int, err error) {
	var ids []int
	if ids, err = opq.Limit(1).IDs(setContextOp(ctx, opq.ctx, ent.OpQueryFirstID)); err != nil {
		return
	}
	if len(ids) == 0 {
		err = &NotFoundError{oidcprovider.Label}
		return
	}
	return ids[0], nil
}

// FirstIDX is like FirstID, but panics if an error occurs.
func (opq *OidcProviderQuery) FirstIDX(ctx context.Context) int {
	id, err := opq.FirstID(ctx)
	if err != nil && !IsNotFound(err) {
		panic(err)
	}
	return id
}

// Only returns a single OidcProvider entity found by the query, ensuring it only returns one.
// Returns a *NotSingularError when more than one OidcProvider entity is found.
// Returns a *NotFoundError when no OidcProvider entities are found.
func (opq *OidcProviderQuery) Only(ctx context.Context) (*OidcProvider, error) {
	nodes, err := opq.Limit(2).All(setContextOp(ctx, opq.ctx, ent.OpQueryOnly))
	if err != nil {
		return nil, err
	}
	switch len(nodes) {
	case 1:
		return nodes[0], nil
	case 0:
		return nil, &NotFoundError{oidcprovider.Label}
	default:
		return nil, &NotSingularError{oidcprovider.Label}
	}
}

// OnlyX is like Only, but panics if an error occurs.
func (opq *OidcProviderQuery) OnlyX(ctx context.Context) *OidcProvider {
	node, err := opq.Only(ctx)
	if err != nil {
		panic(err)
	}
	return node
}

// OnlyID is like Only, but returns the only OidcProvider ID in the query.
// Returns a *NotSingularError when more than one OidcProvider ID is found.
// Returns a *NotFoundError when no entities are found.
func (opq *OidcProviderQuery) OnlyID(ctx context.Context) (id int, err error) {
	var ids []int
	if ids, err = opq.Limit(2).IDs(setContextOp(ctx, opq.ctx, ent.OpQueryOnlyID)); err != nil {
		return
	}
	switch len(ids) {
	case 1:
		id = ids[0]
	case 0:
		err = &NotFoundError{oidcprovider.Label}
	default:
		err = &NotSingularError{oidcprovider.Label}
	}
	return
}

// OnlyIDX is like OnlyID, but panics if an error occurs.
func (opq *OidcProviderQuery) OnlyIDX(ctx context.Context) int {
	id, err := opq.OnlyID(ctx)
	if err != nil {
		panic(err)
	}
	return id
}

// All executes the query and returns a list of OidcProviders.
func (opq *OidcProviderQuery) All(ctx context.Context) ([]*OidcProvider, error) {
	ctx = setContextOp(ctx, opq.ctx, ent.OpQueryAll)
	if err := opq.prepareQuery(ctx); err != nil {
		return nil, err
	}
	qr := querierAll[[]*OidcProvider, *OidcProviderQuery]()
	return withInterceptors[[]*OidcProvider](ctx, opq, qr, opq.inters)
}

// AllX is like All, but panics if an error occurs.
func (opq *OidcProviderQuery) AllX(ctx context.Context) []*OidcProvider {
	nodes, err := opq.All(ctx)
	if err != nil {
		panic(err)
	}
	return nodes
}

// IDs executes the query and returns a list of OidcProvider IDs.
func (opq *OidcProviderQuery) IDs(ctx context.Context) (ids []int, err error) {
	if opq.ctx.Unique == nil && opq.path != nil {
		opq.Unique(true)
	}
	ctx = setContextOp(ctx, opq.ctx, ent.OpQueryIDs)
	if err = opq.Select(oidcprovider.FieldID).Scan(ctx, &ids); err != nil {
		return nil, err
	}
	return ids, nil
}

// IDsX is like IDs, but panics if an error occurs.
func (opq *OidcProviderQuery) IDsX(ctx context.Context) []int {
	ids, err := opq.IDs(ctx)
	if err != nil {
		panic(err)
	}
	return ids
}

// Count returns the count of the given query.
func (opq *OidcProviderQuery) Count(ctx context.Context) (int, error) {
	ctx = setContextOp(ctx, opq.ctx, ent.OpQueryCount)
	if err := opq.prepareQuery(ctx); err != nil {
		return 0, err
	}
	return withInterceptors[int](ctx, opq, querierCount[*OidcProviderQuery](), opq.inters)
}

// CountX is like Count, but panics if an error occurs.
func (opq *OidcProviderQuery) CountX(ctx context.Context) int {
	count, err := opq.Count(ctx)
	if err != nil {
		panic(err)
	}
	return count
}

// Exist returns true if the query has elements in the graph.
func (opq *OidcProviderQuery) Exist(ctx context.Context) (bool, error) {
	ctx = setContextOp(ctx, opq.ctx, ent.OpQueryExist)
	switch _, err := opq.FirstID(ctx); {
	case IsNotFound(err):
		return false, nil
	case err != nil:
		return false, fmt.Errorf("ent: check existence: %w", err)
	default:
		return true, nil
	}
}

// ExistX is like Exist, but panics if an error occurs.
func (opq *OidcProviderQuery) ExistX(ctx context.Context) bool {
	exist, err := opq.Exist(ctx)
	if err != nil {
		panic(err)
	}
	return exist
}

// Clone returns a duplicate of the OidcProviderQuery builder, including all associated steps. It can be
// used to prepare common query builders and use them differently after the clone is made.
func (opq *OidcProviderQuery) Clone() *OidcProviderQuery {
	if opq == nil {
		return nil
	}
	return &OidcProviderQuery{
		config:     opq.config,
		ctx:        opq.ctx.Clone(),
		order:      append([]oidcprovider.OrderOption{}, opq.order...),
		inters:     append([]Interceptor{}, opq.inters...),
		predicates: append([]predicate.OidcProvider{}, opq.predicates...),
		// clone intermediate query.
		sql:       opq.sql.Clone(),
		path:      opq.path,
		modifiers: append([]func(*sql.Selector){}, opq.modifiers...),
	}
}

// GroupBy is used to group vertices by one or more fields/columns.
// It is often used with aggregate functions, like: count, max, mean, min, sum.
//
// Example:
//
//	var v []struct {
//		OidcProviderID int64 `json:"oidc_provider_id,omitempty"`
//		Count int `json:"count,omitempty"`
//	}
//
//	client.OidcProvider.Query().
//		GroupBy(oidcprovider.FieldOidcProviderID).
//		Aggregate(ent.Count()).
//		Scan(ctx, &v)
func (opq *OidcProviderQuery) GroupBy(field string, fields ...string) *OidcProviderGroupBy {
	opq.ctx.Fields = append([]string{field}, fields...)
	grbuild := &OidcProviderGroupBy{build: opq}
	grbuild.flds = &opq.ctx.Fields
	grbuild.label = oidcprovider.Label
	grbuild.scan = grbuild.Scan
	return grbuild
}

// Select allows the selection one or more fields/columns for the given query,
// instead of selecting all fields in the entity.
//
// Example:
//
//	var v []struct {
//		OidcProviderID int64 `json:"oidc_provider_id,omitempty"`
//	}
//
//	client.OidcProvider.Query().
//		Select(oidcprovider.FieldOidcProviderID).
//		Scan(ctx, &v)
func (opq *OidcProviderQuery) Select(fields ...string) *OidcProviderSelect {
	opq.ctx.Fields = append(opq.ctx.Fields, fields...)
	sbuild := &OidcProviderSelect{OidcProviderQuery: opq}
	sbuild.label = oidcprovider.Label
	sbuild.flds, sbuild.scan = &opq.ctx.Fields, sbuild.Scan
	return sbuild
}

// Aggregate returns a OidcProviderSelect configured with the given aggregations.
func (opq *OidcProviderQuery) Aggregate(fns ...AggregateFunc) *OidcProviderSelect {
	return opq.Select().Aggregate(fns...)
}

func (opq *OidcProviderQuery) prepareQuery(ctx context.Context) error {
	for _, inter := range opq.inters {
		if inter == nil {
			return fmt.Errorf("ent: uninitialized interceptor (forgotten import ent/runtime?)")
		}
		if trv, ok := inter.(Traverser); ok {
			if err := trv.Traverse(ctx, opq); err != nil {
				return err
			}
		}
	}
	for _, f := range opq.ctx.Fields {
		if !oidcprovider.ValidColumn(f) {
			return &ValidationError{Name: f, err: fmt.Errorf("ent: invalid field %q for query", f)}
		}
	}
	if opq.path != nil {
		prev, err := opq.path(ctx)
		if err != nil {
			return err
		}
		opq.sql = prev
	}
	return nil
}

func (opq *OidcProviderQuery) sqlAll(ctx context.Context, hooks ...queryHook) ([]*OidcProvider, error) {
	var (
		nodes = []*OidcProvider{}
		_spec = opq.querySpec()
	)
	_spec.ScanValues = func(columns []string) ([]any, error) {
		return (*OidcProvider).scanValues(nil, columns)
	}
	_spec.Assign = func(columns []string, values []any) error {
		node := &OidcProvider{config: opq.config}
		nodes = append(nodes, node)
		return node.assignValues(columns, values)
	}
	_spec.Node.Schema = opq.schemaConfig.OidcProvider
	ctx = internal.NewSchemaConfigContext(ctx, opq.schemaConfig)
	if len(opq.modifiers) > 0 {
		_spec.Modifiers = opq.modifiers
	}
	for i := range hooks {
		hooks[i](ctx, _spec)
	}
	if err := sqlgraph.QueryNodes(ctx, opq.driver, _spec); err != nil {
		return nil, err
	}
	if len(nodes) == 0 {
		return nodes, nil
	}
	return nodes, nil
}

func (opq *OidcProviderQuery) sqlCount(ctx context.Context) (int, error) {
	_spec := opq.querySpec()
	_spec.Node.Schema = opq.schemaConfig.OidcProvider
	ctx = internal.NewSchemaConfigContext(ctx, opq.schemaConfig)
	if len(opq.modifiers) > 0 {
		_spec.Modifiers = opq.modifiers
	}
	_spec.Node.Columns = opq.ctx.Fields
	if len(opq.ctx.Fields) > 0 {
		_spec.Unique = opq.ctx.Unique != nil && *opq.ctx.Unique
	}
	return sqlgraph.CountNodes(ctx, opq.driver, _spec)
}

func (opq *OidcProviderQuery) querySpec() *sqlgraph.QuerySpec {
	_spec := sqlgraph.NewQuerySpec(oidcprovider.Table, oidcprovider.Columns, sqlgraph.NewFieldSpec(oidcprovider.FieldID, field.TypeInt))
	_spec.From = opq.sql
	if unique := opq.ctx.Unique; unique != nil {
		_spec.Unique = *unique
	} else if opq.path != nil {
		_spec.Unique = true
	}
	if fields := opq.ctx.Fields; len(fields) > 0 {
		_spec.Node.Columns = make([]string, 0, len(fields))
		_spec.Node.Columns = append(_spec.Node.Columns, oidcprovider.FieldID)
		for i := range fields {
			if fields[i] != oidcprovider.FieldID {
				_spec.Node.Columns = append(_spec.Node.Columns, fields[i])
			}
		}
	}
	if ps := opq.predicates; len(ps) > 0 {
		_spec.Predicate = func(selector *sql.Selector) {
			for i := range ps {
				ps[i](selector)
			}
		}
	}
	if limit := opq.ctx.Limit; limit != nil {
		_spec.Limit = *limit
	}
	if offset := opq.ctx.Offset; offset != nil {
		_spec.Offset = *offset
	}
	if ps := opq.order; len(ps) > 0 {
		_spec.Order = func(selector *sql.Selector) {
			for i := range ps {
				ps[i](selector)
			}
		}
	}
	return _spec
}

func (opq *OidcProviderQuery) sqlQuery(ctx context.Context) *sql.Selector {
	builder := sql.Dialect(opq.driver.Dialect())
	t1 := builder.Table(oidcprovider.Table)
	columns := opq.ctx.Fields
	if len(columns) == 0 {
		columns = oidcprovider.Columns
	}
	selector := builder.Select(t1.Columns(columns...)...).From(t1)
	if opq.sql != nil {
		selector = opq.sql
		selector.Select(selector.Columns(columns...)...)
	}
	if opq.ctx.Unique != nil && *opq.ctx.Unique {
		selector.Distinct()
	}
	t1.Schema(opq.schemaConfig.OidcProvider)
	ctx = internal.NewSchemaConfigContext(ctx, opq.schemaConfig)
	selector.WithContext(ctx)
	for _, m := range opq.modifiers {
		m(selector)
	}
	for _, p := range opq.predicates {
		p(selector)
	}
	for _, p := range opq.order {
		p(selector)
	}
	if offset := opq.ctx.Offset; offset != nil {
		// limit is mandatory for offset clause. We start
		// with default value, and override it below if needed.
		selector.Offset(*offset).Limit(math.MaxInt32)
	}
	if limit := opq.ctx.Limit; limit != nil {
		selector.Limit(*limit)
	}
	return selector
}

// Modify adds a query modifier for attaching custom logic to queries.
func (opq *OidcProviderQuery) Modify(modifiers ...func(s *sql.Selector)) *OidcProviderSelect {
	opq.modifiers = append(opq.modifiers, modifiers...)
	return opq.Select()
}

// OidcProviderGroupBy is the group-by builder for OidcProvider entities.
type OidcProviderGroupBy struct {
	selector
	build *OidcProviderQuery
}

// Aggregate adds the given aggregation functions to the group-by query.
func (opgb *OidcProviderGroupBy) Aggregate(fns ...AggregateFunc) *OidcProviderGroupBy {
	opgb.fns = append(opgb.fns, fns...)
	return opgb
}

// Scan applies the selector query and scans the result into the given value.
func (opgb *OidcProviderGroupBy) Scan(ctx context.Context, v any) error {
	ctx = setContextOp(ctx, opgb.build.ctx, ent.OpQueryGroupBy)
	if err := opgb.build.prepareQuery(ctx); err != nil {
		return err
	}
	return scanWithInterceptors[*OidcProviderQuery, *OidcProviderGroupBy](ctx, opgb.build, opgb, opgb.build.inters, v)
}

func (opgb *OidcProviderGroupBy) sqlScan(ctx context.Context, root *OidcProviderQuery, v any) error {
	selector := root.sqlQuery(ctx).Select()
	aggregation := make([]string, 0, len(opgb.fns))
	for _, fn := range opgb.fns {
		aggregation = append(aggregation, fn(selector))
	}
	if len(selector.SelectedColumns()) == 0 {
		columns := make([]string, 0, len(*opgb.flds)+len(opgb.fns))
		for _, f := range *opgb.flds {
			columns = append(columns, selector.C(f))
		}
		columns = append(columns, aggregation...)
		selector.Select(columns...)
	}
	selector.GroupBy(selector.Columns(*opgb.flds...)...)
	if err := selector.Err(); err != nil {
		return err
	}
	rows := &sql.Rows{}
	query, args := selector.Query()
	if err := opgb.build.driver.Query(ctx, query, args, rows); err != nil {
		return err
	}
	defer rows.Close()
	return sql.ScanSlice(rows, v)
}

// OidcProviderSelect is the builder for selecting fields of OidcProvider entities.
type OidcProviderSelect struct {
	*OidcProviderQuery
	selector
}

// Aggregate adds the given aggregation functions to the selector query.
func (ops *OidcProviderSelect) Aggregate(fns ...AggregateFunc) *OidcProviderSelect {
	ops.fns = append(ops.fns, fns...)
	return ops
}

// Scan applies the selector query and scans the result into the given value.
func (ops *OidcProviderSelect) Scan(ctx context.Context, v any) error {
	ctx = setContextOp(ctx, ops.ctx, ent.OpQuerySelect)
	if err := ops.prepareQuery(ctx); err != nil {
		return err
	}
	return scanWithInterceptors[*OidcProviderQuery, *OidcProviderSelect](ctx, ops.OidcProviderQuery, ops, ops.inters, v)
}

func (ops *OidcProviderSelect) sqlScan(ctx context.Context, root *OidcProviderQuery, v any) error {
	selector := root.sqlQuery(ctx)
	aggregation := make([]string, 0, len(ops.fns))
	for _, fn := range ops.fns {
		aggregation = append(aggregation, fn(selector))
	}
	switch n := len(*ops.selector.flds); {
	case n == 0 && len(aggregation) > 0:
		selector.Select(aggregation...)
	case n != 0 && len(aggregation) > 0:
		selector.AppendSelect(aggregation...)
	}
	rows := &sql.Rows{}
	query, args := selector.Query()
	if err := ops.driver.Query(ctx, query, args, rows); err != nil {
		return err
	}
	defer rows.Close()
	return sql.ScanSlice(rows, v)
}

// Modify adds a query modifier for attaching custom logic to queries.
func (ops *OidcProviderSelect) Modify(modifiers ...func(s *sql.Selector)) *OidcProviderSelect {
	ops.modifiers = append(ops.modifiers, modifiers...)
	return ops
}
