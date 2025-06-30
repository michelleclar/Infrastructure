package org.carl.infrastructure.authorization.modle;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusTest
class UserIdentityTest {
    @Inject SecurityIdentity securityIdentity;

    @Test
    void test() {
        UserIdentity userIdentity = new UserIdentity(securityIdentity);


    }
}
