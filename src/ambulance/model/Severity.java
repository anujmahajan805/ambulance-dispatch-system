package ambulance.model;

/** How urgent an emergency is. A higher priority value is served first. */
public enum Severity {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int priority;

    Severity(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
