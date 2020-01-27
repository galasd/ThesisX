package cz.xgald01.dp.service;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "NASA", uniqueConstraints = {
        @UniqueConstraint(columnNames = "ASTEROID_ID"),
})
public class NasaEntity implements Serializable {

    // Specified columns in given nasa table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASTEROID_ID", unique = true, nullable = false)
    private Integer asteroidId;

    @Column(name = "NAME", unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "EARTH_DISTANCE", unique = false, nullable = false, length = 100)
    private Integer earthDistance;

    // Accessors and mutators for all three fields
    public NasaEntity() {
    }

    public int getAsteroidId() {
        return asteroidId;
    }

    public void setAsteroidId(int id) {
        this.asteroidId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEarthDistance() {
        return earthDistance;
    }

    public void setEarthDistance(int distance) {
        this.earthDistance = distance;
    }

}
