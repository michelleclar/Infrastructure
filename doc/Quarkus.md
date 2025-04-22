## quarkus http root
>  https://cn.quarkus.io/guides/rest#request-or-response-filters
```properties

quarkus.http.root-path=/
```

## quarkus use keyloak

### setting keyloak

#### 1. create realms

![image-20250331141919090](/Users/carl/workspace/backend/Infrastructure/doc/image/keyloak-create-realms-1.png)

#### 2. create user

![image-20250331142151228](/Users/carl/workspace/backend/Infrastructure/doc/image/keyloak-create-user1.png)

#### 3. Create client

![image-20250331142526059](/Users/carl/workspace/backend/Infrastructure/doc/image/keyloak-create-client-1.png)

> open client create and auth

![image-20250331142720360](/Users/carl/workspace/backend/Infrastructure/doc/image/keyloak-create-client-2.png)

> modify

![](/Users/carl/workspace/backend/Infrastructure/doc/image/image-20250331142828932.png)

### 2 config quarkus applaction

```properties
quarkus.oidc.auth-server-url=http://localhost:1080/realms/scaffold
quarkus.oidc.client-id=scaffold-dev
quarkus.oidc.credentials.secret=secret
quarkus.keycloak.policy-enforcer.enable=true
# if need redirt to keyloak login 
quarkus.oidc.roles.source=accesstoken
quarkus.oidc.application-type=web-app

```
### build native

```puml
./gradlew :demo:build "-Dquarkus.native.enabled=true" "-Dquarkus.native.container-build=true"
```