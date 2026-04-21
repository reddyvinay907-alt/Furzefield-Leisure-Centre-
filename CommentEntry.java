package leisure.core;

/**
 * Post-attendance comment submitted by a participant for a completed
 * {@link ActivitySession}.
 *
 * Satisfaction scale:
 *   1 = Strongly Dissatisfied
 *   2 = Dissatisfied
 *   3 = Neutral
 *   4 = Satisfied
 *   5 = Strongly Satisfied
 *
 * This is an immutable value object — fields are validated at construction.
 */
public class CommentEntry {

    private final String participantCode;
    private final String sessionCode;
    private final int    satisfactionRating;   // 1 – 5
    private final String feedbackText;

    public CommentEntry(String participantCode, String sessionCode,
                        int satisfactionRating, String feedbackText) {
        if (satisfactionRating < 1 || satisfactionRating > 5) {
            throw new IllegalArgumentException(
                "Satisfaction rating must be 1–5. Received: " + satisfactionRating);
        }
        this.participantCode   = participantCode;
        this.sessionCode       = sessionCode;
        this.satisfactionRating = satisfactionRating;
        this.feedbackText      = feedbackText;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getParticipantCode()    { return participantCode;    }
    public String getSessionCode()        { return sessionCode;        }
    public int    getSatisfactionRating() { return satisfactionRating; }
    public String getFeedbackText()       { return feedbackText;       }

    /** Returns a human-readable label for the numeric rating. */
    public String getRatingLabel() {
        return switch (satisfactionRating) {
            case 1 -> "Strongly Dissatisfied";
            case 2 -> "Dissatisfied";
            case 3 -> "Neutral";
            case 4 -> "Satisfied";
            case 5 -> "Strongly Satisfied";
            default -> "Unknown";
        };
    }

    @Override
    public String toString() {
        return String.format("%d/5 (%s) — \"%s\"",
                satisfactionRating, getRatingLabel(), feedbackText);
    }
}
