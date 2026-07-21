## 2026-07-07T13:00:16Z

You are the mq-kafka-m5-worker. Your task is to implement Milestone 5 (Integration Tests with Testcontainers) for the `infrastructure-component-mq-kafka` module.
The project root is `/Users/carl/workspace/backend/Infrastructure`.

Please do the following:
1. Update `/Users/carl/workspace/backend/Infrastructure/infrastructure-component-mq-kafka/build.gradle.kts` to add the Testcontainers dependencies:
   - `testImplementation(libs.testcontainers)`
   - `testImplementation(libs.testcontainers.junit.jupiter)`
   - `testImplementation("org.testcontainers:kafka:1.20.4")`
   These must be added under the `dependencies { ... }` block.

2. Create `/Users/carl/workspace/backend/Infrastructure/infrastructure-component-mq-kafka/src/test/java/org/carl/infrastructure/mq/kafka/builder/KafkaIntegrationTest.java` with a comprehensive integration test suite using Testcontainers. Use `confluentinc/cp-kafka:7.4.0` (or another appropriate image tag).
   The test suite must cover:
   - Setting up a Kafka container.
   - Initializing the `MQClient` pointing to the container's bootstrap servers.
   - A producer successfully publishing a raw message (`byte[]`) to a topic.
   - A consumer successfully receiving that message via a listener or direct receive, and verifying the payload.
   - A typed (generic `T` = a custom POJO class, e.g., `TestUser`) producer/consumer round-trip verifying JSON serialization/deserialization.
   - Gracefully closing the producers, consumers, and client.

3. Write a completion report (`handoff.md`) in your working directory and notify the parent when done.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
