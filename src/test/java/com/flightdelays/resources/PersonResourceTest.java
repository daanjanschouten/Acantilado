package com.flightdelays.resources;

import com.schouten.core.other.Person;
import com.schouten.core.aviation.other.PersonDAO;
import com.schouten.core.resources.other.PersonResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;

import javax.ws.rs.core.Response;
import java.util.Optional;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PersonResource}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class PersonResourceTest {
    private static final PersonDAO DAO = mock(PersonDAO.class);

    public static final ResourceExtension RULE = ResourceExtension.builder()
            .addResource(new PersonResource(DAO))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .build();
    private Person person;

    @BeforeEach
    public void setup() {
        person = new Person();
        person.setId(1L);
    }

    @AfterEach
    public void tearDown() {
        reset(DAO);
    }

    @Test
    public void getPersonSuccess() {
        when(DAO.findById(1L)).thenReturn(Optional.of(person));

        Person found = RULE.target("/people/1").request().get(Person.class);

        Assertions.assertEquals(found.getId(), person.getId());
        verify(DAO).findById(1L);
    }

    @Test
    public void getPersonNotFound() {
        when(DAO.findById(2L)).thenReturn(Optional.empty());
        final Response response = RULE.target("/people/2").request().get();

        Assertions.assertEquals(response.getStatusInfo().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode());
        verify(DAO).findById(2L);
    }
}
