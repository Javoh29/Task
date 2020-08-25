package com.range.task;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_table")
public class LocationModel {

    @PrimaryKey(autoGenerate = true)
    public int id = 0;

    private String time;

    private double lat;

    private double lon;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "LocationModel{" +
                "id=" + id +
                ", time='" + time + '\'' +
                ", lat=" + lat +
                ", lon=" + lon +
                '}';
    }
}
