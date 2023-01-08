package com.flightdelays;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
public class IntegrationTest {

    private static final String TMP_FILE = createTempFile();
    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-example.yml");

    public static final DropwizardAppExtension<HelloWorldConfiguration> RULE = new DropwizardAppExtension<>(
            HelloWorldApplication.class, CONFIG_PATH,
            ConfigOverride.config("database.url", "jdbc:h2:" + TMP_FILE));

    @BeforeAll
    public static void migrateDb() throws Exception {
        RULE.getApplication().run("db", "migrate", CONFIG_PATH);
    }

    private static String createTempFile() {
        try {
            return File.createTempFile("test-example", null).getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

//    @Test
//    public void testHelloWorld() {
//        final Optional<String> name = Optional.of("Dr. IntegrationTest");
//
//        final Saying saying = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/hello-world")
//                .queryParam("name", name.get())
//                .request()
//                .get(Saying.class);
//        Assertions.assertEquals(saying.getContent(), RULE.getConfiguration().buildTemplate().render(name));
//    }
//
//    @Test
//    public void testPostPerson() {
////        final Person person = new Person("Dr. IntegrationTest", "Chief Wizard");
////        final Person newPerson = RULE.client().target("http://localhost:" + RULE.getLocalPort() + "/people")
////                .request()
////                .post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE))
////                .readEntity(Person.class);
////        Assertions.assertNotNull(newPerson.getId());
////        Assertions.assertEquals(newPerson.getFullName(), person.getFullName());
////        Assertions.assertEquals(newPerson.getJobTitle(), person.getJobTitle());
//    }
}