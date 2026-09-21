package ambulance.model;

public class Hospital {
    private final String name;
    private final String node;
    private int availableBeds;

    public Hospital(String name, String node, int availableBeds) {
        this.name = name;
        this.node = node;
        this.availableBeds = availableBeds;
    }

    public String getName() {
        return name;
    }

    public String getNode() {
        return node;
    }

    public int getAvailableBeds() {
        return availableBeds;
    }

    public boolean hasFreeBed() {
        return availableBeds > 0;
    }

    /** Reserves one bed for an incoming patient. */
    public void reserveBed() {
        if (availableBeds <= 0) {
            throw new IllegalStateException(name + " has no free beds");
        }
        availableBeds--;
    }

    public void releaseBed() {
        availableBeds++;
    }

    @Override
    public String toString() {
        return name + " (" + availableBeds + " beds free)";
    }
}
