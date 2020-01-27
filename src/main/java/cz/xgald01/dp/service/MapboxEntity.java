package cz.xgald01.dp.service;

import javax.persistence.*;

@Entity
@Table(name = "MAPBOX", uniqueConstraints = {@UniqueConstraint(columnNames = "PLACE_ID"), })
public class MapboxEntity {

    // Specified columns in given mapbox table
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PLACE_ID", unique = true, nullable = false)
    private Integer placeId;

    @Column(name = "NAME", unique = false, nullable = false)
    private String name;

    @Column(name = "LATITUDE", unique = true, nullable = false)
    private Double latitude;

    @Column(name = "LONGITUDE", unique = true, nullable = false)
    private Double longitude;

    // Accessors and mutators for all fields
    public MapboxEntity(){
    }

    public int getPlaceId(){
        return placeId;
    }

    public void setPlaceId(int id){
        this.placeId = id;
    }

    public String getName(){
        return name;
    }

    public void setName(String placeName) {
        this.name = placeName;
    }

    public Double getLatitude(){
        return latitude;
    }

    public void setLatitude(Double placeLatitude) {
        this.latitude = placeLatitude;
    }

    public Double getLongitde(){
        return longitude;
    }

    public void setLongitude(Double placeLongitude) {
        this.longitude = placeLongitude;
    }
}
