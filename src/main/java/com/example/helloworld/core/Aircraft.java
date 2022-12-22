package com.example.helloworld.core;

import javax.persistence.*;

@Table(name = "aircraft")
@Entity
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public Aircraft() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
