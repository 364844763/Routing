package com.hit.jj.pathplaning;

/**
 * Created by Administrator on 2015/8/5.
 */
public class Path {
    private String id;
    private double length;
    private String Start;
    private String end;
    private double sLongitude;
    private double sLatitude;
    private double eLongitude;
    private double eLatitude;
    private int nextDirection;//0-直行，1-左行，2-右行
    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStart() {
        return Start;
    }

    public void setStart(String start) {
        Start = start;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public int getNextDirection() {
        return nextDirection;
    }

    public void setNextDirection(int nextDirection) {
        this.nextDirection = nextDirection;
    }

    public double getsLongitude() {
        return sLongitude;
    }

    public void setsLongitude(double sLongitude) {
        this.sLongitude = sLongitude;
    }

    public double getsLatitude() {
        return sLatitude;
    }

    public void setsLatitude(double sLatitude) {
        this.sLatitude = sLatitude;
    }

    public double geteLongitude() {
        return eLongitude;
    }

    public void seteLongitude(double eLongitude) {
        this.eLongitude = eLongitude;
    }

    public double geteLatitude() {
        return eLatitude;
    }

    public void seteLatitude(double eLatitude) {
        this.eLatitude = eLatitude;
    }
}
