## use `ent`

1. run `go run -mod=mod entgo.io/ent/cmd/ent new User` create schema template
2. complete schema
3. run `go generate ./ent` generate go struct file

### consume primary by `go ent orm`

> `ent` default id is primary

```go
func (Schema) Fields() []Schema.Field {
return field.String("id").StorageKey("sku_id").Unique().Immutable()
}
```