package leisure.core;

/**
 * Lifecycle states for a {@link BookingRecord}.
 *
 * CREATED  → UPDATED   (moved to a different session)
 *          → CANCELLED (withdrawn; booking ID permanently retired)
 *          → ATTENDED  (session completed; review submitted)
 *
 * Only ATTENDED records contribute to attendance counts and income reports.
 */
public enum BookingState {
    CREATED,
    UPDATED,
    CANCELLED,
    ATTENDED
}
