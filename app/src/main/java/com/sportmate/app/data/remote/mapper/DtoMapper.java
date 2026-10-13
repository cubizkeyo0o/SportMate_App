package com.sportmate.app.data.remote.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sportmate.app.data.remote.UtcTimestamps;
import com.sportmate.app.data.remote.dto.BookingResponseDto;
import com.sportmate.app.data.remote.dto.PlayRequestPageResponseDto;
import com.sportmate.app.data.remote.dto.PlayRequestParticipantResponseDto;
import com.sportmate.app.data.remote.dto.PlayRequestResponseDto;
import com.sportmate.app.data.remote.dto.DiscoveredCourtResponseDto;
import com.sportmate.app.data.remote.dto.DiscoveredCourtSportResponseDto;
import com.sportmate.app.data.remote.dto.DiscoveredOperatingHourResponseDto;
import com.sportmate.app.data.remote.dto.SportResponseDto;
import com.sportmate.app.data.remote.dto.SportSummaryResponseDto;
import com.sportmate.app.data.remote.dto.UserResponseDto;
import com.sportmate.app.data.remote.dto.UserSportProfileResponseDto;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.domain.model.BookingStatus;
import com.sportmate.app.domain.model.Court;
import com.sportmate.app.domain.model.CourtSport;
import com.sportmate.app.domain.model.CourtStatus;
import com.sportmate.app.domain.model.OperatingHour;
import com.sportmate.app.domain.model.ParticipantStatus;
import com.sportmate.app.domain.model.PlayRequest;
import com.sportmate.app.domain.model.PlayRequestPage;
import com.sportmate.app.domain.model.PlayRequestParticipant;
import com.sportmate.app.domain.model.PlayRequestStatus;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.User;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.domain.model.UserRole;
import com.sportmate.app.domain.model.UserStatus;
import com.sportmate.app.domain.model.Weekday;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * DTO to domain model mapping. Missing required fields throw {@link IllegalStateException}, which
 * {@link com.sportmate.app.data.remote.ApiCallExecutor} reports as an unreadable response.
 */
public final class DtoMapper {
    // Court-local wall-clock time as the backend formats it; 24:00 is not a valid value.
    private static final Pattern LOCAL_TIME = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");
    private static final Pattern LOCAL_DATE = Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}$");

    private DtoMapper() {
    }

    @NonNull
    public static User toUser(@Nullable UserResponseDto dto) {
        if (dto == null) {
            throw new IllegalStateException("user is missing");
        }
        return new User(
                required(dto.id, "id"),
                required(dto.email, "email"),
                required(dto.displayName, "display_name"),
                dto.phone,
                toUserStatus(dto.status),
                toUserRole(dto.role));
    }

    @NonNull
    public static List<Sport> toSports(@NonNull List<SportResponseDto> dtos) {
        List<Sport> sports = new ArrayList<>(dtos.size());
        for (SportResponseDto dto : dtos) {
            sports.add(toSport(dto));
        }
        return Collections.unmodifiableList(sports);
    }

    @NonNull
    public static Sport toSport(@Nullable SportResponseDto dto) {
        if (dto == null) {
            throw new IllegalStateException("sport is missing");
        }
        return new Sport(
                required(dto.id, "id"),
                required(dto.name, "name"),
                required(dto.slug, "slug"));
    }

    @NonNull
    public static List<UserSportProfile> toUserSportProfiles(@NonNull List<UserSportProfileResponseDto> dtos) {
        List<UserSportProfile> profiles = new ArrayList<>(dtos.size());
        for (UserSportProfileResponseDto dto : dtos) {
            profiles.add(toUserSportProfile(dto));
        }
        return Collections.unmodifiableList(profiles);
    }

    @NonNull
    public static UserSportProfile toUserSportProfile(@Nullable UserSportProfileResponseDto dto) {
        if (dto == null) {
            throw new IllegalStateException("sport profile is missing");
        }
        SportSummaryResponseDto sport = dto.sport;
        if (sport == null || sport.isActive == null) {
            throw new IllegalStateException("sport is missing");
        }
        return new UserSportProfile(
                new Sport(required(sport.id, "sport.id"), required(sport.name, "sport.name"), required(sport.slug, "sport.slug")),
                sport.isActive,
                toSkillLevel(dto.skillLevel));
    }

    /** SkillLevel values are serialized as snake_case names; they are stable on the backend. */
    @NonNull
    public static SkillLevel toSkillLevel(@Nullable String value) {
        if (value != null) {
            for (SkillLevel level : SkillLevel.values()) {
                if (toApiValue(level).equals(value)) {
                    return level;
                }
            }
        }
        throw new IllegalStateException("unknown skill level");
    }

    @NonNull
    public static String toApiValue(@NonNull SkillLevel level) {
        switch (level) {
            case BEGINNER:
                return "beginner";
            case ELEMENTARY:
                return "elementary";
            case INTERMEDIATE:
                return "intermediate";
            case ADVANCED:
                return "advanced";
            case EXPERT:
                return "expert";
            default:
                throw new IllegalArgumentException("unmapped skill level");
        }
    }

    @NonNull
    public static PlayRequestPage toPlayRequestPage(@Nullable PlayRequestPageResponseDto dto) {
        if (dto == null || dto.items == null || dto.page == null || dto.pageSize == null || dto.totalCount == null) {
            throw new IllegalStateException("play request page is incomplete");
        }
        List<PlayRequest> items = new ArrayList<>(dto.items.size());
        for (PlayRequestResponseDto item : dto.items) {
            items.add(toPlayRequest(item));
        }
        return new PlayRequestPage(items, dto.page, dto.pageSize, dto.totalCount);
    }

    @NonNull
    public static PlayRequest toPlayRequest(@Nullable PlayRequestResponseDto dto) {
        if (dto == null || dto.host == null || dto.spotsNeeded == null || dto.acceptedCount == null) {
            throw new IllegalStateException("play request is incomplete");
        }
        List<PlayRequestParticipant> participants = new ArrayList<>();
        if (dto.participants != null) {
            for (PlayRequestParticipantResponseDto participant : dto.participants) {
                participants.add(toPlayRequestParticipant(participant));
            }
        }
        return new PlayRequest(
                required(dto.id, "id"),
                required(dto.sportId, "sport_id"),
                required(dto.sportName, "sport_name"),
                required(dto.sportSlug, "sport_slug"),
                required(dto.title, "title"),
                dto.description,
                UtcTimestamps.parseDateTimeOffset(dto.startsAtUtc),
                UtcTimestamps.parseDateTimeOffset(dto.endsAtUtc),
                dto.spotsNeeded,
                dto.acceptedCount,
                dto.locationName,
                nullableSkillLevel(dto.minSkillLevel),
                nullableSkillLevel(dto.maxSkillLevel),
                toPlayRequestStatus(dto.status),
                required(dto.host.displayName, "host.display_name"),
                nullableSkillLevel(dto.host.skillLevel),
                nullableParticipantStatus(dto.myParticipantStatus),
                participants,
                dto.latitude,
                dto.longitude,
                UtcTimestamps.parseDateTimeOffset(dto.createdAtUtc),
                UtcTimestamps.parseDateTimeOffset(dto.updatedAtUtc));
    }

    @NonNull
    public static List<PlayRequestParticipant> toPlayRequestParticipants(
            @NonNull List<PlayRequestParticipantResponseDto> dtos) {
        List<PlayRequestParticipant> participants = new ArrayList<>(dtos.size());
        for (PlayRequestParticipantResponseDto dto : dtos) {
            participants.add(toPlayRequestParticipant(dto));
        }
        return Collections.unmodifiableList(participants);
    }

    @NonNull
    private static PlayRequestParticipant toPlayRequestParticipant(@Nullable PlayRequestParticipantResponseDto dto) {
        if (dto == null) {
            throw new IllegalStateException("play request participant is missing");
        }
        return new PlayRequestParticipant(
                required(dto.id, "participant.id"),
                required(dto.displayName, "participant.display_name"),
                nullableSkillLevel(dto.skillLevel),
                toParticipantStatus(dto.status),
                UtcTimestamps.parseDateTimeOffset(dto.requestedAtUtc));
    }

    @Nullable
    private static SkillLevel nullableSkillLevel(@Nullable String value) {
        return value == null ? null : toSkillLevel(value);
    }

    @Nullable
    private static ParticipantStatus nullableParticipantStatus(@Nullable String value) {
        return value == null ? null : toParticipantStatus(value);
    }

    @NonNull
    private static PlayRequestStatus toPlayRequestStatus(@Nullable String value) {
        if ("open".equals(value)) return PlayRequestStatus.OPEN;
        if ("full".equals(value)) return PlayRequestStatus.FULL;
        if ("cancelled".equals(value)) return PlayRequestStatus.CANCELLED;
        if ("completed".equals(value)) return PlayRequestStatus.COMPLETED;
        return PlayRequestStatus.UNKNOWN;
    }

    @NonNull
    private static ParticipantStatus toParticipantStatus(@Nullable String value) {
        if ("pending".equals(value)) return ParticipantStatus.PENDING;
        if ("accepted".equals(value)) return ParticipantStatus.ACCEPTED;
        if ("rejected".equals(value)) return ParticipantStatus.REJECTED;
        if ("withdrawn".equals(value)) return ParticipantStatus.WITHDRAWN;
        return ParticipantStatus.UNKNOWN;
    }

    @NonNull
    public static List<Court> toCourts(@NonNull List<DiscoveredCourtResponseDto> dtos) {
        List<Court> courts = new ArrayList<>(dtos.size());
        for (DiscoveredCourtResponseDto dto : dtos) {
            courts.add(toCourt(dto));
        }
        return Collections.unmodifiableList(courts);
    }

    @NonNull
    public static Court toCourt(@Nullable DiscoveredCourtResponseDto dto) {
        if (dto == null || dto.latitude == null || dto.longitude == null) {
            throw new IllegalStateException("court is incomplete");
        }
        return new Court(
                required(dto.id, "id"),
                required(dto.name, "name"),
                dto.description,
                required(dto.address, "address"),
                required(dto.city, "city"),
                dto.latitude,
                dto.longitude,
                required(dto.timeZoneId, "time_zone_id"),
                toCourtStatus(dto.status));
    }

    @NonNull
    public static List<CourtSport> toCourtSports(@NonNull List<DiscoveredCourtSportResponseDto> dtos) {
        List<CourtSport> sports = new ArrayList<>(dtos.size());
        for (DiscoveredCourtSportResponseDto dto : dtos) {
            if (dto == null || dto.pricePerHour == null) {
                throw new IllegalStateException("court sport is incomplete");
            }
            sports.add(new CourtSport(
                    required(dto.courtSportId, "court_sport_id"),
                    toSport(dto.sport),
                    dto.pricePerHour,
                    required(dto.currency, "currency")));
        }
        return Collections.unmodifiableList(sports);
    }

    @NonNull
    public static List<OperatingHour> toOperatingHours(@NonNull List<DiscoveredOperatingHourResponseDto> dtos) {
        List<OperatingHour> hours = new ArrayList<>(dtos.size());
        for (DiscoveredOperatingHourResponseDto dto : dtos) {
            if (dto == null || dto.dayOfWeek == null) {
                throw new IllegalStateException("operating hour is incomplete");
            }
            hours.add(new OperatingHour(
                    toWeekday(dto.dayOfWeek),
                    localTime(dto.opensAt, "opens_at"),
                    localTime(dto.closesAt, "closes_at")));
        }
        return Collections.unmodifiableList(hours);
    }

    /** Backend day numbers: 0 = Sunday ... 6 = Saturday. */
    @NonNull
    public static Weekday toWeekday(int dayOfWeek) {
        Weekday[] days = Weekday.values();
        if (dayOfWeek < 0 || dayOfWeek >= days.length) {
            throw new IllegalStateException("unknown day of week");
        }
        return days[dayOfWeek];
    }

    @NonNull
    public static List<Booking> toBookings(@NonNull List<BookingResponseDto> dtos) {
        List<Booking> bookings = new ArrayList<>(dtos.size());
        for (BookingResponseDto dto : dtos) {
            bookings.add(toBooking(dto));
        }
        return Collections.unmodifiableList(bookings);
    }

    @NonNull
    public static Booking toBooking(@Nullable BookingResponseDto dto) {
        if (dto == null || dto.sport == null || dto.totalPrice == null) {
            throw new IllegalStateException("booking is incomplete");
        }
        String localDate = required(dto.localDate, "local_date");
        if (!LOCAL_DATE.matcher(localDate).matches()) {
            throw new IllegalStateException("local_date is not yyyy-MM-dd");
        }
        return new Booking(
                required(dto.id, "id"),
                required(dto.courtId, "court_id"),
                required(dto.courtSportId, "court_sport_id"),
                new Sport(required(dto.sport.id, "sport.id"), required(dto.sport.name, "sport.name"),
                        required(dto.sport.slug, "sport.slug")),
                UtcTimestamps.parse(dto.startsAtUtc),
                UtcTimestamps.parse(dto.endsAtUtc),
                required(dto.timeZoneId, "time_zone_id"),
                localDate,
                localTime(dto.localStartTime, "local_start_time"),
                localTime(dto.localEndTime, "local_end_time"),
                toBookingStatus(dto.status),
                dto.totalPrice,
                required(dto.currency, "currency"),
                dto.holdExpiresAtUtc == null ? null : UtcTimestamps.parse(dto.holdExpiresAtUtc),
                dto.cancellationReason,
                dto.cancelledAtUtc == null ? null : UtcTimestamps.parse(dto.cancelledAtUtc),
                UtcTimestamps.parse(dto.createdAtUtc));
    }

    // BookingStatus values are serialized as snake_case names.
    @NonNull
    static BookingStatus toBookingStatus(@Nullable String status) {
        if (status == null) {
            return BookingStatus.UNKNOWN;
        }
        switch (status) {
            case "pending":
                return BookingStatus.PENDING;
            case "confirmed":
                return BookingStatus.CONFIRMED;
            case "cancelled":
                return BookingStatus.CANCELLED;
            case "completed":
                return BookingStatus.COMPLETED;
            case "no_show":
                return BookingStatus.NO_SHOW;
            default:
                return BookingStatus.UNKNOWN;
        }
    }

    // CourtStatus values are serialized as snake_case names.
    @NonNull
    static CourtStatus toCourtStatus(@Nullable String status) {
        if ("active".equals(status)) {
            return CourtStatus.ACTIVE;
        }
        if ("inactive".equals(status)) {
            return CourtStatus.INACTIVE;
        }
        return CourtStatus.UNKNOWN;
    }

    private static String localTime(@Nullable String value, String field) {
        if (value == null || !LOCAL_TIME.matcher(value).matches()) {
            throw new IllegalStateException(field + " is not HH:mm");
        }
        return value;
    }

    // Role names are the backend's UserRole member names.
    @NonNull
    static UserRole toUserRole(@Nullable String role) {
        if ("User".equals(role)) {
            return UserRole.USER;
        }
        if ("CourtOwner".equals(role)) {
            return UserRole.COURT_OWNER;
        }
        if ("Admin".equals(role)) {
            return UserRole.ADMIN;
        }
        return UserRole.UNKNOWN;
    }

    // Enum values are serialized snake_case by the backend.
    @NonNull
    static UserStatus toUserStatus(@Nullable String status) {
        if ("active".equals(status)) {
            return UserStatus.ACTIVE;
        }
        if ("suspended".equals(status)) {
            return UserStatus.SUSPENDED;
        }
        return UserStatus.UNKNOWN;
    }

    private static String required(@Nullable String value, String field) {
        if (value == null) {
            throw new IllegalStateException(field + " is missing");
        }
        return value;
    }
}
