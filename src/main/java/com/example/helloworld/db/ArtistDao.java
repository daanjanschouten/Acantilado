package com.example.helloworld.db;

import com.example.helloworld.core.Artist;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Optional;

public class ArtistDao extends AbstractDAO<Artist> {
    public ArtistDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Artist> findById(Long id) {
        return Optional.ofNullable(get(id));
    }

    public Artist create(Artist artist) {
        return persist(artist);
    }

    public List<Artist> findAll() {
        return list(namedTypedQuery("com.example.helloworld.core.Artist.findAll"));
    }
}
