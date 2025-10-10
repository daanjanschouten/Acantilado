package com.acantilado;

import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.*;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.acantilado.core.resources.administrative.*;
import com.acantilado.core.resources.properties.IdealistaRealEstateResource;
import com.acantilado.gathering.administration.AdministrativeCollectorScheduler;
import com.acantilado.gathering.administration.AdministrativeCollectorService;
import com.acantilado.gathering.properties.IdealistaCollectorScheduler;
import com.acantilado.gathering.properties.IdealistaCollectorService;
import com.acantilado.tasks.EchoTask;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AcantiladoApplication extends Application<AcantiladoConfiguration> {
    public static void main(String[] args) throws Exception {
        new AcantiladoApplication().run(args);
    }

    private final HibernateBundle<AcantiladoConfiguration> hibernateBundle =
            new HibernateBundle<AcantiladoConfiguration>(
                    Ayuntamiento.class,
                    Provincia.class,
                    ComunidadAutonoma.class,
                    CodigoPostal.class,
                    Barrio.class,
                    IdealistaProperty.class,
                    IdealistaTerrain.class,
                    IdealistaContactInformation.class,
                    IdealistaPropertyPriceRecord.class,
                    IdealistaTerrainPriceRecord.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(AcantiladoConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "Acantilado";
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
        final AyuntamientoDAO ayuntamientoDao = new AyuntamientoDAO(hibernateBundle.getSessionFactory());
        final ComunidadAutonomaDAO comunidadAutonomaDao = new ComunidadAutonomaDAO(hibernateBundle.getSessionFactory());
        final ProvinciaDAO provinciaDao = new ProvinciaDAO(hibernateBundle.getSessionFactory());
        final CodigoPostalDAO codigoPostalDAO = new CodigoPostalDAO(hibernateBundle.getSessionFactory());
        final BarrioDAO barrioDAO = new BarrioDAO(hibernateBundle.getSessionFactory());

        final IdealistaContactInformationDAO idealistaContactInformationDAO = new IdealistaContactInformationDAO(hibernateBundle.getSessionFactory());
        final IdealistaPropertyPriceRecordDAO idealistaPropertyPriceRecordDAO = new IdealistaPropertyPriceRecordDAO(hibernateBundle.getSessionFactory());
        final IdealistaTerrainPriceRecordDAO idealistaTerrainPriceRecordDAO = new IdealistaTerrainPriceRecordDAO(hibernateBundle.getSessionFactory());

        final IdealistaTerrainDAO idealistaTerrainDAO = new IdealistaTerrainDAO(hibernateBundle.getSessionFactory());
        final IdealistaPropertyDAO idealistaPropertyDAO = new IdealistaPropertyDAO(hibernateBundle.getSessionFactory());

        final IdealistaCollectorService collectorService = new IdealistaCollectorService(
                idealistaContactInformationDAO,
                idealistaPropertyDAO,
                idealistaTerrainDAO,
                provinciaDao,
                ayuntamientoDao,
                hibernateBundle.getSessionFactory());
        final AdministrativeCollectorService administrativeCollectorService = new AdministrativeCollectorService(
                codigoPostalDAO,
                ayuntamientoDao,
                barrioDAO,
                hibernateBundle.getSessionFactory());

        environment.servlets()
                .addServlet("h2-console", new org.h2.server.web.WebServlet())
                .addMapping("/console/*");

        environment.admin().addTask(new EchoTask());
        environment.jersey().register(RolesAllowedDynamicFeature.class);

        // Administration
        environment.jersey().register(new AyuntamientoResource(ayuntamientoDao));
        environment.jersey().register(new ComunidadAutonomaResource(comunidadAutonomaDao));
        environment.jersey().register(new ProvinciaResource(provinciaDao));
        environment.jersey().register(new CodigoPostalResource(codigoPostalDAO));
        environment.jersey().register(new BarrioResource(barrioDAO));
        environment.lifecycle().manage(new AdministrativeCollectorScheduler(administrativeCollectorService));

        // Properties
        environment.jersey().register(new IdealistaRealEstateResource(
                idealistaTerrainDAO,
                idealistaPropertyDAO,
                idealistaPropertyPriceRecordDAO,
                idealistaTerrainPriceRecordDAO));
        environment.lifecycle().manage(new IdealistaCollectorScheduler(collectorService));
    }
}