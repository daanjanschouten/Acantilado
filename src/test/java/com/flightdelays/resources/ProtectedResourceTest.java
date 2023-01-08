package com.flightdelays.resources;

import com.flightdelays.auth.ExampleAuthenticator;
import com.flightdelays.auth.ExampleAuthorizer;
import com.schouten.core.other.User;
import com.schouten.core.resources.other.ProtectedResource;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.Test;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.HttpHeaders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class ProtectedResourceTest {
    private static final BasicCredentialAuthFilter<User> BASIC_AUTH_HANDLER =
            new BasicCredentialAuthFilter.Builder<User>()
                    .setAuthenticator(new ExampleAuthenticator())
                    .setAuthorizer(new ExampleAuthorizer())
                    .setPrefix("Basic")
                    .setRealm("SUPER SECRET STUFF")
                    .buildAuthFilter();

    public static final ResourceExtension RULE = ResourceExtension.builder()
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthDynamicFeature(BASIC_AUTH_HANDLER))
            .addProvider(new AuthValueFactoryProvider.Binder<>(User.class))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(ProtectedResource.class)
            .build();

    @Test
    public void testProtectedEndpoint() {
        String secret = RULE.target("/protected").request()
                .header(HttpHeaders.AUTHORIZATION, "Basic Z29vZC1ndXk6c2VjcmV0")
                .get(String.class);
        assertTrue(secret.startsWith("Hey there, good-guy. You know the secret!"));
    }

    @Test
    public void testProtectedEndpointNoCredentials401() {
        try {
            RULE.target("/protected").request()
                .get(String.class);
        } catch (NotAuthorizedException e) {
            Assertions.assertEquals(e.getResponse().getStatus(), 401);
            assertTrue(e.getResponse().getHeaders().get(HttpHeaders.WWW_AUTHENTICATE)
                    .contains("Basic realm=\"SUPER SECRET STUFF\""));
        }

    }

    @Test
    public void testProtectedEndpointBadCredentials401() {
        NotAuthorizedException exception = Assertions.assertThrows(
                NotAuthorizedException.class,
                () -> RULE.target("/protected").request()
                        .header(HttpHeaders.AUTHORIZATION, "Basic c25lYWt5LWJhc3RhcmQ6YXNkZg==")
                        .get(String.class)
        );
        Assertions.assertEquals(401, exception.getResponse().getStatus());
        Assertions.assertTrue(exception.getResponse().getHeaders().get(HttpHeaders.WWW_AUTHENTICATE)
                        .contains("Basic realm=\"SUPER SECRET STUFF\""));
    }

    @Test
    public void testProtectedAdminEndpoint() {
        String secret = RULE.target("/protected/admin").request()
                .header(HttpHeaders.AUTHORIZATION, "Basic Y2hpZWYtd2l6YXJkOnNlY3JldA==")
                .get(String.class);
        assertTrue(secret.startsWith("Hey there, chief-wizard. It looks like you are an admin."));
    }

    @Test
    public void testProtectedAdminEndpointPrincipalIsNotAuthorized403() {
        ForbiddenException exception = Assertions.assertThrows(
                ForbiddenException.class,
                () -> RULE.target("/protected/admin").request()
                        .header(HttpHeaders.AUTHORIZATION, "Basic Z29vZC1ndXk6c2VjcmV0")
                        .get(String.class)
        );
        Assertions.assertEquals(403, exception.getResponse().getStatus());
    }
}
