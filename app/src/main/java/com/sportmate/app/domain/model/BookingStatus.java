package com.sportmate.app.domain.model;

/** A booking's lifecycle state. Mirrors the backend BookingStatus enum. */
public enum BookingStatus {
    /** On hold, waiting for payment; blocks the court while the hold runs. */
    PENDING,
    /** Paid. */
    CONFIRMED,
    /** Cancelled by a person, or the hold expired. */
    CANCELLED,
    COMPLETED,
    NO_SHOW,
    /** A status this app version does not know about. */
    UNKNOWN
}
