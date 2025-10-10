package com.acantilado.gathering.administration;

public abstract class CollectorService {
    public abstract boolean isSeedingNecessary();

    public abstract void seed();
}
