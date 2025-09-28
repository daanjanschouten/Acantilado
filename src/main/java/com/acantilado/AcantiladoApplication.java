package com.acantilado;

import com.acantilado.core.administrative.*;
import com.acantilado.core.properties.idealista.*;
import com.acantilado.tasks.EchoTask;
import com.acantilado.collection.properties.IdealistaCollectorScheduler;
import com.acantilado.collection.properties.IdealistaCollectorService;
import com.acantilado.core.resources.administrative.AyuntamientoResource;
import com.acantilado.core.resources.administrative.ComunidadAutonomaResource;
import com.acantilado.core.resources.administrative.ProvinciaResource;
import com.acantilado.core.resources.properties.IdealistaPropertyResource;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcantiladoApplication extends Application<AcantiladoConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoApplication.class);

    public static void main(String[] args) throws Exception {
        new AcantiladoApplication().run(args);
    }

    private final HibernateBundle<AcantiladoConfiguration> hibernateBundle =
            new HibernateBundle<AcantiladoConfiguration>(
                    Ayuntamiento.class,
                    Provincia.class,
                    ComunidadAutonoma.class,
                    IdealistaProperty.class,
                    IdealistaContactInformation.class,
                    IdealistaPriceRecord.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(AcantiladoConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "Vertigo";
    }

    @Override
    public void initialize(Bootstrap<AcantiladoConfiguration> bootstrap) {
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
            public DataSourceFactory getDataSourceFactory(AcantiladoConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(new ViewBundle<>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(AcantiladoConfiguration configuration) {
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
    public void run(AcantiladoConfiguration configuration, Environment environment) {
        final AyuntamientoDao ayuntamientoDao = new AyuntamientoDao(hibernateBundle.getSessionFactory());
        final ComunidadAutonomaDao comunidadAutonomaDao = new ComunidadAutonomaDao(hibernateBundle.getSessionFactory());
        final ProvinciaDao provinciaDao = new ProvinciaDao(hibernateBundle.getSessionFactory());


        final IdealistaContactInformationDAO idealistaContactInformationDAO = new IdealistaContactInformationDAO(hibernateBundle.getSessionFactory());
        final IdealistaPriceRecordDAO idealistaPriceRecordDAO = new IdealistaPriceRecordDAO(hibernateBundle.getSessionFactory());
        final IdealistaPropertyDAO idealistaPropertyDAO = new IdealistaPropertyDAO(hibernateBundle.getSessionFactory());

        final IdealistaCollectorService collectorService = new IdealistaCollectorService(
                idealistaContactInformationDAO,
                idealistaPropertyDAO,
                idealistaPriceRecordDAO,
                hibernateBundle.getSessionFactory());

        environment.admin().addTask(new EchoTask());
        environment.jersey().register(RolesAllowedDynamicFeature.class);

        // Administration
        environment.jersey().register(new AyuntamientoResource(ayuntamientoDao));
        environment.jersey().register(new ComunidadAutonomaResource(comunidadAutonomaDao));
        environment.jersey().register(new ProvinciaResource(provinciaDao));

        // Properties
        environment.jersey().register(new IdealistaPropertyResource(idealistaPropertyDAO, idealistaPriceRecordDAO));
        environment.lifecycle().manage(new IdealistaCollectorScheduler(collectorService));
    }
}