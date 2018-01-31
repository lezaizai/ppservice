package com.didlink.models;

import java.sql.Time;
import java.sql.Timestamp;

public class UserLocation {
    long lid;
    long uid;
    String address;
    int port;
    double latitude;
    double longtitude;
    long locatetime;

    public UserLocation(long uid, String address, int port, double latitude, double longtitude, long locatetime) {
        this.uid = uid;
        this.address = address;
        this.port = port;
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.locatetime = locatetime;
    }

    public void setLid(long lid) {
        this.lid = lid;
    }

    public long getLid(){
        return lid;
    }

    public long getUid(){
        return uid;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public long getLocatetime() {
        return locatetime;
    }
}
