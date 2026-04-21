package leisure;

import leisure.bootstrap.DataBootstrap;
import leisure.core.*;
import leisure.engine.BookingEngine;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for the FLC Group Exercise Booking System.
 *
 * Coverage:
 *   — Happy-path booking creation
 *   — Booking ID format and uniqueness
 *   — Capacity limit enforcement (max 4)
 *   — Duplicate booking prevention
 *   — Time-conflict detection
 *   — Session transfer (modify)
 *   — Booking cancellation
 *   — Attendance submission and comment recording
 *   — Rating validation (1–5)
 *   — Attendance and income report generation
 *   — DataBootstrap completeness verification
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FLCBookingSystemTest {

    private BookingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BookingEngine();

        // Five test participants
        engine.addParticipant(new Participant("X01", "Test Alpha",   "x01@t.com"));
        engine.addParticipant(new Participant("X02", "Test Beta",    "x02@t.com"));
        engine.addParticipant(new Participant("X03", "Test Gamma",   "x03@t.com"));
        engine.addParticipant(new Participant("X04", "Test Delta",   "x04@t.com"));
        engine.addParticipant(new Participant("X05", "Test Epsilon", "x05@t.com"));

        // Five test sessions
        engine.addSession(new ActivitySession("TS01", "Core Conditioning",
                WeekendDay.SATURDAY, SessionSlot.MORNING,   1, 13.00));
        engine.addSession(new ActivitySession("TS02", "Cardio Blast",
                WeekendDay.SATURDAY, SessionSlot.AFTERNOON, 1, 11.50));
        engine.addSession(new ActivitySession("TS03", "Endurance Circuit",
                WeekendDay.SATURDAY, SessionSlot.EVENING,   1, 14.00));
        engine.addSession(new ActivitySession("TS04", "Core Conditioning",
                WeekendDay.SATURDAY, SessionSlot.MORNING,   2, 13.00));
        engine.addSession(new ActivitySession("TS05", "Flexibility Flow",
                WeekendDay.SUNDAY,   SessionSlot.MORNING,   1, 10.00));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING CREATION — HAPPY PATH
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("Create booking — returns record with CREATED state")
    void testCreateBookingSuccess() {
        BookingRecord r = engine.createBooking("X01", "TS01");

        assertNotNull(r);
        assertEquals("X01",  r.getParticipantCode());
        assertEquals("TS01", r.getSessionCode());
        assertEquals(BookingState.CREATED, r.getState());
        assertTrue(r.isActive());
    }

    @Test @Order(2)
    @DisplayName("Booking IDs are unique across successive bookings")
    void testBookingIdUniqueness() {
        BookingRecord r1 = engine.createBooking("X01", "TS01");
        BookingRecord r2 = engine.createBooking("X02", "TS01");
        assertNotEquals(r1.getRecordId(), r2.getRecordId());
    }

    @Test @Order(3)
    @DisplayName("Booking ID follows REC-XXXX format")
    void testBookingIdFormat() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        assertTrue(r.getRecordId().matches("REC-\\d{4}"),
                "ID should match REC-XXXX pattern");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CAPACITY ENFORCEMENT
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(4)
    @DisplayName("Capacity limit enforced — 5th booking is rejected")
    void testCapacityLimit() {
        engine.createBooking("X01", "TS01");
        engine.createBooking("X02", "TS01");
        engine.createBooking("X03", "TS01");
        engine.createBooking("X04", "TS01");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> engine.createBooking("X05", "TS01"));

        assertTrue(ex.getMessage().contains("fully occupied"),
                "Error should state session is fully occupied");
    }

    @Test @Order(5)
    @DisplayName("Open spot count decrements correctly with each booking")
    void testOpenSpotDecrement() {
        ActivitySession s = engine.getCatalogue().findByCode("TS01").orElseThrow();
        assertEquals(4, s.openSpots());

        engine.createBooking("X01", "TS01");
        assertEquals(3, s.openSpots());

        engine.createBooking("X02", "TS01");
        assertEquals(2, s.openSpots());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DUPLICATE BOOKING PREVENTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(6)
    @DisplayName("Duplicate booking for same participant and session is rejected")
    void testDuplicateBookingRejected() {
        engine.createBooking("X01", "TS01");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> engine.createBooking("X01", "TS01"));

        assertTrue(ex.getMessage().toLowerCase().contains("already has an active booking"),
                "Error should mention existing booking");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIME-CONFLICT DETECTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(7)
    @DisplayName("Time conflict detected for same participant, week, day, and slot")
    void testTimeConflictDetected() {
        engine.addSession(new ActivitySession("TCON", "Flexibility Flow",
                WeekendDay.SATURDAY, SessionSlot.MORNING, 1, 10.00));

        engine.createBooking("X01", "TS01"); // Saturday Morning Week 1

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> engine.createBooking("X01", "TCON")); // same slot

        assertTrue(ex.getMessage().toLowerCase().contains("conflict"),
                "Error should mention conflict");
    }

    @Test @Order(8)
    @DisplayName("No conflict for same day but different session slots")
    void testNoConflictDifferentSlots() {
        engine.createBooking("X01", "TS01"); // Saturday Morning
        BookingRecord r = engine.createBooking("X01", "TS02"); // Saturday Afternoon
        assertNotNull(r, "Different slot should be permitted");
    }

    @Test @Order(9)
    @DisplayName("No conflict for same day/slot in different weeks")
    void testNoConflictDifferentWeeks() {
        engine.createBooking("X01", "TS01"); // Week 1 Sat Morning
        BookingRecord r = engine.createBooking("X01", "TS04"); // Week 2 Sat Morning
        assertNotNull(r, "Different week should be permitted");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SESSION TRANSFER (MODIFY)
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("Transfer updates session code and sets UPDATED state")
    void testTransferBookingSuccess() {
        BookingRecord original = engine.createBooking("X01", "TS01");
        String        recId    = original.getRecordId();

        engine.transferBooking(recId, "TS05"); // Sunday Morning

        BookingRecord updated = engine.findRecord(recId).orElseThrow();
        assertEquals("TS05",              updated.getSessionCode());
        assertEquals(BookingState.UPDATED, updated.getState());

        // Old session releases spot
        ActivitySession oldS = engine.getCatalogue().findByCode("TS01").orElseThrow();
        assertFalse(oldS.isAlreadyEnrolled("X01"), "X01 should be removed from TS01");

        // New session gains spot
        ActivitySession newS = engine.getCatalogue().findByCode("TS05").orElseThrow();
        assertTrue(newS.isAlreadyEnrolled("X01"), "X01 should be enrolled in TS05");
    }

    @Test @Order(11)
    @DisplayName("Transfer to a full session is rejected")
    void testTransferToFullSession() {
        engine.createBooking("X01", "TS02");
        engine.createBooking("X02", "TS02");
        engine.createBooking("X03", "TS02");
        engine.createBooking("X04", "TS02"); // TS02 full

        BookingRecord r = engine.createBooking("X05", "TS01");
        assertThrows(IllegalStateException.class,
                () -> engine.transferBooking(r.getRecordId(), "TS02"));
    }

    @Test @Order(12)
    @DisplayName("Transfer to the same session throws IllegalArgumentException")
    void testTransferToSameSession() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        assertThrows(IllegalArgumentException.class,
                () -> engine.transferBooking(r.getRecordId(), "TS01"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCELLATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(13)
    @DisplayName("Cancel booking — sets CANCELLED state and releases spot")
    void testCancelBookingSuccess() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        engine.cancelBooking(r.getRecordId());

        BookingRecord cancelled = engine.findRecord(r.getRecordId()).orElseThrow();
        assertEquals(BookingState.CANCELLED, cancelled.getState());
        assertFalse(cancelled.isActive());

        ActivitySession s = engine.getCatalogue().findByCode("TS01").orElseThrow();
        assertFalse(s.isAlreadyEnrolled("X01"), "Spot should be released");
    }

    @Test @Order(14)
    @DisplayName("Double-cancellation throws IllegalStateException")
    void testDoubleCancellationRejected() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        engine.cancelBooking(r.getRecordId());

        assertThrows(IllegalStateException.class,
                () -> engine.cancelBooking(r.getRecordId()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ATTENDANCE & COMMENTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(15)
    @DisplayName("Submit attendance — sets ATTENDED state and stores comment")
    void testSubmitAttendanceSuccess() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        engine.submitAttendance(r.getRecordId(), 5, "Excellent session!");

        BookingRecord attended = engine.findRecord(r.getRecordId()).orElseThrow();
        assertEquals(BookingState.ATTENDED, attended.getState());
        assertTrue(attended.isAttended());

        ActivitySession s = engine.getCatalogue().findByCode("TS01").orElseThrow();
        assertEquals(1,   s.confirmedAttendanceCount());
        assertEquals(5.0, s.meanRating(), 0.001);
    }

    @Test @Order(16)
    @DisplayName("Mean rating calculated correctly from multiple entries")
    void testMeanRatingCalculation() {
        BookingRecord r1 = engine.createBooking("X01", "TS01");
        BookingRecord r2 = engine.createBooking("X02", "TS01");
        BookingRecord r3 = engine.createBooking("X03", "TS01");

        engine.submitAttendance(r1.getRecordId(), 5, "Excellent");
        engine.submitAttendance(r2.getRecordId(), 3, "Average");
        engine.submitAttendance(r3.getRecordId(), 4, "Good");

        ActivitySession s = engine.getCatalogue().findByCode("TS01").orElseThrow();
        assertEquals(4.0, s.meanRating(), 0.001, "(5+3+4)/3 = 4.0");
    }

    @Test @Order(17)
    @DisplayName("Rating of 0 is rejected with IllegalArgumentException")
    void testRatingZeroRejected() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        assertThrows(IllegalArgumentException.class,
                () -> engine.submitAttendance(r.getRecordId(), 0, "x"));
    }

    @Test @Order(18)
    @DisplayName("Rating of 6 is rejected with IllegalArgumentException")
    void testRatingSixRejected() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        assertThrows(IllegalArgumentException.class,
                () -> engine.submitAttendance(r.getRecordId(), 6, "x"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REPORTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(19)
    @DisplayName("Income report identifies highest earner")
    void testIncomeReportHighestEarner() {
        BookingRecord r = engine.createBooking("X01", "TS01"); // Core Conditioning £13
        engine.submitAttendance(r.getRecordId(), 5, "Great");

        String report = engine.produceIncomeReport();
        assertTrue(report.contains("HIGHEST EARNER"),   "Report should flag the top earner");
        assertTrue(report.contains("Core Conditioning"), "Core Conditioning should appear");
    }

    @Test @Order(20)
    @DisplayName("Attendance report contains session code after attendance")
    void testAttendanceReportContent() {
        BookingRecord r = engine.createBooking("X01", "TS01");
        engine.submitAttendance(r.getRecordId(), 4, "Good session");

        String report = engine.produceAttendanceReport();
        assertTrue(report.contains("TS01"),             "Report should list session code");
        assertTrue(report.contains("Core Conditioning"), "Report should show exercise type");
    }

    @Test @Order(21)
    @DisplayName("Non-attended bookings are excluded from income report")
    void testNonAttendedExcludedFromIncome() {
        engine.createBooking("X01", "TS01"); // booked but not attended

        String report = engine.produceIncomeReport();
        assertFalse(report.contains("Core Conditioning"),
                "Unattended sessions should not appear in income");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCHEDULE BROWSING
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(22)
    @DisplayName("Browse by day returns only Saturday sessions")
    void testBrowseByDaySaturday() {
        List<ActivitySession> results = engine.viewByDay(WeekendDay.SATURDAY);
        assertFalse(results.isEmpty());
        results.forEach(s -> assertEquals(WeekendDay.SATURDAY, s.getDay()));
    }

    @Test @Order(23)
    @DisplayName("Browse by exercise type returns only matching sessions")
    void testBrowseByTypeCoreConditioning() {
        List<ActivitySession> results = engine.viewByType("Core Conditioning");
        assertFalse(results.isEmpty());
        results.forEach(s -> assertEquals("Core Conditioning", s.getExerciseType()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATABOOTSTRAP VERIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(24)
    @DisplayName("DataBootstrap loads 10 participants, 48 sessions, at least 22 comments")
    void testDataBootstrapCompleteness() {
        BookingEngine seeded = new BookingEngine();
        DataBootstrap.load(seeded);

        assertEquals(10, seeded.listAllParticipants().size(),
                "Exactly 10 participants required");

        long sessionCount = seeded.getCatalogue().allSessions().size();
        assertEquals(48, sessionCount,
                "Exactly 48 sessions required (8 weekends × 6)");

        long commentCount = seeded.getCatalogue().allSessions().stream()
                .mapToLong(s -> s.getCommentLog().size())
                .sum();
        assertTrue(commentCount >= 22,
                "At least 22 comments required; found: " + commentCount);
    }

    @Test @Order(25)
    @DisplayName("DataBootstrap creates valid participant codes P01–P10")
    void testBootstrapParticipantCodes() {
        BookingEngine seeded = new BookingEngine();
        DataBootstrap.load(seeded);

        assertTrue(seeded.findParticipant("P01").isPresent(), "P01 must exist");
        assertTrue(seeded.findParticipant("P10").isPresent(), "P10 must exist");
        assertFalse(seeded.findParticipant("P11").isPresent(), "P11 must not exist");
    }
}
