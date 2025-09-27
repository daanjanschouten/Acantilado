package com.schouten.core.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

public class CollectorIterator<T> implements Iterator<Collection<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorIterator.class);

    private final Function<URI, Collection<T>> fetcher;
    private final Function<Optional<URI>, URI> uriTransformer;
    private final int maxBatchSize;

    private boolean moreRecordsRemain = true;
    private URI uri;

    public CollectorIterator(
            Function<URI, Collection<T>> fetcher,
            int maxBatchSize,
            Function<Optional<URI>, URI> uriTransformer) {
        this.fetcher = fetcher;
        this.maxBatchSize = maxBatchSize;
        this.uriTransformer = uriTransformer;

        uri = uriTransformer.apply(Optional.empty());
    }

    @Override
    public boolean hasNext() {
        return moreRecordsRemain;
    }

    @Override
    public Collection<T> next() {
        try {
            LOGGER.info("Starting another iteration");
            Collection<T> collection = fetcher.apply(uri);
            if (collection.size() < maxBatchSize) {
                LOGGER.info("Exhausted records, only received {}", collection.size());
                moreRecordsRemain = false;
            }

            uri = uriTransformer.apply(Optional.of(uri));
            return collection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
