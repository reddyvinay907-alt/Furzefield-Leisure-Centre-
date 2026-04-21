package leisure.bootstrap;

import leisure.core.*;
import leisure.engine.BookingEngine;

/**
 * Bootstraps the FLC system with realistic sample data.
 *
 *   Participants : 10  (codes P01–P10)
 *   Sessions     : 48  (8 weekends × 6 sessions each)
 *   Comments     : 22+ (pre-confirmed with ratings and feedback)
 *
 * Cost per exercise type (fixed):
 *   Core Conditioning   £13.00
 *   Cardio Blast        £11.50
 *   Flexibility Flow    £10.00
 *   Endurance Circuit   £14.00
 */
public class DataBootstrap {

    private static final double CORE_COST      = 13.00;
    private static final double CARDIO_COST    = 11.50;
    private static final double FLEX_COST      = 10.00;
    private static final double ENDURANCE_COST = 14.00;

    public static void load(BookingEngine engine) {
        loadParticipants(engine);
        loadSessions(engine);
        loadActivityHistory(engine);
    }

    // ── Participants ──────────────────────────────────────────────────────────

    private static void loadParticipants(BookingEngine engine) {
        engine.addParticipant(new Participant("P01", "Sofia Andersen",     "sofia.a@leisure.co.uk"));
        engine.addParticipant(new Participant("P02", "Marcus Obi",         "marcus.o@leisure.co.uk"));
        engine.addParticipant(new Participant("P03", "Hana Suzuki",        "hana.s@leisure.co.uk"));
        engine.addParticipant(new Participant("P04", "Callum Fraser",      "callum.f@leisure.co.uk"));
        engine.addParticipant(new Participant("P05", "Nneka Adeyemi",      "nneka.a@leisure.co.uk"));
        engine.addParticipant(new Participant("P06", "Lukas Novak",        "lukas.n@leisure.co.uk"));
        engine.addParticipant(new Participant("P07", "Ines Delacroix",     "ines.d@leisure.co.uk"));
        engine.addParticipant(new Participant("P08", "Rajan Krishnasamy",  "rajan.k@leisure.co.uk"));
        engine.addParticipant(new Participant("P09", "Astrid Lindqvist",   "astrid.l@leisure.co.uk"));
        engine.addParticipant(new Participant("P10", "Emeka Okafor",       "emeka.o@leisure.co.uk"));
    }

    // ── Sessions (8 weekends × 6 per weekend = 48 total) ─────────────────────
    //
    //  Saturday:  Morning   → Core Conditioning
    //             Afternoon → Cardio Blast
    //             Evening   → Endurance Circuit
    //  Sunday:    Morning   → Flexibility Flow
    //             Afternoon → Core Conditioning (second weekly run)
    //             Evening   → Cardio Blast (second weekly run)

    private static void loadSessions(BookingEngine engine) {
        for (int w = 1; w <= 8; w++) {
            String pfx = "W" + w;
            engine.addSession(new ActivitySession(pfx + "SAM", "Core Conditioning",
                    WeekendDay.SATURDAY, SessionSlot.MORNING,   w, CORE_COST));
            engine.addSession(new ActivitySession(pfx + "SAA", "Cardio Blast",
                    WeekendDay.SATURDAY, SessionSlot.AFTERNOON, w, CARDIO_COST));
            engine.addSession(new ActivitySession(pfx + "SAE", "Endurance Circuit",
                    WeekendDay.SATURDAY, SessionSlot.EVENING,   w, ENDURANCE_COST));
            engine.addSession(new ActivitySession(pfx + "SUM", "Flexibility Flow",
                    WeekendDay.SUNDAY,   SessionSlot.MORNING,   w, FLEX_COST));
            engine.addSession(new ActivitySession(pfx + "SUA", "Core Conditioning",
                    WeekendDay.SUNDAY,   SessionSlot.AFTERNOON, w, CORE_COST));
            engine.addSession(new ActivitySession(pfx + "SUE", "Cardio Blast",
                    WeekendDay.SUNDAY,   SessionSlot.EVENING,   w, CARDIO_COST));
        }
    }

    // ── Activity history ──────────────────────────────────────────────────────

    private static void loadActivityHistory(BookingEngine engine) {
        // Week 1 — Saturday Morning: Core Conditioning
        attend(engine, "P01", "W1SAM", 5, "Excellent workout — really challenged my core stability.");
        attend(engine, "P02", "W1SAM", 4, "Great session, very well paced.");
        attend(engine, "P03", "W1SAM", 5, "Instructor was fantastic, felt results immediately.");

        // Week 1 — Saturday Afternoon: Cardio Blast
        attend(engine, "P04", "W1SAA", 4, "High energy and motivating throughout.");
        attend(engine, "P05", "W1SAA", 3, "Good but slightly too fast at the start.");

        // Week 1 — Sunday Morning: Flexibility Flow
        attend(engine, "P06", "W1SUM", 5, "Perfect for post-week recovery. Felt amazing after.");
        attend(engine, "P07", "W1SUM", 4, "Very calming and well structured.");

        // Week 2 — Saturday Evening: Endurance Circuit
        attend(engine, "P01", "W2SAE", 4, "Pushed me to my limit — great circuit design.");
        attend(engine, "P08", "W2SAE", 5, "Best endurance session I have done. Outstanding.");
        attend(engine, "P09", "W2SAE", 3, "Tough going but fair. Would come back.");

        // Week 2 — Sunday Afternoon: Core Conditioning
        attend(engine, "P10", "W2SUA", 5, "Consistent quality. A real highlight of my weekend.");
        attend(engine, "P02", "W2SUA", 4, "Strong focus on technique — appreciated that.");

        // Week 3 — Saturday Morning: Core Conditioning
        attend(engine, "P03", "W3SAM", 5, "Second time attending — still discovering new exercises.");
        attend(engine, "P05", "W3SAM", 4, "Really solid core workout. No complaints.");

        // Week 3 — Sunday Evening: Cardio Blast
        attend(engine, "P06", "W3SUE", 3, "Fun session but music volume was too high.");
        attend(engine, "P07", "W3SUE", 4, "Loved the interval structure. Very effective.");

        // Week 4 — Saturday Afternoon: Cardio Blast
        attend(engine, "P08", "W4SAA", 5, "Absolutely nailed it today. Great class.");
        attend(engine, "P09", "W4SAA", 4, "Instructor kept everyone engaged all session.");

        // Week 4 — Sunday Morning: Flexibility Flow
        attend(engine, "P10", "W4SUM", 5, "Deeply restorative. This is my favourite session type.");
        attend(engine, "P01", "W4SUM", 4, "Good variety of stretches. Well-timed throughout.");

        // Week 5 — Saturday Evening: Endurance Circuit (extra)
        attend(engine, "P04", "W5SAE", 5, "Surpassed my own expectations today.");
        attend(engine, "P02", "W5SAE", 4, "Challenging and rewarding. Will book again.");

        // Active (not yet attended) bookings
        silentBook(engine, "P03", "W5SAA");  // Cardio Blast week 5
        silentBook(engine, "P05", "W5SUA");  // Core Conditioning week 5
        silentBook(engine, "P06", "W6SAM");  // Core Conditioning week 6
        silentBook(engine, "P07", "W6SAE");  // Endurance Circuit week 6
        silentBook(engine, "P08", "W7SAM");  // Core Conditioning week 7
        silentBook(engine, "P09", "W7SUM");  // Flexibility Flow week 7
        silentBook(engine, "P10", "W8SAA");  // Cardio Blast week 8
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Creates a booking and immediately marks it attended with a comment. */
    private static void attend(BookingEngine engine, String participantCode,
                                String sessionCode, int rating, String feedback) {
        try {
            BookingRecord r = engine.createBooking(participantCode, sessionCode);
            engine.submitAttendance(r.getRecordId(), rating, feedback);
        } catch (Exception e) {
            System.err.println("[DataBootstrap] Seed failed for "
                + participantCode + "/" + sessionCode + ": " + e.getMessage());
        }
    }

    /** Creates an active booking without attending. */
    private static void silentBook(BookingEngine engine,
                                    String participantCode, String sessionCode) {
        try {
            engine.createBooking(participantCode, sessionCode);
        } catch (Exception e) {
            System.err.println("[DataBootstrap] Booking failed for "
                + participantCode + "/" + sessionCode + ": " + e.getMessage());
        }
    }
}
