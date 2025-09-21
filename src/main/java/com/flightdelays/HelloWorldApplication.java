package com.flightdelays;

import com.flightdelays.auth.ExampleAuthenticator;
import com.flightdelays.auth.ExampleAuthorizer;
import com.flightdelays.cli.RenderCommand;
import com.flightdelays.health.TemplateHealthCheck;
import com.flightdelays.tasks.EchoTask;
import com.schouten.core.administrative.Ayuntamiento;
import com.schouten.core.administrative.ComunidadAutonoma;
import com.schouten.core.administrative.Provincia;
import com.schouten.core.administrative.db.AyuntamientoDao;
import com.schouten.core.administrative.db.ComunidadAutonomaDao;
import com.schouten.core.administrative.db.ProvinciaDao;
import com.schouten.core.aviation.Aircraft;
import com.schouten.core.aviation.Airport;
import com.schouten.core.aviation.Carrier;
import com.schouten.core.aviation.Flight;
import com.schouten.core.aviation.db.AircraftDao;
import com.schouten.core.aviation.db.AirportDao;
import com.schouten.core.aviation.db.CarrierDao;
import com.schouten.core.aviation.db.FlightDao;
import com.schouten.core.aviation.other.PersonDAO;
import com.schouten.core.other.Person;
import com.schouten.core.other.Template;
import com.schouten.core.other.User;
import com.schouten.core.resources.administrative.AyuntamientoResource;
import com.schouten.core.resources.administrative.ComunidadAutonomaResource;
import com.schouten.core.resources.administrative.ProvinciaResource;
import com.schouten.core.resources.aviation.AircraftResource;
import com.schouten.core.resources.aviation.AirportResource;
import com.schouten.core.resources.aviation.CarrierResource;
import com.schouten.core.resources.aviation.FlightResource;
import com.schouten.core.resources.other.*;
import com.schouten.core.resources.other.filters.DateRequiredFeature;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
    public static void main(String[] args) throws Exception {
        new HelloWorldApplication().run(args);
    }

    private final HibernateBundle<HelloWorldConfiguration> hibernateBundle =
            new HibernateBundle<HelloWorldConfiguration>(
                    Aircraft.class,
                    Airport.class,
                    Ayuntamiento.class,
                    Provincia.class,
                    ComunidadAutonoma.class,
                    Carrier.class,
                    Flight.class,
                    Person.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "Vertigo";
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
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(new ViewBundle<>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(HelloWorldConfiguration configuration) {
                return configuration.getViewRendererConfiguration();
            }
        });

        final Set<Class<?>> annotatedClasses = new HashSet<>(Arrays.asList(
                Aircraft.class,
                Airport.class,
                Ayuntamiento.class,
                Provincia.class,
                ComunidadAutonoma.class,
                Carrier.class,
                Flight.class,
                Person.class));
        HibernateUtil.generateSchema(annotatedClasses);
    }

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final AircraftDao aircraftDao = new AircraftDao(hibernateBundle.getSessionFactory());
        final PersonDAO personDAO = new PersonDAO(hibernateBundle.getSessionFactory());
        final FlightDao flightDao = new FlightDao(hibernateBundle.getSessionFactory());
        final AirportDao airportDao = new AirportDao(hibernateBundle.getSessionFactory());
        final AyuntamientoDao ayuntamientoDao = new AyuntamientoDao(hibernateBundle.getSessionFactory());
        final ComunidadAutonomaDao comunidadAutonomaDao = new ComunidadAutonomaDao(hibernateBundle.getSessionFactory());
        final ProvinciaDao provinciaDao = new ProvinciaDao(hibernateBundle.getSessionFactory());
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
        environment.jersey().register(new AirportResource(airportDao));
        environment.jersey().register(new CarrierResource(carrierDao));
        environment.jersey().register(new AyuntamientoResource(ayuntamientoDao));
        environment.jersey().register(new ComunidadAutonomaResource(comunidadAutonomaDao));
        environment.jersey().register(new ProvinciaResource(provinciaDao));
        environment.jersey().register(new FlightResource(flightDao, carrierDao, aircraftDao, airportDao));
        environment.jersey().register(new FilteredResource());
    }
}