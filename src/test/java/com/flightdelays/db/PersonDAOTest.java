package com.flightdelays.db;

import com.schouten.core.aviation.other.PersonDAO;
import com.schouten.core.other.Person;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
public class PersonDAOTest {

    public static DAOTestExtension daoTestExtension = DAOTestExtension.newBuilder()
        .addEntityClass(Person.class)
        .build();

    private static PersonDAO personDAO;

    @BeforeAll
    public static void setUp() {
        personDAO = new PersonDAO(daoTestExtension.getSessionFactory());
    }

    @Test
    public void createPerson() {
        final Person jeff = daoTestExtension.inTransaction(() -> personDAO.create(new Person("Jeff", "The plumber")));
        Assertions.assertTrue(jeff.getId() > 0);
        Assertions.assertEquals(jeff.getFullName(), "Jeff");
        Assertions.assertEquals(jeff.getJobTitle(), "The plumber");
        Assertions.assertEquals(personDAO.findById(jeff.getId()), Optional.of(jeff));
    }

    @Test
    public void findAll() {
        daoTestExtension.inTransaction(() -> {
            personDAO.create(new Person("Jeff", "The plumber"));
            personDAO.create(new Person("Jim", "The cook"));
            personDAO.create(new Person("Randy", "The watchman"));
        });
        final List<Person> persons = personDAO.findAll();
        Assertions.assertTrue(
                persons.stream().map(Person::getFullName).collect(Collectors.toList())
                        .containsAll(List.of("Jeff", "Jim", "Randy")));
        Assertions.assertTrue(
                persons.stream().map(Person::getJobTitle).collect(Collectors.toList())
                        .containsAll(List.of("The plumber", "The cook", "The watchman")));
    }

    @Test
    public void handlesNullFullName() {
        SessionFactory sessionFactory = daoTestExtension.getSessionFactory();
        Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> daoTestExtension.inTransaction(() -> personDAO.create(new Person(null, "The null"))));
    }
}
