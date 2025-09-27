package com.flightdelays;

import com.flightdelays.tasks.EchoTask;
import com.schouten.core.administrative.Ayuntamiento;
import com.schouten.core.administrative.ComunidadAutonoma;
import com.schouten.core.administrative.Provincia;
import com.schouten.core.administrative.db.AyuntamientoDao;
import com.schouten.core.administrative.db.ComunidadAutonomaDao;
import com.schouten.core.administrative.db.ProvinciaDao;
import com.schouten.core.collectors.properties.ApifyCollector;
import com.schouten.core.collectors.properties.IdealistaCollector;
import com.schouten.core.collectors.properties.IdealistaData;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaCountry;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaOperation;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaPropertyType;
import com.schouten.core.collectors.properties.idealistaTypes.IdealistaSortBy;
import com.schouten.core.properties.idealista.IdealistaProperty;
import com.schouten.core.resources.administrative.AyuntamientoResource;
import com.schouten.core.resources.administrative.ComunidadAutonomaResource;
import com.schouten.core.resources.administrative.ProvinciaResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldApplication.class);

    public static void main(String[] args) throws Exception {
        new HelloWorldApplication().run(args);
    }

    private final HibernateBundle<HelloWorldConfiguration> hibernateBundle =
            new HibernateBundle<HelloWorldConfiguration>(
                    Ayuntamiento.class,
                    Provincia.class,
                    ComunidadAutonoma.class) {
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
                Ayuntamiento.class,
                Provincia.class,
                ComunidadAutonoma.class));
        HibernateUtil.generateSchema(annotatedClasses);
    }

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final AyuntamientoDao ayuntamientoDao = new AyuntamientoDao(hibernateBundle.getSessionFactory());
        final ComunidadAutonomaDao comunidadAutonomaDao = new ComunidadAutonomaDao(hibernateBundle.getSessionFactory());
        final ProvinciaDao provinciaDao = new ProvinciaDao(hibernateBundle.getSessionFactory());
        environment.admin().addTask(new EchoTask());
//        environment.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<User>()
//                .setAuthenticator(new ExampleAuthenticator())
//                .setAuthorizer(new ExampleAuthorizer())
//                .setRealm("SUPER SECRET STUFF")
//                .buildAuthFilter()));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        environment.jersey().register(new AyuntamientoResource(ayuntamientoDao));
        environment.jersey().register(new ComunidadAutonomaResource(comunidadAutonomaDao));
        environment.jersey().register(new ProvinciaResource(provinciaDao));

        IdealistaData data = new IdealistaData(
                IdealistaCountry.SPAIN,
                IdealistaOperation.SALE,
                IdealistaPropertyType.LAND,
                IdealistaSortBy.PROXIMITY,
                "Torrelaguna",
                100);
        IdealistaCollector idealistaCollector = new IdealistaCollector();
        ApifyCollector.ApifyRunDetails details = idealistaCollector.startSearch(data.toRequestBodyString(data));

        LOGGER.info("got dataset details: {}", details);

        String status = "STARTED";
        while (!Objects.equals(status, "SUCCEEDED")) {
            status = idealistaCollector.getSearchStatus(details);
            LOGGER.info("Run status: {}", status);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Set<IdealistaProperty> properties = idealistaCollector.getSearchResults(details);
        LOGGER.info("Got results: {}", properties);
    }
}