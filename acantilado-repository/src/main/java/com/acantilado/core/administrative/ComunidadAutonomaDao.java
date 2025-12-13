package com.acantilado.core.administrative;

import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComunidadAutonomaDAO extends AbstractDAO<ComunidadAutonoma> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComunidadAutonomaDAO.class);

  public ComunidadAutonomaDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public Optional<ComunidadAutonoma> findById(Long comunidadAutonomaId) {
    return Optional.ofNullable(get(comunidadAutonomaId));
  }

  public ComunidadAutonoma create(ComunidadAutonoma comunidadAutonoma) {
    return persist(comunidadAutonoma);
  }

  public List<ComunidadAutonoma> findAll() {
    return list(namedTypedQuery("com.schouten.core.comunidadautonoma.findAll"));
  }
}
