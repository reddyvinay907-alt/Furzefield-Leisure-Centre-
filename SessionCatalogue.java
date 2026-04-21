package leisure.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory catalogue of all {@link ActivitySession} objects across the
 * full eight-weekend programme.
 *
 * Provides multi-key lookup:
 *   • Primary key : sessionCode
 *   • Filter views : by day, by exercise type, by slot (conflict detection)
 */
public class SessionCatalogue {

    /** sessionCode → ActivitySession */
    private final Map<String, ActivitySession> index = new LinkedHashMap<>();

    // ── Mutation ──────────────────────────────────────────────────────────────

    public void register(ActivitySession session) {
        index.put(session.getSessionCode(), session);
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public Optional<ActivitySession> findByCode(String sessionCode) {
        return Optional.ofNullable(index.get(sessionCode));
    }

    /** All sessions on the given day, ordered by week then slot. */
    public List<ActivitySession> filterByDay(WeekendDay day) {
        return index.values().stream()
                .filter(s -> s.getDay() == day)
                .sorted(Comparator.comparingInt(ActivitySession::getWeekIndex)
                        .thenComparing(ActivitySession::getSlot))
                .collect(Collectors.toList());
    }

    /** All sessions of the given exercise type (case-insensitive). */
    public List<ActivitySession> filterByType(String exerciseType) {
        return index.values().stream()
                .filter(s -> s.getExerciseType().equalsIgnoreCase(exerciseType))
                .sorted(Comparator.comparingInt(ActivitySession::getWeekIndex)
                        .thenComparing(ActivitySession::getDay)
                        .thenComparing(ActivitySession::getSlot))
                .collect(Collectors.toList());
    }

    /** Distinct exercise type names, sorted alphabetically. */
    public List<String> allExerciseTypes() {
        return index.values().stream()
                .map(ActivitySession::getExerciseType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Collection<ActivitySession> allSessions() {
        return Collections.unmodifiableCollection(index.values());
    }

    /**
     * Finds the session occupying a specific week/day/slot —
     * used to detect time conflicts.
     */
    public Optional<ActivitySession> findBySlotKey(int weekIndex,
                                                    WeekendDay day,
                                                    SessionSlot slot) {
        return index.values().stream()
                .filter(s -> s.getWeekIndex() == weekIndex
                          && s.getDay()        == day
                          && s.getSlot()       == slot)
                .findFirst();
    }
}
