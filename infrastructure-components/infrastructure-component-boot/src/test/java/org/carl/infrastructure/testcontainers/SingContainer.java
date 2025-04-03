package org.carl.infrastructure.testcontainers;

// import org.testcontainers.containers.GenericContainer;
// import org.testcontainers.junit.jupiter.Testcontainers;
// import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

// @Testcontainers
// public interface SingContainer extends QuarkusTestResourceLifecycleManager {
//    GenericContainer<?> getContainer();
//
//    default void runContainer() {
//        this.start();
//    }
//
//    @Override
//    default Map<String, String> start() {
//        if (!getContainer().isRunning()) {
//            getContainer().start();
//        }
//
// ServiceLoader.load(SingContainer.class).forEach(QuarkusTestResourceLifecycleManager::start);
//        return ImmutableMap.of();
//    }
//
//    @Override
//    default void stop() {
//        getContainer().stop();
//    }
// }
