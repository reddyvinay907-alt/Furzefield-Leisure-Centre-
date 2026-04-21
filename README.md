# Furzefield Leisure Centre — Group Exercise Booking System

**Module:** 7COM1025 Programming for Software Engineers  
**Institution:** University of Hertfordshire  
**Version:** 4.0.0  
**Java:** 17+  
**Build Tool:** Apache Maven 3.6+

---

## Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Prerequisites](#prerequisites)
4. [Build & Run](#build--run)
5. [Running the Tests](#running-the-tests)
6. [System Architecture](#system-architecture)
7. [Class Reference](#class-reference)
8. [Sample Data](#sample-data)
9. [Menu Reference](#menu-reference)
10. [Business Rules & Constraints](#business-rules--constraints)
11. [Error Messages](#error-messages)
12. [Video Demo Guide](#video-demo-guide)

---

## Overview

A self-contained CLI application for managing group exercise lesson bookings at Furzefield Leisure Centre (FLC). Participants can browse a weekend timetable, create and manage bookings, attend sessions and leave feedback, and generate attendance and income reports.

**Key facts at a glance:**

| Item | Value |
|---|---|
| Sessions per weekend | 6 (3 Saturday + 3 Sunday) |
| Weekends in timetable | 8 |
| Total sessions | 48 |
| Max participants per session | 4 |
| Pre-loaded participants | 10 |
| Pre-loaded feedback entries | 22 |
| Exercise types | 4 |
| JUnit 5 tests | 25 |

---

## Project Structure

```
FLC2/
├── pom.xml
└── src/
    ├── main/java/leisure/
    │   ├── Launcher.java                     ← Entry point
    │   ├── core/
    │   │   ├── WeekendDay.java               ← Enum: SATURDAY | SUNDAY
    │   │   ├── SessionSlot.java              ← Enum: MORNING | AFTERNOON | EVENING
    │   │   ├── BookingState.java             ← Enum: CREATED | UPDATED | CANCELLED | ATTENDED
    │   │   ├── Participant.java              ← Participant entity
    │   │   ├── ActivitySession.java          ← Session entity (capacity, cost, comments)
    │   │   ├── BookingRecord.java            ← Booking record (state machine)
    │   │   ├── CommentEntry.java             ← Post-attendance feedback (value object)
    │   │   └── SessionCatalogue.java         ← In-memory session repository
    │   ├── engine/
    │   │   └── BookingEngine.java            ← Central Façade (all business rules)
    │   ├── console/
    │   │   └── MenuController.java           ← CLI menu interface
    │   └── bootstrap/
    │       └── DataBootstrap.java            ← Sample data loader
    └── test/java/leisure/
        └── FLCBookingSystemTest.java         ← 25 JUnit 5 tests
```

---

## Prerequisites

| Requirement | Minimum Version | Check Command |
|---|---|---|
| Java JDK | 17 | `java -version` |
| Apache Maven | 3.6 | `mvn -version` |

**Install JDK 17 (if needed):**

- **Windows / macOS:** Download from [https://adoptium.net](https://adoptium.net) and run the installer.
- **Ubuntu / Debian:** `sudo apt install openjdk-21-jdk`
- **macOS (Homebrew):** `brew install openjdk@17`

**Install Maven (if needed):**

- **Ubuntu / Debian:** `sudo apt install maven`
- **macOS (Homebrew):** `brew install maven`
- **Windows:** Download from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi), unzip, and add `bin/` to your PATH.

---

## Build & Run

```bash
# 1. Unzip the project
unzip FLC2-BookingSystem.zip
cd FLC2

# 2. Compile all sources
mvn compile

# 3. Build the executable JAR (includes all dependencies)
mvn package

# 4. Run the program
java -jar target/FLC-BookingSystem-v4.jar
```

You will see the welcome banner immediately, followed by the main menu. All 10 participants, 48 sessions, and 22 pre-loaded feedback entries are ready to use from the first run.

---

## Running the Tests

```bash
# Run all 25 JUnit 5 tests
mvn test
```

**Expected output:**

```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### What the tests cover

| # | Test Method | Behaviour Verified |
|---|---|---|
| 1 | `testCreateBookingSuccess` | Happy path — returns a CREATED record with correct participant and session |
| 2 | `testBookingIdUniqueness` | Two bookings always get different IDs |
| 3 | `testBookingIdFormat` | ID matches the `REC-XXXX` pattern |
| 4 | `testCapacityLimit` | 5th booking on a 4-spot session throws `IllegalStateException` |
| 5 | `testOpenSpotDecrement` | `openSpots()` decrements correctly with each booking |
| 6 | `testDuplicateBookingRejected` | Same participant booking same session twice is rejected |
| 7 | `testTimeConflictDetected` | Two bookings at same week/day/slot throws conflict error |
| 8 | `testNoConflictDifferentSlots` | Morning and Afternoon on the same day — no conflict |
| 9 | `testNoConflictDifferentWeeks` | Same slot in Week 1 and Week 2 — no conflict |
| 10 | `testTransferBookingSuccess` | Transfer updates session code, state → UPDATED; old released, new occupied |
| 11 | `testTransferToFullSession` | Transfer to a full session is rejected |
| 12 | `testTransferToSameSession` | Transfer to current session throws `IllegalArgumentException` |
| 13 | `testCancelBookingSuccess` | State → CANCELLED; session releases spot |
| 14 | `testDoubleCancellationRejected` | Re-cancelling a CANCELLED record throws `IllegalStateException` |
| 15 | `testSubmitAttendanceSuccess` | State → ATTENDED; attendance count = 1; mean rating = submitted value |
| 16 | `testMeanRatingCalculation` | Ratings 5, 3, 4 produce mean of 4.0 |
| 17 | `testRatingZeroRejected` | Rating 0 throws `IllegalArgumentException` |
| 18 | `testRatingSixRejected` | Rating 6 throws `IllegalArgumentException` |
| 19 | `testIncomeReportHighestEarner` | Income report flags `◉ HIGHEST EARNER` correctly |
| 20 | `testAttendanceReportContent` | Attendance report contains session code and exercise type |
| 21 | `testNonAttendedExcludedFromIncome` | Unattended bookings do not appear in income report |
| 22 | `testBrowseByDaySaturday` | `viewByDay(SATURDAY)` returns only Saturday sessions |
| 23 | `testBrowseByTypeCoreConditioning` | `viewByType("Core Conditioning")` returns only matching sessions |
| 24 | `testDataBootstrapCompleteness` | Bootstrap loads exactly 10 participants, 48 sessions, ≥ 22 comments |
| 25 | `testBootstrapParticipantCodes` | Participants P01–P10 exist; P11 does not |

---

## System Architecture

The system is organised into four packages with strict separation of concerns:

| Package | Classes | Responsibility |
|---|---|---|
| `leisure.core` | Domain entities + enums | Pure domain logic — no I/O. Entities manage their own invariants. |
| `leisure.engine` | `BookingEngine` | Façade. Sole enforcement point for all business rules. |
| `leisure.console` | `MenuController` | CLI presentation — reads input, delegates to engine, prints output. |
| `leisure.bootstrap` | `DataBootstrap` | Loads sample data at startup. No logic; no dependency on console. |

### Design Patterns Used

- **Façade** — `BookingEngine` is the single entry point for all operations. `MenuController` never touches domain objects directly.
- **Repository** — `SessionCatalogue` encapsulates multi-key session lookup (by code, day, type, slot).
- **State Machine** — `BookingRecord` uses named transition methods (`transferSession()`, `cancelRecord()`, `markAsAttended()`) instead of a public setter, preventing illegal state transitions.
- **Value Object** — `CommentEntry` is immutable after construction; rating is validated in the constructor.

---

## Class Reference

### Enums

| Enum | Values |
|---|---|
| `WeekendDay` | `SATURDAY`, `SUNDAY` |
| `SessionSlot` | `MORNING` (08:30), `AFTERNOON` (13:00), `EVENING` (18:30) |
| `BookingState` | `CREATED` → `UPDATED` / `CANCELLED` / `ATTENDED` |

### Core Entities

#### `Participant`
| Field | Type | Description |
|---|---|---|
| `participantCode` | `String` | Unique ID, e.g. `P01` |
| `fullName` | `String` | Display name |
| `contactEmail` | `String` | Email address |

#### `ActivitySession`
| Field | Type | Description |
|---|---|---|
| `sessionCode` | `String` | Unique ID, e.g. `W3SAM` |
| `exerciseType` | `String` | e.g. `Core Conditioning` |
| `day` | `WeekendDay` | Saturday or Sunday |
| `slot` | `SessionSlot` | Morning / Afternoon / Evening |
| `weekIndex` | `int` | 1–8 |
| `costPerHead` | `double` | Fixed fee per participant |
| `CAPACITY_LIMIT` | `int` | `4` (constant) |

Key methods: `hasOpenSpot()`, `occupySpot()`, `vacateSpot()`, `appendComment()`, `meanRating()`, `confirmedAttendanceCount()`

#### `BookingRecord`
| Field | Type | Description |
|---|---|---|
| `recordId` | `String` | Unique ID, format `REC-XXXX` |
| `participantCode` | `String` | Owning participant |
| `sessionCode` | `String` | Current session |
| `state` | `BookingState` | Current lifecycle state |

Key methods: `transferSession()`, `cancelRecord()`, `markAsAttended()`, `isActive()`, `isCancelled()`, `isAttended()`

#### `CommentEntry` (immutable)
| Field | Type | Description |
|---|---|---|
| `participantCode` | `String` | Submitting participant |
| `sessionCode` | `String` | Session reviewed |
| `satisfactionRating` | `int` | 1 (Strongly Dissatisfied) to 5 (Strongly Satisfied) |
| `feedbackText` | `String` | Free-text comment |

#### `BookingEngine` — key public methods

| Method | Description |
|---|---|
| `createBooking(participantCode, sessionCode)` | Creates a booking; enforces capacity, duplicate, and conflict rules |
| `transferBooking(recordId, newSessionCode)` | Moves booking to another session |
| `cancelBooking(recordId)` | Cancels booking; releases session spot |
| `submitAttendance(recordId, rating, feedbackText)` | Confirms attendance; stores comment |
| `viewByDay(WeekendDay)` | Returns all sessions on a given day |
| `viewByType(String)` | Returns all sessions of a given exercise type |
| `produceAttendanceReport()` | Formatted attendance + mean rating report |
| `produceIncomeReport()` | Formatted income report with highest earner highlighted |

---

## Sample Data

### Participants (P01–P10)

| Code | Name | Email |
|---|---|---|
| P01 | Sofia Andersen | sofia.a@leisure.co.uk |
| P02 | Marcus Obi | marcus.o@leisure.co.uk |
| P03 | Hana Suzuki | hana.s@leisure.co.uk |
| P04 | Callum Fraser | callum.f@leisure.co.uk |
| P05 | Nneka Adeyemi | nneka.a@leisure.co.uk |
| P06 | Lukas Novak | lukas.n@leisure.co.uk |
| P07 | Ines Delacroix | ines.d@leisure.co.uk |
| P08 | Rajan Krishnasamy | rajan.k@leisure.co.uk |
| P09 | Astrid Lindqvist | astrid.l@leisure.co.uk |
| P10 | Emeka Okafor | emeka.o@leisure.co.uk |

### Exercise Types & Costs

| Exercise Type | Cost per Head | Saturday Slot | Sunday Slot |
|---|---|---|---|
| Core Conditioning | £13.00 | Morning | Afternoon |
| Cardio Blast | £11.50 | Afternoon | Evening |
| Endurance Circuit | £14.00 | Evening | — |
| Flexibility Flow | £10.00 | — | Morning |

### Session Code Format

Session codes follow the pattern `W{week}{day}{slot}`:

| Code Part | Values |
|---|---|
| `W{1–8}` | Week number |
| `SA` | Saturday |
| `SU` | Sunday |
| `M` | Morning |
| `A` | Afternoon |
| `E` | Evening |

**Examples:** `W1SAM` = Week 1, Saturday Morning · `W4SUE` = Week 4, Sunday Evening

---

## Menu Reference

```
  ┌──────────────────────────────────────────────────────┐
  │                    MAIN MENU                         │
  ├──────────────────────────────────────────────────────┤
  │  1.  Browse Session Schedule                         │
  │  2.  Create New Booking                              │
  │  3.  Modify Existing Booking                         │
  │  4.  Cancel a Booking                                │
  │  5.  Record Attendance & Submit Feedback             │
  │  6.  My Booking History                              │
  │  7.  Participant Directory                           │
  │  8.  Attendance & Satisfaction Report                │
  │  9.  Income Report                                   │
  │  0.  Exit                                            │
  └──────────────────────────────────────────────────────┘
```

### Option 1 — Browse Session Schedule
Choose to filter by **day** (Saturday/Sunday) or by **exercise type**. Displays session code, exercise type, day, slot, week number, cost per head, and available spots.

### Option 2 — Create New Booking
Enter your participant code (P01–P10) and a session code. The system checks:
- Session has a free spot (max 4)
- No existing active booking for the same participant + session
- No time conflict (same week/day/slot)

On success, prints your unique booking reference (e.g. `REC-0001`).

### Option 3 — Modify Existing Booking
Lists your active bookings. Enter the booking reference and a new session code. The original reference is retained with state updated to `UPDATED`.

### Option 4 — Cancel a Booking
Lists your active bookings. Enter the booking reference to cancel. The spot is released and the reference is permanently retired.

### Option 5 — Record Attendance & Submit Feedback
Lists your active bookings. Enter the booking reference, a satisfaction rating (1–5), and a text comment. State changes to `ATTENDED`.

### Option 6 — My Booking History
Shows all booking records for a participant across all states (CREATED, UPDATED, CANCELLED, ATTENDED).

### Option 7 — Participant Directory
Lists all 10 registered participants with their codes and contact details.

### Option 8 — Attendance & Satisfaction Report
For each session with at least one confirmed attendee, shows:
- Session code, exercise type, day, slot, and week
- Number of confirmed attendees
- Average satisfaction rating out of 5

Only `ATTENDED` bookings are counted.

### Option 9 — Income Report
Shows total income per exercise type (cost per head × confirmed attendees), sorted highest to lowest. Flags the top earner with `◉ HIGHEST EARNER`.

---

## Business Rules & Constraints

| Rule | Enforcement Point |
|---|---|
| Maximum 4 participants per session | `ActivitySession.occupySpot()` + `BookingEngine.createBooking()` |
| No duplicate bookings | `BookingEngine.createBooking()` — checks for existing active record on same participant + session |
| No time conflicts | `BookingEngine.verifyNoConflict()` — matches week index + day + slot across all active records |
| Booking ID uniqueness | Sequential counter in `BookingEngine.generateRecordId()`; format `REC-XXXX` |
| Booking ID not reused | Cancelled records remain in the ledger; counter never decrements |
| Rating must be 1–5 | `CommentEntry` constructor + `BookingEngine.submitAttendance()` |
| Only ATTENDED records count in reports | `ActivitySession.confirmedAttendanceCount()` uses comment log size |
| State machine correctness | Named transition methods on `BookingRecord`; no public status setter |

---

## Error Messages

| Situation | Message |
|---|---|
| Session fully booked | `Error: Session is fully occupied — '...' has no available spots.` |
| Duplicate booking | `Error: [Name] already has an active booking for session [code].` |
| Time conflict | `Conflict detected for selected time slot — [code] already has a booking at [day] [slot] in Week [n].` |
| Transfer to full session | `Error: The target session is fully occupied. Transfer cannot proceed.` |
| Transfer to same session | `Error: The target session must differ from the current one.` |
| Invalid booking reference | `Error: Booking record '[ID]' not found.` |
| Operating on inactive record | `Error: Booking [ID] is not active — current state: [state].` |
| Invalid rating | `Error: Satisfaction rating must be 1–5. Received: [n]` |
| Unknown participant | `Error: Participant code '[code]' is not registered.` |
| Unknown session | `Error: Session code '[code]' does not exist in the catalogue.` |

---

## Video Demo Guide

Record the following sequence to demonstrate all functionalities and constraint checks:

1. **Start the program** — show the welcome banner and main menu
2. **Option 1 → by day** — select Saturday, show the timetable grid
3. **Option 1 → by type** — select Core Conditioning, show filtered results
4. **Option 2 → success** — book P01 into a session with free spots; note the `REC-XXXX` reference
5. **Option 2 → session full** — fill a session to 4, then attempt a 5th booking; show "fully occupied" error
6. **Option 2 → duplicate** — attempt to book the same participant into the same session again; show error
7. **Option 2 → time conflict** — attempt to book a participant into a session at the same slot they already hold; show "Conflict detected" error
8. **Option 6** — show the participant's booking history with CREATED status
9. **Option 3 → success** — transfer the booking to a different session; show UPDATED status
10. **Option 3 → full target** — attempt to transfer to a full session; show error
11. **Option 4 → success** — cancel a booking; confirm with option 6 that status is now CANCELLED
12. **Option 4 → already cancelled** — attempt to cancel the same reference again; show error
13. **Option 5 → success** — attend a session, enter rating 5 and a comment; show "Attendance recorded"
14. **Option 5 → invalid rating** — enter rating 6; show validation error, then re-enter a valid value
15. **Option 8** — show the attendance and satisfaction report with pre-seeded data
16. **Option 9** — show the income report with `◉ HIGHEST EARNER` flagged
17. **Option 0** — exit cleanly

---

## Notes

- **No persistent storage:** all data lives in memory for the duration of one run. On exit, nothing is saved — this is a deliberate spec requirement.
- **No real-time clock:** the system does not use the current date. A lesson is "attended" only when a participant selects option 5 in the menu.
- **No external database required:** the system is fully self-contained.
- Data generated during a session (new bookings, attendance, feedback) is immediately reflected in reports within the same run.
