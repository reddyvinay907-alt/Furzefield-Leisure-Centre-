package leisure.core;

/**
 * Represents the two weekend days on which the leisure centre
 * runs its group exercise programme.
 */
public enum WeekendDay {
    SATURDAY("Saturday"),
    SUNDAY  ("Sunday");

    private final String displayLabel;

    WeekendDay(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() { return displayLabel; }

    @Override
    public String toString() { return displayLabel; }
}
