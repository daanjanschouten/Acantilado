package com.example.helloworld.core;

import javax.persistence.*;


@Entity
public class Artist {
    private Long artistId;

    @OneToOne(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Ranking ranking;

    public Artist() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artist_id")
    public Long getArtistId() {
        return artistId;
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }
}
