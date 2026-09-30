package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;

import java.math.BigDecimal;

/** A sport a court offers, with the current price for one hour of the court. */
public final class CourtSport {
    private final String id;
    private final Sport sport;
    private final BigDecimal pricePerHour;
    private final String currency;

    public CourtSport(
            @NonNull String id,
            @NonNull Sport sport,
            @NonNull BigDecimal pricePerHour,
            @NonNull String currency) {
        this.id = id;
        this.sport = sport;
        this.pricePerHour = pricePerHour;
        this.currency = currency;
    }

    /** The court-sport link id, which identifies what is booked. Not the sport's id. */
    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public Sport getSport() {
        return sport;
    }

    /** Exact decimal amount; zero is valid. */
    @NonNull
    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    /** ISO 4217 code, e.g. {@code "VND"}. */
    @NonNull
    public String getCurrency() {
        return currency;
    }
}
