package com.example.web3.service;

public final class AreaChecker {
    private AreaChecker() {}

    public static boolean isHit(double x, double y, double r) {
        if (r <= 0) {
            return false;
        }

        boolean rect = (x >= 0) && (y >= 0) && (x <= r) && (y <= r / 2.0);


        boolean circle = (x <= 0) && (y >= 0) && (x * x + y * y <= r * r);


        boolean triangle = (x >= 0) && (y <= 0) && (x <= r) && (y >= (x / 2.0) - (r / 2.0));

        return rect || circle || triangle;
    }
}
