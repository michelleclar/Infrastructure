package org.carl.infra.workflow.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Defines when a workflow schedule fires.
 *
 * <p>Calendar, interval, and Cron expressions are combined as a union. {@code skipCalendars} are
 * then subtracted from that union. Cron is convenient for existing systems; calendar rules provide
 * a structured representation that a durable scheduler can return without losing information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleSpec(
        List<Calendar> calendars,
        List<Interval> intervals,
        List<String> cronExpressions,
        List<Calendar> skipCalendars,
        Instant startAt,
        Instant endAt,
        Duration jitter,
        String timeZoneName) {

    public ScheduleSpec {
        calendars = immutable(calendars);
        intervals = immutable(intervals);
        cronExpressions = immutable(cronExpressions);
        skipCalendars = immutable(skipCalendars);

        for (String expression : cronExpressions) {
            requireText(expression, "cron expression");
        }
        if (calendars.isEmpty() && intervals.isEmpty() && cronExpressions.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one calendar, interval, or cron expression is required");
        }
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must not be before startAt");
        }
        if (jitter != null && jitter.isNegative()) {
            throw new IllegalArgumentException("jitter must not be negative");
        }
        if (timeZoneName != null) {
            requireText(timeZoneName, "timeZoneName");
        }
    }

    /** Creates a Cron schedule in the scheduler's default time zone. */
    public static ScheduleSpec cron(String expression) {
        return cron(expression, null);
    }

    /** Creates a Cron schedule in the supplied IANA time zone. */
    public static ScheduleSpec cron(String expression, String timeZoneName) {
        return new ScheduleSpec(
                List.of(),
                List.of(),
                List.of(requireText(expression, "expression")),
                List.of(),
                null,
                null,
                null,
                timeZoneName);
    }

    /** Creates a fixed-interval schedule with no offset. */
    public static ScheduleSpec interval(Duration every) {
        return new ScheduleSpec(
                List.of(),
                List.of(new Interval(every, null)),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null);
    }

    /** A fixed interval expressed as {@code epoch + n * every + offset}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Interval(Duration every, Duration offset) {
        public Interval {
            if (every == null || every.isZero() || every.isNegative()) {
                throw new IllegalArgumentException("every must be positive");
            }
            if (offset != null && offset.isNegative()) {
                throw new IllegalArgumentException("offset must not be negative");
            }
        }
    }

    /** A structured calendar rule. Null field lists use the documented calendar defaults. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Calendar(
            List<Range> seconds,
            List<Range> minutes,
            List<Range> hours,
            List<Range> daysOfMonth,
            List<Range> months,
            List<Range> years,
            List<Range> daysOfWeek,
            String comment) {

        private static final List<Range> ZERO = List.of(new Range(0));
        private static final List<Range> ALL_MONTH_DAYS = List.of(new Range(1, 31));
        private static final List<Range> ALL_MONTHS = List.of(new Range(1, 12));
        private static final List<Range> ALL_WEEK_DAYS = List.of(new Range(0, 6));

        public Calendar {
            seconds = seconds == null ? ZERO : List.copyOf(seconds);
            minutes = minutes == null ? ZERO : List.copyOf(minutes);
            hours = hours == null ? ZERO : List.copyOf(hours);
            daysOfMonth = daysOfMonth == null ? ALL_MONTH_DAYS : List.copyOf(daysOfMonth);
            months = months == null ? ALL_MONTHS : List.copyOf(months);
            years = years == null ? List.of() : List.copyOf(years);
            daysOfWeek = daysOfWeek == null ? ALL_WEEK_DAYS : List.copyOf(daysOfWeek);
            if (comment != null && comment.isBlank()) {
                comment = null;
            }
            validateRanges(seconds, 0, 59, "seconds");
            validateRanges(minutes, 0, 59, "minutes");
            validateRanges(hours, 0, 23, "hours");
            validateRanges(daysOfMonth, 1, 31, "daysOfMonth");
            validateRanges(months, 1, 12, "months");
            validateRanges(years, 0, Integer.MAX_VALUE, "years");
            validateRanges(daysOfWeek, 0, 6, "daysOfWeek");
        }

        private static void validateRanges(
                List<Range> ranges, int minimum, int maximum, String field) {
            for (Range range : ranges) {
                if (range.start() < minimum || range.end() > maximum) {
                    throw new IllegalArgumentException(
                            field + " range must be between " + minimum + " and " + maximum);
                }
            }
        }
    }

    /** Inclusive range used by a calendar field. */
    public record Range(int start, int end, int step) {
        public Range {
            if (start < 0) {
                throw new IllegalArgumentException("start must not be negative");
            }
            if (end < start) {
                end = start;
            }
            if (step < 0) {
                throw new IllegalArgumentException("step must not be negative");
            }
            if (step == 0) {
                step = 1;
            }
        }

        public Range(int start) {
            this(start, start, 1);
        }

        public Range(int start, int end) {
            this(start, end, 1);
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
