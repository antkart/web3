package com.example.web3.model;

import java.io.Serializable;

public class ResultRow implements Serializable {
    private double x;
    private double y;
    private double r;
    private boolean hit;
    private String currentTime;
    private String execTime;

    public ResultRow() { }

    public ResultRow(double x, double y, double r, boolean hit, String currentTime, String execTime) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.hit = hit;
        this.currentTime = currentTime;
        this.execTime = execTime;
    }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getR() { return r; }
    public void setR(double r) { this.r = r; }

    public boolean isHit() { return hit; }
    public void setHit(boolean hit) { this.hit = hit; }

    public String getCurrentTime() { return currentTime; }
    public void setCurrentTime(String currentTime) { this.currentTime = currentTime; }

    public String getExecTime() { return execTime; }
    public void setExecTime(String execTime) { this.execTime = execTime; }
}
