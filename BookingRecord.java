package leisure.core;

/**
 * A booking record linking a {@link Participant} to an {@link ActivitySession}.
 *
 * Booking ID format: REC-XXXX (e.g. REC-0001)
 * IDs are generated sequentially and NEVER reused after cancellation.
 *
 * Lifecycle:
 *   CREATED → UPDATED   (session transferred)
 *           → CANCELLED (withdrawn; ID retired)
 *           → ATTENDED  (session completed; comment submitted)
 */
public class BookingRecord {

    private final String       recordId;
    private final String       participantCode;
    private       String       sessionCode;
    private       BookingState state;

    public BookingRecord(String recordId, String participantCode, String sessionCode) {
        this.recordId        = recordId;
        this.participantCode = participantCode;
        this.sessionCode     = sessionCode;
        this.state           = BookingState.CREATED;
    }

    // ── Transitions ───────────────────────────────────────────────────────────

    /** Transfers this record to a different session. */
    public void transferSession(String newSessionCode) {
        this.sessionCode = newSessionCode;
        this.state       = BookingState.UPDATED;
    }

    public void cancelRecord() {
        this.state = BookingState.CANCELLED;
    }

    public void markAsAttended() {
        this.state = BookingState.ATTENDED;
    }

    // ── Predicates ────────────────────────────────────────────────────────────

    /** True when the record is still actionable (CREATED or UPDATED). */
    public boolean isActive() {
        return state == BookingState.CREATED || state == BookingState.UPDATED;
    }

    public boolean isCancelled() { return state == BookingState.CANCELLED; }
    public boolean isAttended()  { return state == BookingState.ATTENDED;  }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String       getRecordId()        { return recordId;        }
    public String       getParticipantCode() { return participantCode; }
    public String       getSessionCode()     { return sessionCode;     }
    public BookingState getState()           { return state;           }

    @Override
    public String toString() {
        return String.format("BookingRecord[%s]  Participant:%s  Session:%s  State:%s",
                recordId, participantCode, sessionCode, state);
    }
}
