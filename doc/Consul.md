### open acl
```shell
cat consul.d/acl.hcl
acl = { enabled = true default_policy = "deny" enable_token_persistence = true }
```
### get token
```shell
# exec sh
docker-compose exec consul sh
# exec command
consul acl bootstrap
SecretID 就是token

AccessorID:       30422e56-e314-9e2c-4f98-4373fa85ae30
SecretID:         b25200bf-730c-fe0f-5257-8df238313b25
Description:      Bootstrap Token (Global Management)
Local:            false
Create Time:      2024-05-23 09:08:04.11698481 +0000 UTC
Policies:
   00000000-0000-0000-0000-000000000001 - global-management
```