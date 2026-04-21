package leisure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single scheduled group exercise session at the leisure centre.
 *
 * Business rules enforced here:
 *   - Maximum {@value #CAPACITY_LIMIT} participants per session
 *   - Tuition cost is fixed per exercise type, not per slot
 *   - Comment entries are only accepted after attendance is confirmed
 *     (enforced upstream by {@code BookingEngine})
 */
public class ActivitySession {

    public static final int CAPACITY_LIMIT = 4;

    private final String      sessionCode;     // e.g. "W3SAM"
    private final String      exerciseType;    // e.g. "Core Conditioning"
    private final WeekendDay  day;
    private final SessionSlot slot;
    private final int         weekIndex;       // 1 – 8
    private final double      costPerHead;

    /** Participant codes of all active (non-cancelled) bookings. */
    private final List<String>       activeParticipants = new ArrayList<>();

    /** All comment entries submitted after attendance. */
    private final List<CommentEntry> commentLog         = new ArrayList<>();

    public ActivitySession(String sessionCode, String exerciseType,
                           WeekendDay day, SessionSlot slot,
                           int weekIndex, double costPerHead) {
        this.sessionCode  = sessionCode;
        this.exerciseType = exerciseType;
        this.day          = day;
        this.slot         = slot;
        this.weekIndex    = weekIndex;
        this.costPerHead  = costPerHead;
    }

    // ── Capacity management ───────────────────────────────────────────────────

    public boolean hasOpenSpot() {
        return activeParticipants.size() < CAPACITY_LIMIT;
    }

    public int openSpots() {
        return CAPACITY_LIMIT - activeParticipants.size();
    }

    public boolean isAlreadyEnrolled(String participantCode) {
        return activeParticipants.contains(participantCode);
    }

    /**
     * Reserves a spot for a participant.
     * @throws IllegalStateException if the session is fully occupied
     */
    public void occupySpot(String participantCode) {
        if (!hasOpenSpot()) {
            throw new IllegalStateException("Session is fully occupied.");
        }
        if (isAlreadyEnrolled(participantCode)) {
            throw new IllegalStateException("Participant is already enrolled in this session.");
        }
        activeParticipants.add(participantCode);
    }

    /**
     * Releases the participant's spot.
     * Called on cancellation or session transfer.
     */
    public void vacateSpot(String participantCode) {
        activeParticipants.remove(participantCode);
    }

    // ── Comment management ────────────────────────────────────────────────────

    public void appendComment(CommentEntry entry) {
        commentLog.add(entry);
    }

    /**
     * Mean satisfaction rating across all submitted comment entries.
     * Returns 0.0 if no entries exist.
     */
    public double meanRating() {
        if (commentLog.isEmpty()) return 0.0;
        return commentLog.stream()
                .mapToInt(CommentEntry::getSatisfactionRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Confirmed attendance count — proxied by comment entry count,
     * since entries are only created on attendance.
     */
    public int confirmedAttendanceCount() {
        return commentLog.size();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String      getSessionCode()  { return sessionCode;  }
    public String      getExerciseType() { return exerciseType; }
    public WeekendDay  getDay()          { return day;          }
    public SessionSlot getSlot()         { return slot;         }
    public int         getWeekIndex()    { return weekIndex;    }
    public double      getCostPerHead()  { return costPerHead;  }

    public List<String>       getActiveParticipants() {
        return Collections.unmodifiableList(activeParticipants);
    }
    public List<CommentEntry> getCommentLog() {
        return Collections.unmodifiableList(commentLog);
    }

    @Override
    public String toString() {
        return String.format(
            "%-10s | %-22s | %-10s | %-12s | Wk%-2d | £%5.2f | %d/%d spots",
            sessionCode, exerciseType, day.getDisplayLabel(),
            slot.getLabel(), weekIndex, costPerHead,
            activeParticipants.size(), CAPACITY_LIMIT
        );
    }
}
