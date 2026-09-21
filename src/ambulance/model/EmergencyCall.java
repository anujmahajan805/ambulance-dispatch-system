package ambulance.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An incoming emergency. Natural ordering puts the most severe call first;
 * calls of equal severity are served in the order they arrived (FIFO).
 */
public class EmergencyCall implements Comparable<EmergencyCall> {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final int id;
    private final String locationNode;
    private final Severity severity;
    private final String description;

    public EmergencyCall(String locationNode, Severity severity, String description) {
        this.id = SEQUENCE.incrementAndGet();
        this.locationNode = locationNode;
        this.severity = severity;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getLocationNode() {
        return locationNode;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(EmergencyCall other) {
        int bySeverity = Integer.compare(other.severity.getPriority(), severity.getPriority());
        return bySeverity != 0 ? bySeverity : Integer.compare(id, other.id);
    }

    @Override
    public String toString() {
        return "Call#" + id + " [" + severity + "] " + description + " @" + locationNode;
    }
}
