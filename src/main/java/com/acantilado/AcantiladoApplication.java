package com.acantilado;

import com.acantilado.collection.administration.AdministrativeCollectorScheduler;
import com.acantilado.collection.administration.GeographicCollectorService;
import com.acantilado.collection.amenity.AmenityCollectorScheduler;
import com.acantilado.collection.amenity.AmenityCollectorServiceFactory;
import com.acantilado.collection.properties.IdealistaCollectorScheduler;
import com.acantilado.collection.properties.IdealistaCollectorServiceFactory;
import com.acantilado.core.administrative.*;
import com.acantilado.core.amenity.GoogleAmenity;
import com.acantilado.core.amenity.GoogleAmenityDAO;
import com.acantilado.core.amenity.GoogleAmenitySnapshot;
import com.acantilado.core.amenity.GoogleAmenitySnapshotDAO;
import com.acantilado.core.idealista.*;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.acantilado.core.resources.administrative.*;
import com.acantilado.core.resources.amenity.GoogleAmenityResource;
import com.acantilado.core.resources.amenity.GoogleAmenitySnapshotResource;
import com.acantilado.core.resources.properties.IdealistaRealEstateResource;
import com.acantilado.tasks.EchoTask;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AcantiladoApplication extends Application<AcantiladoConfiguration> {
    public static void main(String[] args) throws Exception {
        new AcantiladoApplication().run(args);
    }

    private final HibernateBundle<AcantiladoConfiguration> hibernateBundle =
            new HibernateBundle<>(
                    Ayuntamiento.class,
                    Provincia.class,
                    ComunidadAutonoma.class,
                    CodigoPostal.class,
                    Barrio.class,
                    IdealistaLocationMapping.class,
                    IdealistaProperty.class,
                    IdealistaTerrain.class,
                    IdealistaAyuntamientoLocation.class,
                    IdealistaContactInformation.class,
                    IdealistaPropertyPriceRecord.class,
                    IdealistaTerrainPriceRecord.class,
                    GoogleAmenity.class,
                    GoogleAmenitySnapshot.class) {
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
        bootstrap.getObjectMapper().registerModule(new Jdk8Module());
        bootstrap.getObjectMapper().registerModule(new JavaTimeModule());

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

        final Set<Class<?>> annotatedClasses = new HashSet<>(Arrays.asList(
                Ayuntamiento.class,
                Provincia.class,
                ComunidadAutonoma.class,
                CodigoPostal.class,
                Barrio.class,
                IdealistaLocationMapping.class,
                IdealistaProperty.class,
                IdealistaTerrain.class,
                IdealistaAyuntamientoLocation.class,
                IdealistaContactInformation.class,
                IdealistaPropertyPriceRecord.class,
                IdealistaTerrainPriceRecord.class,
                GoogleAmenity.class,
                GoogleAmenitySnapshot.class));
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
        final IdealistaLocationDAO locationDAO = new IdealistaLocationDAO(hibernateBundle.getSessionFactory());

        final GoogleAmenityDAO amenityDAO = new GoogleAmenityDAO(hibernateBundle.getSessionFactory());
        final GoogleAmenitySnapshotDAO amenitySnapshotDAO = new GoogleAmenitySnapshotDAO(hibernateBundle.getSessionFactory());

        final IdealistaLocationMappingDAO idealistaAyuntamientoMappingDAO = new IdealistaLocationMappingDAO(hibernateBundle.getSessionFactory());
        final IdealistaCollectorServiceFactory collectorServiceFactory = new IdealistaCollectorServiceFactory(
                idealistaContactInformationDAO,
                idealistaPropertyDAO,
                idealistaTerrainDAO,
                locationDAO,
                provinciaDao,
                codigoPostalDAO,
                ayuntamientoDao,
                barrioDAO,
                idealistaAyuntamientoMappingDAO,
                hibernateBundle.getSessionFactory());
        final AmenityCollectorServiceFactory amenityServiceFactory = new AmenityCollectorServiceFactory(
                amenityDAO,
                amenitySnapshotDAO,
                provinciaDao,
                ayuntamientoDao,
                hibernateBundle.getSessionFactory());

        final GeographicCollectorService geographicCollectorService = new GeographicCollectorService(
                codigoPostalDAO,
                ayuntamientoDao,
                provinciaDao,
                barrioDAO,
                hibernateBundle.getSessionFactory());

        environment.admin().addTask(new EchoTask());
        environment.jersey().register(RolesAllowedDynamicFeature.class);

        // Administration
        environment.jersey().register(new AyuntamientoResource(ayuntamientoDao));
        environment.jersey().register(new ComunidadAutonomaResource(comunidadAutonomaDao));
        environment.jersey().register(new ProvinciaResource(provinciaDao));
        environment.jersey().register(new CodigoPostalResource(codigoPostalDAO));
        environment.jersey().register(new BarrioResource(barrioDAO));
        environment.jersey().register(new GoogleAmenityResource(amenityDAO));
        environment.jersey().register(new GoogleAmenitySnapshotResource(amenitySnapshotDAO));

        // environment.jersey().register(new SwaggerServlet());


        environment.lifecycle().manage(
                new AdministrativeCollectorScheduler(
                        geographicCollectorService,
                        configuration.getAdministrativeCollector()));

        // Properties
        environment.jersey().register(new IdealistaRealEstateResource(
                idealistaTerrainDAO,
                idealistaPropertyDAO,
                idealistaPropertyPriceRecordDAO,
                idealistaTerrainPriceRecordDAO));

        environment.lifecycle().manage(
                new IdealistaCollectorScheduler(
                        collectorServiceFactory,
                        configuration.getIdealistaCollector()));
        environment.lifecycle().manage(
                new AmenityCollectorScheduler(
                        amenityServiceFactory,
                        configuration.getAmenityCollector()));
    }
}