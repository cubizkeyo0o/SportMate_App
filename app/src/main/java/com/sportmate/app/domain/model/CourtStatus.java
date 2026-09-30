package com.sportmate.app.domain.model;

/** Whether a court accepts new online bookings. Mirrors the backend CourtStatus enum. */
public enum CourtStatus {
    ACTIVE,
    INACTIVE,
    /** A status this app version does not know about. */
    UNKNOWN
}
