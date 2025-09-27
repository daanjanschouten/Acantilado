package com.schouten.core.properties.idealista;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class IdealistaPriceRecordDAO extends AbstractDAO<IdealistaPriceRecord> {

    public IdealistaPriceRecordDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<IdealistaPriceRecord> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public List<IdealistaPriceRecord> findByPropertyCode(Long propertyCode) {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaPriceRecord.findByPropertyCode")
                .setParameter("propertyCode", propertyCode)
                .getResultList();
    }

    public Optional<IdealistaPriceRecord> findLatestByPropertyCode(Long propertyCode) {
        List<IdealistaPriceRecord> results = namedTypedQuery("com.schouten.core.properties.idealista.IdealistaPriceRecord.findLatestByPropertyCode")
                .setParameter("propertyCode", propertyCode)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<IdealistaPriceRecord> findByPropertyCodeBetweenDates(Long propertyCode,
                                                                     LocalDateTime startDate,
                                                                     LocalDateTime endDate) {
        Query<IdealistaPriceRecord> query = currentSession().createQuery(
                "SELECT p FROM IdealistaPriceRecord p WHERE p.propertyCode = :propertyCode " +
                        "AND p.recordedAt BETWEEN :startDate AND :endDate ORDER BY p.recordedAt DESC",
                IdealistaPriceRecord.class);

        query.setParameter("propertyCode", propertyCode);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }

    public List<IdealistaPriceRecord> findPropertiesWithPriceChanges() {
        Query<IdealistaPriceRecord> query = currentSession().createQuery(
                "SELECT p FROM IdealistaPriceRecord p WHERE p.propertyCode IN " +
                        "(SELECT pr.propertyCode FROM IdealistaPriceRecord pr GROUP BY pr.propertyCode HAVING COUNT(pr.propertyCode) > 1)",
                IdealistaPriceRecord.class);

        return query.getResultList();
    }

    public List<IdealistaPriceRecord> findAll() {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<IdealistaPriceRecord> criteria = builder.createQuery(IdealistaPriceRecord.class);
        Root<IdealistaPriceRecord> root = criteria.from(IdealistaPriceRecord.class);
        criteria.select(root).orderBy(builder.desc(root.get("recordedAt")));

        return currentSession().createQuery(criteria).getResultList();
    }

    public IdealistaPriceRecord create(IdealistaPriceRecord priceRecord) {
        return persist(priceRecord);
    }

    public IdealistaPriceRecord saveOrUpdate(IdealistaPriceRecord priceRecord) {
        currentSession().saveOrUpdate(priceRecord);
        return priceRecord;
    }

    public void delete(IdealistaPriceRecord priceRecord) {
        currentSession().delete(priceRecord);
    }

    public void deleteByPropertyCode(Long propertyCode) {
        Query<?> query = currentSession().createQuery(
                "DELETE FROM IdealistaPriceRecord p WHERE p.propertyCode = :propertyCode");
        query.setParameter("propertyCode", propertyCode);
        query.executeUpdate();
    }
}