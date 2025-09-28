package com.acantilado.collection.properties;


import com.acantilado.collection.properties.collectors.ApifyCollector;
import com.acantilado.collection.properties.collectors.IdealistaCollector;
import com.acantilado.collection.properties.idealistaTypes.IdealistaSearchRequest;
import com.acantilado.collection.properties.queries.DefaultIdealistaSearchQueries.IdealistaSearch;
import com.acantilado.core.properties.idealista.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class IdealistaCollectorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaCollectorService.class);
    private static final int SLEEP_MS = 10000;

    private final IdealistaContactInformationDAO contactDAO;
    private final IdealistaPropertyDAO propertyDAO;
    private final IdealistaPriceRecordDAO priceRecordDAO;
    private final SessionFactory sessionFactory;

    public IdealistaCollectorService(IdealistaContactInformationDAO contactDAO,
                                  IdealistaPropertyDAO propertyDAO,
                                  IdealistaPriceRecordDAO priceRecordDAO,
                                  SessionFactory sessionFactory) {
        this.contactDAO = contactDAO;
        this.propertyDAO = propertyDAO;
        this.priceRecordDAO = priceRecordDAO;
        this.sessionFactory = sessionFactory;
    }

    public void collectProperties(Set<IdealistaSearch> searches) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction(); // One transaction for all
        ManagedSessionContext.bind(session);

        try {
            IdealistaCollector collector = new IdealistaCollector();
            searches.forEach(search -> executeCollection(collector, IdealistaSearchRequest.fromSearch(search)));

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new RuntimeException("Collecting properties failed", e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
            session.close();
        }
    }


    private void executeCollection(IdealistaCollector collector, IdealistaSearchRequest request) {
        LOGGER.info("Starting search for request {}", request);
        ApifyCollector.ApifyPendingSearch pendingSearch = collector.startSearch(request.toRequestBodyString(request));

        ApifyCollector.PENDING_SEARCH_STATUS searchStatus = ApifyCollector.PENDING_SEARCH_STATUS.STARTED;
        while (searchStatus != ApifyCollector.PENDING_SEARCH_STATUS.SUCCEEDED) {
            searchStatus = collector.getSearchStatus(pendingSearch);
            LOGGER.info("Run status: {} {}", searchStatus, pendingSearch);
            try {
                Thread.sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        collector.getSearchResults(pendingSearch).forEach(this::processProperty);
    }

    private void processProperty(IdealistaProperty property) {
        IdealistaContactInformation definitiveContactInformation = establishContactInformation(property.getContactInfo());
        IdealistaProperty definitiveIdealistaProperty = establishProperty(property);

        property.setContactInfo(definitiveContactInformation);
        propertyDAO.saveOrUpdate(definitiveIdealistaProperty);
    }

    private IdealistaContactInformation establishContactInformation(IdealistaContactInformation newContactInformation) {
        IdealistaContactInformation definitiveContactInformation;

        if (newContactInformation.getPhoneNumber() == 0) {
            definitiveContactInformation = contactDAO.create(newContactInformation);
        } else {
            definitiveContactInformation = contactDAO
                    .findByPhoneNumber(newContactInformation.getPhoneNumber())
                    .orElse(contactDAO.create(newContactInformation));
        }
        return definitiveContactInformation;
    }

    private IdealistaProperty establishProperty(IdealistaProperty newProperty) {
        final long propertyCode = newProperty.getPropertyCode();
        Optional<IdealistaProperty> maybeProperty = propertyDAO.findByPropertyCode(propertyCode);

        if (maybeProperty.isPresent()) {
            final long currentTimestamp = Instant.now().toEpochMilli();
            IdealistaProperty existingProperty = maybeProperty.get();
            existingProperty.setLastSeen(currentTimestamp);

            Optional<IdealistaPriceRecord> maybePriceRecord = this.priceRecordDAO.findLatestByPropertyCode(propertyCode);
            if (maybePriceRecord.isEmpty()) {
                LOGGER.error("Found existing property but no existing price record {}", existingProperty);
            } else {
                final long newPrice = newProperty.getPriceRecords().get(0).getPrice();
                if (maybePriceRecord.get().getPrice() != newPrice) {
                    LOGGER.info("Identified price change for property {}", propertyCode);
                    IdealistaPriceRecord priceRecord = new IdealistaPriceRecord(
                            propertyCode, newPrice, currentTimestamp);
                    existingProperty.getPriceRecords().add(priceRecord);
                }
            }
            return existingProperty;
        }
        LOGGER.info("Identified new property {}", propertyCode);
        return newProperty;
    }
}