package leisure.core;

/**
 * The three timed sessions available within each weekend day.
 *
 * A participant may not hold two active bookings that share the same
 * WeekendDay, weekIndex, and SessionSlot — this constitutes a time conflict.
 */
public enum SessionSlot {
    MORNING  ("Morning",   "08:30"),
    AFTERNOON("Afternoon", "13:00"),
    EVENING  ("Evening",   "18:30");

    private final String label;
    private final String startTime;

    SessionSlot(String label, String startTime) {
        this.label     = label;
        this.startTime = startTime;
    }

    public String getLabel()     { return label;     }
    public String getStartTime() { return startTime; }

    @Override
    public String toString() { return label + " (" + startTime + ")"; }
}
