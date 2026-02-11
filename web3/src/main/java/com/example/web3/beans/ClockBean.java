package com.example.web3.beans;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ClockBean {
    private static final ZoneId SPB = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getNow() {
        return ZonedDateTime.now(SPB).format(FMT);
    }
}
