package com.acantilado.utils;

import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.acantilado.collection.location.CityAyuntamientoCode;
import org.hibernate.SessionFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.acantilado.utils.RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction;

public class ProvinceCollectionUtils {

    public static Provincia getProvinceFromName(
            SessionFactory sessionFactory, ProvinciaDAO provinciaDAO, String provinceName) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            List<Provincia> provincias = provinciaDAO.findByName(provinceName);
            if (provincias.size() != 1) {
                throw new RuntimeException("More than 1 or 0 provinces found for province " + provinceName);
            }
            return provincias.get(0);
        });
    }

    public static Map<String, List<IdealistaLocationMapping>> getMappingsByLocationIds(
            SessionFactory sessionFactory, IdealistaLocationMappingDAO mappingDAO, Set<String> idealistaLocationIdsForProvince) {
        return executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> mappingDAO.findAll()
                        .stream()
                        .filter(mapped ->
                                idealistaLocationIdsForProvince.contains(mapped.getIdealistaLocationId()))
                        .collect(Collectors.groupingBy(IdealistaLocationMapping::getIdealistaLocationId)));
    }

    public static Map<Long, List<IdealistaLocationMapping>> getMappingsByAyuntamientoIds(
            SessionFactory sessionFactory, IdealistaLocationMappingDAO mappingDAO, Set<String> idealistaLocationIdsForProvince) {
        return executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> mappingDAO.findAll()
                        .stream()
                        .filter(mapped ->
                                idealistaLocationIdsForProvince.contains(mapped.getIdealistaLocationId()))
                        .collect(Collectors.groupingBy(IdealistaLocationMapping::getAcantiladoAyuntamientoId)));
    }

    public static Set<Ayuntamiento> getAyuntamientosForProvince(
            SessionFactory sessionFactory, AyuntamientoDAO ayuntamientoDAO, Provincia provincia) {
        return executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> {
                    List<Ayuntamiento> ayuntamientos = ayuntamientoDAO.findByProvinceId(provincia.getId());

                    return ayuntamientos.stream()
                            .filter(a -> {
                                String ayuntamientoProvinceCode = String.valueOf(a.getId() / 1000);
                                return ayuntamientoProvinceCode.equals(String.valueOf(provincia.getId()));
                            })
                            .collect(Collectors.toSet());
                });
    }

    public static Set<IdealistaAyuntamientoLocation> getLocationsForProvince(
            SessionFactory sessionFactory, IdealistaLocationDAO locationDAO, Provincia provincia) {
        return executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> new HashSet<>(locationDAO.findByProvinceId(provincia.getId())));
    }

    public static Map<Long, Set<CodigoPostal>> getPostcodesForProvince(
            SessionFactory sessionFactory, CodigoPostalDAO codigoPostalDAO, Set<Ayuntamiento> ayuntamientos) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            Map<Long, Set<CodigoPostal>> postcodesByAyuntamiento = new HashMap<>();

            ayuntamientos.forEach(ayuntamiento -> {
                List<CodigoPostal> postcodes = codigoPostalDAO.findByAyuntamiento(ayuntamiento.getId());
                postcodesByAyuntamiento.put(ayuntamiento.getId(), new HashSet<>(postcodes));
            });

            return postcodesByAyuntamiento;
        });
    }

    public static Set<Barrio> getBarriosForProvince(SessionFactory sessionFactory, BarrioDAO barrioDAO, Provincia provincia) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            Set<Barrio> barrios = new HashSet<>();
            for (CityAyuntamientoCode city : CityAyuntamientoCode.values()) {
                if (city.getProvinceCode() == provincia.getId()) {
                    barrios.addAll(barrioDAO.findByAyuntamiento(city.getMunicipalityCode()));
                }
            }

            return barrios;
        });
    }
}
