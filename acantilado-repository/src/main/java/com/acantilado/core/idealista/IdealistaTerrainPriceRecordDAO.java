package com.acantilado.core.idealista;

import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import io.dropwizard.hibernate.AbstractDAO;
import java.util.List;
import java.util.Optional;
import org.hibernate.SessionFactory;

public class IdealistaTerrainPriceRecordDAO extends AbstractDAO<IdealistaTerrainPriceRecord> {

  public IdealistaTerrainPriceRecordDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public IdealistaTerrainPriceRecord create(IdealistaTerrainPriceRecord priceRecord) {
    return persist(priceRecord);
  }

  public List<IdealistaTerrainPriceRecord> findAll() {
    return list(
        currentSession()
            .createQuery(
                "SELECT p FROM IdealistaTerrainPriceRecord p ORDER BY p.recordedAt DESC",
                IdealistaTerrainPriceRecord.class));
  }

  public Optional<IdealistaTerrainPriceRecord> findById(Long id) {
    return Optional.ofNullable(get(id));
  }

  public List<IdealistaTerrainPriceRecord> findByPropertyCode(Long propertyCode) {
    return namedTypedQuery(
            "com.schouten.core.properties.idealista.IdealistaTerrainPriceRecord.findByPropertyCode")
        .setParameter("propertyCode", propertyCode)
        .getResultList();
  }

  public Optional<IdealistaTerrainPriceRecord> findLatestByPropertyCode(Long propertyCode) {
    List<IdealistaTerrainPriceRecord> results =
        namedTypedQuery(
                "com.schouten.core.properties.idealista.IdealistaTerrainPriceRecord.findLatestByPropertyCode")
            .setParameter("propertyCode", propertyCode)
            .setMaxResults(1)
            .getResultList();

    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
  }

  public IdealistaTerrainPriceRecord merge(IdealistaTerrainPriceRecord priceRecord) {
    currentSession().merge(priceRecord);
    return priceRecord;
  }

  public void delete(IdealistaTerrainPriceRecord priceRecord) {
    currentSession().delete(priceRecord);
  }

  public void deleteByPropertyCode(Long propertyCode) {
    currentSession()
        .createQuery(
            "DELETE FROM IdealistaTerrainPriceRecord p WHERE p.propertyCode = :propertyCode")
        .setParameter("propertyCode", propertyCode)
        .executeUpdate();
  }
}
