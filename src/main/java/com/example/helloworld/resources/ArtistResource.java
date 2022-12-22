package com.example.helloworld.resources;

import com.example.helloworld.core.Artist;
import com.example.helloworld.db.ArtistDao;
import io.dropwizard.hibernate.UnitOfWork;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
@Path("/artists")
@Produces(MediaType.APPLICATION_JSON)
public class ArtistResource {
    private final ArtistDao artistDao;

    public ArtistResource(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    @POST
    @UnitOfWork
    public Artist createArist(Artist artist) {
        return artistDao.create(artist);
    }

    @GET
    @UnitOfWork
    public List<Artist> listFlights() {
        return artistDao.findAll();
    }
}