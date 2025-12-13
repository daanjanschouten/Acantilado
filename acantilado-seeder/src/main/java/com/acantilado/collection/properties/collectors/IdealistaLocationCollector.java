package com.acantilado.collection.properties.collectors;

import com.acantilado.collection.apify.ApifyCollector;
import com.acantilado.collection.location.AcantiladoLocation;
import com.acantilado.collection.properties.idealista.IdealistaSearchRequest;
import com.acantilado.core.idealista.IdealistaLocationDAO;
import com.acantilado.core.idealista.realEstate.IdealistaAyuntamientoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdealistaLocationCollector
    extends ApifyCollector<IdealistaSearchRequest, IdealistaAyuntamientoLocation> {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaLocationCollector.class);

  private final IdealistaLocationDAO locationDAO;

  public IdealistaLocationCollector(
      IdealistaLocationDAO locationDAO,
      ExecutorService executorService,
      SessionFactory sessionFactory) {
    super(executorService, sessionFactory);

    this.locationDAO = locationDAO;
  }

  @Override
  protected String getActorId() {
    return "REcGj6dyoIJ9Z7aE6";
  }

  @Override
  protected int getRetryCount() {
    return 20;
  }

  @Override
  protected int getConcurrentRunCount() {
    return 32;
  }

  @Override
  protected Optional<IdealistaAyuntamientoLocation> constructObject(JsonNode jsonNode) {
    try {
      String idealistaLocationId = jsonNode.get("locationId").textValue();
      String normalizedId = AcantiladoLocation.normalizeIdealistaLocationId(idealistaLocationId);
      return Optional.of(new IdealistaAyuntamientoLocation(normalizedId));
    } catch (Exception e) {
      LOGGER.error("Failed to construct JSON object: {}", jsonNode, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void storeResult(IdealistaAyuntamientoLocation result) {
    this.locationDAO.merge(result);
  }
}
