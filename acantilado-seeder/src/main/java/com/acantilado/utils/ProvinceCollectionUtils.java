package com.acantilado.utils;

import com.acantilado.collection.location.CityAyuntamientoCode;
import com.acantilado.core.administrative.*;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import org.hibernate.SessionFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.acantilado.utils.RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction;

public class ProvinceCollectionUtils {

    public static Provincia getProvinceFromId(
            SessionFactory sessionFactory, ProvinciaDAO provinciaDAO, String provinceId) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            Optional<Provincia> provincia = provinciaDAO.findById(provinceId);
            return provincia.orElseThrow();
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

    public static Set<String> getMappedAyuntamientos(
            SessionFactory sessionFactory,
            IdealistaLocationMappingDAO mappingDAO,
            Set<Ayuntamiento> ayuntamientos) {

        return ayuntamientos
                .stream()
                .map(Ayuntamiento::getId)
                .filter(ayuntamientoId -> {
                    List<IdealistaLocationMapping> mappings = executeCallableInSessionWithoutTransaction(
                            sessionFactory,
                            () -> mappingDAO.findByAyuntamientoId(ayuntamientoId)
                    );
                    return !mappings.isEmpty();
                })
                .collect(Collectors.toSet());
    }

    public static Set<Ayuntamiento> getAyuntamientosForProvince(
            SessionFactory sessionFactory, AyuntamientoDAO ayuntamientoDAO, Provincia provincia) {
        return executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> ayuntamientoDAO.findByProvinceId(provincia.getId())
                        .stream()
                        .filter(a -> !a.getId().startsWith("53"))
                        .collect(Collectors.toSet()));
    }

    public static Set<IdealistaAyuntamientoLocation> getLocationsForProvince(
            SessionFactory sessionFactory, IdealistaLocationDAO locationDAO, Provincia provincia) {
        return executeCallableInSessionWithoutTransaction(sessionFactory,
                () -> new HashSet<>(locationDAO.findByProvinceId(provincia.getId())));
    }

    public static Map<String, Set<CodigoPostal>> getPostcodesForAyuntamientos(
            SessionFactory sessionFactory, CodigoPostalDAO codigoPostalDAO, Set<Ayuntamiento> ayuntamientos) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            Map<String, Set<CodigoPostal>> postcodesByAyuntamiento = new HashMap<>();

            ayuntamientos.forEach(ayuntamiento -> {
                List<CodigoPostal> postcodes = codigoPostalDAO.findByAyuntamiento(ayuntamiento.getId());
                postcodesByAyuntamiento.put(ayuntamiento.getId(), new HashSet<>(postcodes));
            });

            return postcodesByAyuntamiento;
        });
    }

    public static Set<String> getPostcodeIdsForProvince(
            SessionFactory sessionFactory, AyuntamientoDAO ayuntamientoDAO, String provinceId) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () ->
                ayuntamientoDAO.findByProvinceId(provinceId)
                    .stream()
                    .filter(a -> !a.getId().startsWith("53"))
                    .flatMap(a -> a.getCodigosPostales().stream())
                    .map(CodigoPostal::getCodigoPostal)
                    .collect(Collectors.toSet()));
    }

    public static Set<Barrio> getBarriosForProvince(SessionFactory sessionFactory, BarrioDAO barrioDAO, Provincia provincia) {
        return executeCallableInSessionWithoutTransaction(sessionFactory, () -> {
            Set<Barrio> barrios = new HashSet<>();
            for (CityAyuntamientoCode city : CityAyuntamientoCode.values()) {
                if (Objects.equals(city.getProvinceCode(), provincia.getId())) {
                    barrios.addAll(barrioDAO.findByAyuntamiento(city.getCityCode()));
                }
            }

            return barrios;
        });
    }
}
