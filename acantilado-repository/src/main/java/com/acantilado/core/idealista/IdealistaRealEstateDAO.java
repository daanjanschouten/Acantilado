package com.acantilado.core.idealista;

import com.acantilado.core.idealista.realEstate.IdealistaRealEstate;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public abstract class IdealistaRealEstateDAO<T extends IdealistaRealEstate> extends AbstractDAO<T> {
    public IdealistaRealEstateDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public abstract Optional<T> findByPropertyCode(Long propertyCode);

    public abstract List<T> findAll();

    public abstract List<T> findByMunicipality(String municipality);

    public abstract List<T> findByAyuntamientoIdIsNull();

    public abstract T create(T realEstate);

    public abstract T merge(T realEstate);

    public abstract void delete(T realEstate);

    public abstract void deleteByPropertyCode(Long propertyCode);
}
