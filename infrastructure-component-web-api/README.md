# infrastructure-component-web-api

`infrastructure-component-web-api` provides app-neutral response envelope and
exception mapping primitives. It has no Quarkus, CDI, JAX-RS, or ER Tool
dependency.

Core APIs:

- `ResponseEnvelope`
- `WebRequestContext`
- `WebError`
- `BusinessWebException`
- `ValidationWebException`
- `NotFoundWebException`
- `WebExceptionMapper`
- `ResponseEnvelopeAdapter`

Applications can use `DefaultResponseEnvelopeAdapter` or provide their own
adapter to keep an existing API response shape.
