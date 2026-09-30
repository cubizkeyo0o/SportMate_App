package com.sportmate.app.testutil;

/** Backend BookingResponse JSON for tests: snake_case properties and snake_case enum values. */
public final class BookingJson {
    public static final String BOOKING_ID = "b1f0c2d3-4e5f-4a6b-8c7d-9e0f1a2b3c4d";
    public static final String COURT_ID = "7d4e2a10-5b3c-4f1e-9a8b-0c1d2e3f4a5b";
    public static final String COURT_SPORT_ID = "c5d6e7f8-0a1b-4c2d-9e3f-4a5b6c7d8e9f";

    private BookingJson() {
    }

    /** A booking on 2099-10-05 10:00-11:30 in Asia/Ho_Chi_Minh (03:00-04:30 UTC). */
    public static String booking(String status, String holdExpiresJson, String reasonJson, String cancelledJson) {
        return "{\"id\":\"" + BOOKING_ID + "\",\"user_id\":\"4f1c2d7e-8a8b-4d8e-9d52-2b1f0c1a7e11\","
                + "\"court_id\":\"" + COURT_ID + "\",\"court_sport_id\":\"" + COURT_SPORT_ID + "\","
                + "\"sport\":{\"id\":\"s-1\",\"name\":\"Badminton\",\"slug\":\"badminton\",\"is_active\":true},"
                + "\"starts_at_utc\":\"2099-10-05T03:00:00Z\",\"ends_at_utc\":\"2099-10-05T04:30:00Z\","
                + "\"time_zone_id\":\"Asia/Ho_Chi_Minh\",\"local_date\":\"2099-10-05\","
                + "\"local_start_time\":\"10:00\",\"local_end_time\":\"11:30\","
                + "\"status\":\"" + status + "\",\"total_price\":225000.00,\"currency\":\"VND\","
                + "\"hold_expires_at_utc\":" + holdExpiresJson + ","
                + "\"cancellation_reason\":" + reasonJson + ",\"cancelled_at_utc\":" + cancelledJson + ","
                + "\"created_at_utc\":\"2099-10-01T02:00:00.1234567Z\","
                + "\"updated_at_utc\":\"2099-10-01T02:00:00.1234567Z\"}";
    }

    public static String pending() {
        return booking("pending", "\"2099-10-01T02:15:00.1234567Z\"", "null", "null");
    }

    public static String confirmed() {
        return booking("confirmed", "null", "null", "null");
    }

    public static String cancelled() {
        return booking("cancelled", "null", "null", "\"2099-10-01T03:00:00Z\"");
    }
}
