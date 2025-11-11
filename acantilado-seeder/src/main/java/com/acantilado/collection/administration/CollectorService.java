package com.acantilado.collection.administration;

public abstract class CollectorService {
    public abstract boolean isSeedingNecessary();

    public abstract void seed();
}
