package com.flightdelays.resources;

import com.schouten.core.other.Person;
import com.schouten.core.aviation.other.PersonDAO;
import com.google.common.collect.ImmutableList;
import com.schouten.core.resources.other.PeopleResource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PeopleResource}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class PeopleResourceTest {
    private static final PersonDAO PERSON_DAO = mock(PersonDAO.class);

    public static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .addResource(new PeopleResource(PERSON_DAO))
            .build();

    @Captor
    private ArgumentCaptor<Person> personCaptor;

    private static Person person;

    @BeforeAll
    public static void setUp() {
        person = new Person();
        person.setFullName("Full Name");
        person.setJobTitle("Job Title");
    }

    @AfterAll
    public static void tearDown() {
        reset(PERSON_DAO);
    }

    @Test
    public void createPerson() {
        when(PERSON_DAO.create(any(Person.class))).thenReturn(person);
        final Response response = RESOURCES.target("/people")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE));

        Assertions.assertEquals(response.getStatusInfo(), Response.Status.OK);
        verify(PERSON_DAO).create(personCaptor.capture());
        Assertions.assertEquals(personCaptor.getValue(), person);
    }

    @Test
    public void listPeople() throws Exception {
        final ImmutableList<Person> people = ImmutableList.of(person);
        when(PERSON_DAO.findAll()).thenReturn(people);

        final List<Person> response = RESOURCES.target("/people")
            .request().get(new GenericType<List<Person>>() {
            });

        verify(PERSON_DAO).findAll();
        Assertions.assertTrue(response.containsAll(people));
    }
}
