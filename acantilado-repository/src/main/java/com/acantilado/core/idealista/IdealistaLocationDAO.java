package com.acantilado.core.idealista;

import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;

public class IdealistaLocationDAO extends AbstractDAO<IdealistaAyuntamientoLocation> {
  public IdealistaLocationDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public Optional<IdealistaAyuntamientoLocation> findByLocationId(String locationId) {
    return Optional.ofNullable(get(locationId));
  }

  public IdealistaAyuntamientoLocation create(IdealistaAyuntamientoLocation location) {
    return persist(location);
  }

  public IdealistaAyuntamientoLocation merge(IdealistaAyuntamientoLocation location) {
    return currentSession().merge(location);
  }

  //    public List<IdealistaAyuntamientoLocation> findByProvinceString(String provinceId) {
  //        return
  // namedTypedQuery("com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId")
  //                .setParameter("provinciaId", provinceId)
  //                .getResultList();
  //    }

  public List<IdealistaAyuntamientoLocation> findByProvinceId(String provinceId) {
    String provinceString = String.join("-", "0-EU-ES", provinceId);

    return namedTypedQuery(
            "com.schouten.core.properties.idealista.IdealistaAyuntamientoLocation.findByProvinciaId")
        .setParameter("provinciaId", provinceString)
        .getResultList();
  }
}
