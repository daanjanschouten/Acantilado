package com.acantilado.core.idealista;

import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class IdealistaPropertyPriceRecordDAO extends AbstractDAO<IdealistaPropertyPriceRecord> {

    public IdealistaPropertyPriceRecordDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public IdealistaPropertyPriceRecord create(IdealistaPropertyPriceRecord priceRecord) {
        return persist(priceRecord);
    }

    public List<IdealistaPropertyPriceRecord> findAll() {
        return list(currentSession().createQuery(
                "SELECT p FROM IdealistaPropertyPriceRecord p ORDER BY p.recordedAt DESC",
                IdealistaPropertyPriceRecord.class));
    }

    public Optional<IdealistaPropertyPriceRecord> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public List<IdealistaPropertyPriceRecord> findByPropertyCode(Long propertyCode) {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaPropertyPriceRecord.findByPropertyCode")
                .setParameter("propertyCode", propertyCode)
                .getResultList();
    }

    public Optional<IdealistaPropertyPriceRecord> findLatestByPropertyCode(Long propertyCode) {
        List<IdealistaPropertyPriceRecord> results = namedTypedQuery(
                "com.schouten.core.properties.idealista.IdealistaPropertyPriceRecord.findLatestByPropertyCode")
                .setParameter("propertyCode", propertyCode)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public IdealistaPropertyPriceRecord merge(IdealistaPropertyPriceRecord priceRecord) {
        currentSession().merge(priceRecord);
        return priceRecord;
    }

    public void delete(IdealistaPropertyPriceRecord priceRecord) {
        currentSession().delete(priceRecord);
    }

    public void deleteByPropertyCode(Long propertyCode) {
        currentSession().createQuery(
                        "DELETE FROM IdealistaPropertyPriceRecord p WHERE p.propertyCode = :propertyCode")
                .setParameter("propertyCode", propertyCode)
                .executeUpdate();
    }
}