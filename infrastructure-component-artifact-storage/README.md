# infrastructure-component-artifact-storage

`infrastructure-component-artifact-storage` provides app-neutral artifact
storage APIs and a local file implementation.

Core APIs:

- `ArtifactStorage`
- `ArtifactWriteRequest`
- `ArtifactMetadata`
- `LocalArtifactStorage`

The local provider stores bytes under a configured root, writes sidecar metadata,
creates parent directories, and rejects keys that contain absolute or traversal
path segments.
