package leisure.engine;

import leisure.core.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central engine for the FLC Booking System.
 *
 * Design: Façade — single authoritative API over all domain objects.
 *
 * All business rules enforced exclusively here:
 *   ✦ Capacity limit  (max 4 per session)
 *   ✦ Duplicate booking prevention
 *   ✦ Time-conflict detection (same participant, same week/day/slot)
 *   ✦ Unique, non-recycled booking ID generation  (REC-XXXX)
 *   ✦ Booking lifecycle management
 *   ✦ Report computation (attendance, income)
 */
public class BookingEngine {

    // ── Domain stores ─────────────────────────────────────────────────────────

    private final Map<String, Participant>  participants = new LinkedHashMap<>();
    private final SessionCatalogue          catalogue    = new SessionCatalogue();
    /** recordId → BookingRecord  (never removed; only state-updated) */
    private final Map<String, BookingRecord> records     = new LinkedHashMap<>();

    private int idCounter = 1;

    // ══════════════════════════════════════════════════════════════════════════
    //  PARTICIPANT DIRECTORY
    // ══════════════════════════════════════════════════════════════════════════

    public void addParticipant(Participant p) {
        participants.put(p.getParticipantCode(), p);
    }

    public Optional<Participant> findParticipant(String code) {
        return Optional.ofNullable(participants.get(code));
    }

    public Collection<Participant> listAllParticipants() {
        return Collections.unmodifiableCollection(participants.values());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SESSION CATALOGUE
    // ══════════════════════════════════════════════════════════════════════════

    public void addSession(ActivitySession session) {
        catalogue.register(session);
    }

    public SessionCatalogue getCatalogue() { return catalogue; }

    public List<ActivitySession> viewByDay(WeekendDay day) {
        return catalogue.filterByDay(day);
    }

    public List<ActivitySession> viewByType(String exerciseType) {
        return catalogue.filterByType(exerciseType);
    }

    public List<String> listExerciseTypes() {
        return catalogue.allExerciseTypes();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING OPERATIONS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new booking for a participant in the given session.
     *
     * Checks performed (in order):
     *  1. Participant exists
     *  2. Session exists
     *  3. Session has capacity
     *  4. No existing active booking for this participant + session
     *  5. No time conflict with other active bookings
     *
     * @return the newly created {@link BookingRecord}
     * @throws IllegalArgumentException on missing participant/session
     * @throws IllegalStateException    on constraint violation
     */
    public BookingRecord createBooking(String participantCode, String sessionCode) {
        Participant     participant = requireParticipant(participantCode);
        ActivitySession session     = requireSession(sessionCode);

        // Capacity
        if (!session.hasOpenSpot()) {
            throw new IllegalStateException(
                "Error: Session is fully occupied — '"
                + session.getExerciseType() + "' on "
                + session.getDay() + " " + session.getSlot().getLabel()
                + " (Week " + session.getWeekIndex() + ") has no available spots.");
        }

        // Duplicate check
        boolean isDuplicate = records.values().stream()
                .anyMatch(r -> r.getParticipantCode().equals(participantCode)
                            && r.getSessionCode().equals(sessionCode)
                            && r.isActive());
        if (isDuplicate) {
            throw new IllegalStateException(
                "Error: " + participant.getFullName()
                + " already has an active booking for session " + sessionCode + ".");
        }

        // Time conflict
        verifyNoConflict(participantCode, session, null);

        // Create record
        String        recordId = generateRecordId();
        BookingRecord record   = new BookingRecord(recordId, participantCode, sessionCode);
        records.put(recordId, record);
        session.occupySpot(participantCode);

        return record;
    }

    /**
     * Transfers an existing booking to a different session.
     *
     * Rules:
     *   - Record must be active
     *   - New session must differ from current
     *   - New session must have capacity
     *   - No time conflict after transfer
     */
    public BookingRecord transferBooking(String recordId, String newSessionCode) {
        BookingRecord   record     = requireActiveRecord(recordId);
        ActivitySession oldSession = requireSession(record.getSessionCode());
        ActivitySession newSession = requireSession(newSessionCode);

        if (record.getSessionCode().equals(newSessionCode)) {
            throw new IllegalArgumentException(
                "Error: The target session must differ from the current one.");
        }
        if (!newSession.hasOpenSpot()) {
            throw new IllegalStateException(
                "Error: The target session is fully occupied. Transfer cannot proceed.");
        }

        verifyNoConflict(record.getParticipantCode(), newSession, recordId);

        oldSession.vacateSpot(record.getParticipantCode());
        newSession.occupySpot(record.getParticipantCode());
        record.transferSession(newSessionCode);

        return record;
    }

    /**
     * Cancels an active booking and releases the session spot.
     * The record is retained with CANCELLED state for audit.
     */
    public void cancelBooking(String recordId) {
        BookingRecord   record  = requireActiveRecord(recordId);
        ActivitySession session = requireSession(record.getSessionCode());

        session.vacateSpot(record.getParticipantCode());
        record.cancelRecord();
    }

    /**
     * Confirms attendance and stores the participant's comment.
     *
     * @param rating       integer 1–5
     * @param feedbackText free-text comment
     */
    public void submitAttendance(String recordId, int rating, String feedbackText) {
        BookingRecord   record  = requireActiveRecord(recordId);
        ActivitySession session = requireSession(record.getSessionCode());

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                "Error: Satisfaction rating must be 1–5. Received: " + rating);
        }

        record.markAsAttended();
        CommentEntry entry = new CommentEntry(
                record.getParticipantCode(), session.getSessionCode(), rating, feedbackText);
        session.appendComment(entry);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REPORTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Attendance and Ratings Report.
     * Lists each session that has at least one confirmed attendee,
     * showing headcount and mean satisfaction rating.
     */
    public String produceAttendanceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("═".repeat(92)).append("\n");
        sb.append("          FLC GROUP EXERCISE — ATTENDANCE & SATISFACTION REPORT\n");
        sb.append("═".repeat(92)).append("\n");
        sb.append(String.format("  %-12s %-24s %-10s %-13s %-6s %-10s %s%n",
                "Session", "Exercise Type", "Day", "Slot", "Week", "Attendees", "Avg Rating"));
        sb.append("  ").append("─".repeat(88)).append("\n");

        catalogue.allSessions().stream()
                .filter(s -> s.confirmedAttendanceCount() > 0)
                .sorted(Comparator.comparingInt(ActivitySession::getWeekIndex)
                        .thenComparing(ActivitySession::getDay)
                        .thenComparing(ActivitySession::getSlot))
                .forEach(s -> sb.append(String.format(
                        "  %-12s %-24s %-10s %-13s %-6d %-10d %.2f / 5%n",
                        s.getSessionCode(), s.getExerciseType(),
                        s.getDay().getDisplayLabel(), s.getSlot().getLabel(),
                        s.getWeekIndex(), s.confirmedAttendanceCount(), s.meanRating())));

        sb.append("═".repeat(92)).append("\n");
        return sb.toString();
    }

    /**
     * Income Report.
     * Totals income per exercise type (attended sessions only)
     * and highlights the highest earner.
     */
    public String produceIncomeReport() {
        Map<String, Double> incomeByType = new LinkedHashMap<>();

        for (ActivitySession s : catalogue.allSessions()) {
            if (s.confirmedAttendanceCount() == 0) continue;
            double earned = s.getCostPerHead() * s.confirmedAttendanceCount();
            incomeByType.merge(s.getExerciseType(), earned, Double::sum);
        }

        if (incomeByType.isEmpty()) {
            return "\n  No income data available — no confirmed attendances recorded yet.\n";
        }

        String topType = Collections.max(
                incomeByType.entrySet(), Map.Entry.comparingByValue()).getKey();

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("═".repeat(64)).append("\n");
        sb.append("          FLC GROUP EXERCISE — INCOME REPORT\n");
        sb.append("═".repeat(64)).append("\n");

        incomeByType.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> {
                    String badge = e.getKey().equals(topType) ? "  ◉ HIGHEST EARNER" : "";
                    sb.append(String.format("  %-24s  £%,9.2f%s%n",
                            e.getKey(), e.getValue(), badge));
                });

        sb.append("  ").append("─".repeat(60)).append("\n");
        sb.append(String.format("  Top earning exercise type : %s  (£%,.2f total)%n",
                topType, incomeByType.get(topType)));
        sb.append("═".repeat(64)).append("\n");
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  QUERY HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Active booking records (CREATED or UPDATED) for a participant. */
    public List<BookingRecord> activeRecordsFor(String participantCode) {
        return records.values().stream()
                .filter(r -> r.getParticipantCode().equals(participantCode) && r.isActive())
                .collect(Collectors.toList());
    }

    /** Full booking history (all states) for a participant. */
    public List<BookingRecord> fullHistoryFor(String participantCode) {
        return records.values().stream()
                .filter(r -> r.getParticipantCode().equals(participantCode))
                .collect(Collectors.toList());
    }

    public Optional<BookingRecord> findRecord(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    public Collection<BookingRecord> allRecords() {
        return Collections.unmodifiableCollection(records.values());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String generateRecordId() {
        return String.format("REC-%04d", idCounter++);
    }

    private Participant requireParticipant(String code) {
        return findParticipant(code).orElseThrow(
            () -> new IllegalArgumentException(
                "Error: Participant code '" + code + "' is not registered."));
    }

    private ActivitySession requireSession(String code) {
        return catalogue.findByCode(code).orElseThrow(
            () -> new IllegalArgumentException(
                "Error: Session code '" + code + "' does not exist in the catalogue."));
    }

    private BookingRecord requireActiveRecord(String recordId) {
        BookingRecord r = records.get(recordId);
        if (r == null) {
            throw new IllegalArgumentException(
                "Error: Booking record '" + recordId + "' not found.");
        }
        if (!r.isActive()) {
            throw new IllegalStateException(
                "Error: Booking " + recordId
                + " is not active — current state: " + r.getState() + ".");
        }
        return r;
    }

    /**
     * Raises an exception if the participant already has an active booking
     * at the same week/day/slot as {@code target}.
     *
     * @param excludeId if non-null, that record is skipped (used during transfer)
     */
    private void verifyNoConflict(String participantCode, ActivitySession target,
                                   String excludeId) {
        records.values().stream()
                .filter(r -> r.getParticipantCode().equals(participantCode))
                .filter(BookingRecord::isActive)
                .filter(r -> !r.getRecordId().equals(excludeId))
                .forEach(r -> {
                    ActivitySession existing =
                            catalogue.findByCode(r.getSessionCode()).orElse(null);
                    if (existing != null
                            && existing.getWeekIndex() == target.getWeekIndex()
                            && existing.getDay()        == target.getDay()
                            && existing.getSlot()       == target.getSlot()) {
                        throw new IllegalStateException(
                            "Conflict detected for selected time slot — "
                            + participantCode + " already has a booking at "
                            + target.getDay() + " "
                            + target.getSlot().getLabel()
                            + " in Week " + target.getWeekIndex() + ".");
                    }
                });
    }
}
