package com.acantilado.auth;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

 // implements Authenticator<BasicCredentials, User>
public class ExampleAuthenticator {
    /**
     * Valid users with mapping user -> roles
     */
    private static final Map<String, Set<String>> VALID_USERS = ImmutableMap.of(
        "guest", ImmutableSet.of(),
        "good-guy", ImmutableSet.of("BASIC_GUY"),
        "chief-wizard", ImmutableSet.of("ADMIN", "BASIC_GUY")
    );

//    @Override
//    public Optional<User> authenticate(BasicCredentials credentials) throws AuthenticationException {
//        if (VALID_USERS.containsKey(credentials.getUsername()) && "secret".equals(credentials.getPassword())) {
//            return Optional.of(new User(credentials.getUsername(), VALID_USERS.get(credentials.getUsername())));
//        }
//        return Optional.empty();
//    }
}
