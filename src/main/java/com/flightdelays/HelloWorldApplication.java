package com.flightdelays;

import com.flightdelays.auth.ExampleAuthenticator;
import com.flightdelays.auth.ExampleAuthorizer;
import com.flightdelays.cli.RenderCommand;
import com.schouten.core.aviation.other.PersonDAO;
import com.schouten.core.other.Person;
import com.schouten.core.other.Template;
import com.schouten.core.other.User;
import com.schouten.core.resources.other.filters.DateRequiredFeature;
import com.flightdelays.health.TemplateHealthCheck;
import com.flightdelays.tasks.EchoTask;
import com.schouten.core.aviation.*;
import com.schouten.core.aviation.db.*;
import com.schouten.core.resources.aviation.*;
import com.schouten.core.resources.other.*;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.io.IOException;
import java.util.*;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
    public static void main(String[] args) throws Exception {
        new HelloWorldApplication().run(args);
    }
    private final HibernateBundle<HelloWorldConfiguration> hibernateBundle =
            new HibernateBundle<HelloWorldConfiguration>(
                    Aircraft.class,
                    Airport.class,
                    Carrier.class,
                    Flight.class,
                    Person.class,
                    Runway.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "Flight Delays";
    }

    @Override
    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
        bootstrap.addCommand(new RenderCommand());
        bootstrap.addBundle(new AssetsBundle());
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(new MigrationsBundle<HelloWorldConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(new ViewBundle<HelloWorldConfiguration>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(HelloWorldConfiguration configuration) {
                return configuration.getViewRendererConfiguration();
            }
        });
        final Set<Class<?>> annotatedClasses = new HashSet<>(Arrays.asList(
                Aircraft.class,
                Airport.class,
                Carrier.class,
                Flight.class,
                Person.class,
                Runway.class));
        HibernateUtil.generateSchema(annotatedClasses);
    }

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final AircraftDao aircraftDao = new AircraftDao(hibernateBundle.getSessionFactory());
        final PersonDAO personDAO = new PersonDAO(hibernateBundle.getSessionFactory());
        final FlightDao flightDao = new FlightDao(hibernateBundle.getSessionFactory());
        final AirportDao airportDao = new AirportDao(hibernateBundle.getSessionFactory());
        final RunwayDao runwayDao = new RunwayDao(hibernateBundle.getSessionFactory());
        final CarrierDao carrierDao = new CarrierDao(hibernateBundle.getSessionFactory());
        final Template template = configuration.buildTemplate();
        environment.healthChecks().register("template", new TemplateHealthCheck(template));
        environment.admin().addTask(new EchoTask());
        environment.jersey().register(DateRequiredFeature.class);
        environment.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<User>()
                .setAuthenticator(new ExampleAuthenticator())
                .setAuthorizer(new ExampleAuthorizer())
                .setRealm("SUPER SECRET STUFF")
                .buildAuthFilter()));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new HelloWorldResource(template));
        environment.jersey().register(new ViewResource());
        environment.jersey().register(new ProtectedResource());
        environment.jersey().register(new PeopleResource(personDAO));
        environment.jersey().register(new AircraftResource(aircraftDao));
        environment.jersey().register(new PersonResource(personDAO));
        environment.jersey().register(new FlightResource(flightDao));
        environment.jersey().register(new AirportResource(airportDao));
        environment.jersey().register(new RunwayResource(runwayDao));
        environment.jersey().register(new CarrierResource(carrierDao));
        environment.jersey().register(new FilteredResource());
    }
}