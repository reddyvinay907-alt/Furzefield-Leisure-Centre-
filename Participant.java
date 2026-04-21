package leisure.core;

/**
 * A registered participant of the leisure centre.
 *
 * Intentionally lightweight: identity only.
 * All booking state is held in {@link BookingRecord}, never here.
 */
public class Participant {

    private final String participantCode;
    private final String fullName;
    private final String contactEmail;

    public Participant(String participantCode, String fullName, String contactEmail) {
        this.participantCode = participantCode;
        this.fullName        = fullName;
        this.contactEmail    = contactEmail;
    }

    public String getParticipantCode() { return participantCode; }
    public String getFullName()        { return fullName;        }
    public String getContactEmail()    { return contactEmail;    }

    @Override
    public String toString() {
        return String.format("[%s]  %-26s  %s", participantCode, fullName, contactEmail);
    }
}
