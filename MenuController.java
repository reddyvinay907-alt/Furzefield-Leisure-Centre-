package leisure.console;

import leisure.core.*;
import leisure.engine.BookingEngine;

import java.util.*;

/**
 * Menu-driven console interface for the FLC Booking System.
 *
 * All business logic is delegated to {@link BookingEngine}.
 * This class is responsible solely for I/O: reading validated input
 * and displaying formatted output.
 */
public class MenuController {

    private final BookingEngine engine;
    private final Scanner       kbd = new Scanner(System.in);

    public MenuController(BookingEngine engine) {
        this.engine = engine;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LAUNCH
    // ══════════════════════════════════════════════════════════════════════════

    public void start() {
        showBanner();
        boolean running = true;
        while (running) {
            showMainMenu();
            int choice = readInt("  Enter your choice: ", 0, 9);
            switch (choice) {
                case 1 -> browseSchedule();
                case 2 -> createNewBooking();
                case 3 -> modifyExistingBooking();
                case 4 -> cancelExistingBooking();
                case 5 -> recordAttendance();
                case 6 -> showMyHistory();
                case 7 -> showParticipantDirectory();
                case 8 -> System.out.println(engine.produceAttendanceReport());
                case 9 -> System.out.println(engine.produceIncomeReport());
                case 0 -> running = false;
            }
        }
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║  Thank you for using the FLC Booking System.         ║");
        System.out.println("  ║  Keep active. See you next weekend!                  ║");
        System.out.println("  ╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FEATURE HANDLERS
    // ══════════════════════════════════════════════════════════════════════════

    // ── 1. Browse Schedule ────────────────────────────────────────────────────

    private void browseSchedule() {
        sectionTitle("BROWSE SESSION SCHEDULE");
        System.out.println("  1.  Filter by weekend day");
        System.out.println("  2.  Filter by exercise type");
        int choice = readInt("  Select filter: ", 1, 2);

        if (choice == 1) {
            WeekendDay day = selectDay();
            printSessionGrid(engine.viewByDay(day), "Sessions on " + day);
        } else {
            String type = selectExerciseType();
            printSessionGrid(engine.viewByType(type), "All '" + type + "' sessions");
        }
    }

    // ── 2. Create Booking ─────────────────────────────────────────────────────

    private void createNewBooking() {
        sectionTitle("CREATE NEW BOOKING");
        String participantCode = promptParticipantCode();
        printAvailableSessions();
        String sessionCode = readLine("  Enter Session Code to book: ").toUpperCase();

        try {
            BookingRecord r = engine.createBooking(participantCode, sessionCode);
            showSuccess("Booking completed successfully!  Reference: " + r.getRecordId());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ── 3. Modify Booking ─────────────────────────────────────────────────────

    private void modifyExistingBooking() {
        sectionTitle("MODIFY AN EXISTING BOOKING");
        String participantCode = promptParticipantCode();
        List<BookingRecord> active = engine.activeRecordsFor(participantCode);

        if (active.isEmpty()) {
            showError("No active bookings found for participant " + participantCode + ".");
            return;
        }

        printRecordList(active);
        String recordId = readLine("  Enter Booking Reference to modify: ").toUpperCase();
        printAvailableSessions();
        String newCode = readLine("  Enter target Session Code: ").toUpperCase();

        try {
            BookingRecord r = engine.transferBooking(recordId, newCode);
            showSuccess("Booking " + r.getRecordId() + " successfully moved to session " + newCode + ".");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ── 4. Cancel Booking ─────────────────────────────────────────────────────

    private void cancelExistingBooking() {
        sectionTitle("CANCEL A BOOKING");
        String participantCode = promptParticipantCode();
        List<BookingRecord> active = engine.activeRecordsFor(participantCode);

        if (active.isEmpty()) {
            showError("No active bookings found for participant " + participantCode + ".");
            return;
        }

        printRecordList(active);
        String recordId = readLine("  Enter Booking Reference to cancel: ").toUpperCase();

        try {
            engine.cancelBooking(recordId);
            showSuccess("Booking " + recordId + " has been cancelled. Spot released.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ── 5. Record Attendance ──────────────────────────────────────────────────

    private void recordAttendance() {
        sectionTitle("RECORD ATTENDANCE & SUBMIT FEEDBACK");
        String participantCode = promptParticipantCode();
        List<BookingRecord> active = engine.activeRecordsFor(participantCode);

        if (active.isEmpty()) {
            showError("No active bookings found for participant " + participantCode + ".");
            return;
        }

        printRecordList(active);
        String recordId   = readLine("  Enter Booking Reference to mark attended: ").toUpperCase();
        int    rating     = readInt("  Satisfaction rating (1 = Strongly Dissatisfied … 5 = Strongly Satisfied): ", 1, 5);
        String feedback   = readLine("  Your feedback on the session: ");

        try {
            engine.submitAttendance(recordId, rating, feedback);
            showSuccess("Attendance recorded. Thank you for your feedback!");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // ── 6. My Booking History ─────────────────────────────────────────────────

    private void showMyHistory() {
        sectionTitle("MY BOOKING HISTORY");
        String participantCode = promptParticipantCode();
        List<BookingRecord> history = engine.fullHistoryFor(participantCode);

        if (history.isEmpty()) {
            System.out.println("\n  No booking records found for " + participantCode + ".\n");
            return;
        }

        System.out.printf("%n  %-12s %-12s %-12s%n", "Reference", "Session", "State");
        System.out.println("  " + "─".repeat(38));
        history.forEach(r ->
            System.out.printf("  %-12s %-12s %-12s%n",
                    r.getRecordId(), r.getSessionCode(), r.getState()));
        System.out.println();
    }

    // ── 7. Participant Directory ───────────────────────────────────────────────

    private void showParticipantDirectory() {
        sectionTitle("PARTICIPANT DIRECTORY");
        engine.listAllParticipants().forEach(p -> System.out.println("  " + p));
        System.out.println();
    }

    // ── 8 & 9 handled inline in start() ──────────────────────────────────────

    // ══════════════════════════════════════════════════════════════════════════
    //  DISPLAY HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void showBanner() {
        System.out.println();
        System.out.println("  ╔════════════════════════════════════════════════════════╗");
        System.out.println("  ║      FURZEFIELD LEISURE CENTRE                        ║");
        System.out.println("  ║      Group Exercise Booking System  ·  v4.0           ║");
        System.out.println("  ║      Weekend Wellness. Every Week.                    ║");
        System.out.println("  ╚════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void showMainMenu() {
        System.out.println("  ┌──────────────────────────────────────────────────────┐");
        System.out.println("  │                    MAIN MENU                         │");
        System.out.println("  ├──────────────────────────────────────────────────────┤");
        System.out.println("  │  1.  Browse Session Schedule                         │");
        System.out.println("  │  2.  Create New Booking                              │");
        System.out.println("  │  3.  Modify Existing Booking                         │");
        System.out.println("  │  4.  Cancel a Booking                                │");
        System.out.println("  │  5.  Record Attendance & Submit Feedback             │");
        System.out.println("  │  6.  My Booking History                              │");
        System.out.println("  │  7.  Participant Directory                            │");
        System.out.println("  │  8.  Attendance & Satisfaction Report                │");
        System.out.println("  │  9.  Income Report                                   │");
        System.out.println("  │  0.  Exit                                            │");
        System.out.println("  └──────────────────────────────────────────────────────┘");
    }

    private void sectionTitle(String title) {
        System.out.println("\n  ┄┄ " + title + " " + "┄".repeat(Math.max(0, 52 - title.length())));
        System.out.println();
    }

    private void showSuccess(String msg) {
        System.out.println("\n  ✔  " + msg + "\n");
    }

    private void showError(String msg) {
        System.out.println("\n  ✖  " + msg + "\n");
    }

    private void printSessionGrid(List<ActivitySession> sessions, String heading) {
        System.out.println("\n  " + heading);
        System.out.printf("%n  %-12s %-24s %-10s %-13s %-6s %-9s %-5s%n",
                "Code", "Exercise Type", "Day", "Slot", "Week", "Cost", "Spots");
        System.out.println("  " + "─".repeat(82));
        sessions.forEach(s ->
            System.out.printf("  %-12s %-24s %-10s %-13s %-6d £%-8.2f %d/%d%n",
                    s.getSessionCode(), s.getExerciseType(),
                    s.getDay().getDisplayLabel(), s.getSlot().getLabel(),
                    s.getWeekIndex(), s.getCostPerHead(),
                    s.getActiveParticipants().size(), ActivitySession.CAPACITY_LIMIT));
        System.out.println();
    }

    private void printAvailableSessions() {
        System.out.println("\n  Sessions with open spots:");
        System.out.printf("  %-12s %-24s %-10s %-13s %-6s %-9s %-5s%n",
                "Code", "Exercise Type", "Day", "Slot", "Week", "Cost", "Spots");
        System.out.println("  " + "─".repeat(82));
        engine.getCatalogue().allSessions().stream()
                .filter(ActivitySession::hasOpenSpot)
                .forEach(s ->
                    System.out.printf("  %-12s %-24s %-10s %-13s %-6d £%-8.2f %d/%d%n",
                            s.getSessionCode(), s.getExerciseType(),
                            s.getDay().getDisplayLabel(), s.getSlot().getLabel(),
                            s.getWeekIndex(), s.getCostPerHead(),
                            s.getActiveParticipants().size(), ActivitySession.CAPACITY_LIMIT));
        System.out.println();
    }

    private void printRecordList(List<BookingRecord> records) {
        System.out.printf("%n  %-12s %-12s %-12s%n", "Reference", "Session", "State");
        System.out.println("  " + "─".repeat(38));
        records.forEach(r ->
            System.out.printf("  %-12s %-12s %-12s%n",
                    r.getRecordId(), r.getSessionCode(), r.getState()));
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INPUT HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private WeekendDay selectDay() {
        System.out.println("  1.  Saturday");
        System.out.println("  2.  Sunday");
        return readInt("  Select: ", 1, 2) == 1 ? WeekendDay.SATURDAY : WeekendDay.SUNDAY;
    }

    private String selectExerciseType() {
        List<String> types = engine.listExerciseTypes();
        System.out.println("  Available exercise types:");
        for (int i = 0; i < types.size(); i++) {
            System.out.printf("  %d.  %s%n", i + 1, types.get(i));
        }
        int idx = readInt("  Select: ", 1, types.size());
        return types.get(idx - 1);
    }

    private String promptParticipantCode() {
        System.out.println("  Participants: P01 – P10");
        return readLine("  Enter Participant Code: ").toUpperCase();
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        String line = kbd.nextLine().trim();
        while (line.isEmpty()) {
            System.out.print("  (required) " + prompt);
            line = kbd.nextLine().trim();
        }
        return line;
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String raw = kbd.nextLine().trim();
            try {
                int val = Integer.parseInt(raw);
                if (val >= min && val <= max) return val;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input — please enter a whole number.");
            }
        }
    }
}
