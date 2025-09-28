package com.acantilado.core.properties.idealista;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public class IdealistaContactInformationDAO extends AbstractDAO<IdealistaContactInformation> {

    public IdealistaContactInformationDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<IdealistaContactInformation> findByPhoneNumber(Long phoneNumber) {
        return Optional.ofNullable(get(phoneNumber));
    }

    public List<IdealistaContactInformation> findAll() {
        return namedTypedQuery("com.schouten.core.properties.idealista.IdealistaContactInformation.findAll")
                .getResultList();
    }

    public IdealistaContactInformation create(IdealistaContactInformation contactInfo) {
        return persist(contactInfo);
    }

    public IdealistaContactInformation saveOrUpdate(IdealistaContactInformation contactInfo) {
        currentSession().saveOrUpdate(contactInfo);
        return contactInfo;
    }

    public List<IdealistaContactInformation> findByUserType(String userType) {
        CriteriaBuilder builder = currentSession().getCriteriaBuilder();
        CriteriaQuery<IdealistaContactInformation> criteria = builder.createQuery(IdealistaContactInformation.class);
        Root<IdealistaContactInformation> root = criteria.from(IdealistaContactInformation.class);

        criteria.select(root).where(builder.equal(root.get("userType"), userType));

        return currentSession().createQuery(criteria).getResultList();
    }

    public List<IdealistaContactInformation> findByContactName(String contactName) {
        Query<IdealistaContactInformation> query = currentSession().createQuery(
                "SELECT c FROM IdealistaContactInformation c WHERE c.contactName = :contactName",
                IdealistaContactInformation.class);
        query.setParameter("contactName", contactName);
        return query.getResultList();
    }

    public Optional<IdealistaContactInformation> findByPhoneNumberWithProperties(Long phoneNumber) {
        Query<IdealistaContactInformation> query = currentSession().createQuery(
                "SELECT c FROM IdealistaContactInformation c LEFT JOIN FETCH c.properties WHERE c.phoneNumber = :phoneNumber",
                IdealistaContactInformation.class);
        query.setParameter("phoneNumber", phoneNumber);
        return Optional.ofNullable(query.uniqueResult());
    }
}