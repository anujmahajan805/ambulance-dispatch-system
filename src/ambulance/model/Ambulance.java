package ambulance.model;

public class Ambulance {
    private final String id;
    private String currentNode;
    private AmbulanceStatus status = AmbulanceStatus.AVAILABLE;

    public Ambulance(String id, String startNode) {
        this.id = id;
        this.currentNode = startNode;
    }

    public String getId() {
        return id;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public AmbulanceStatus getStatus() {
        return status;
    }

    public void setStatus(AmbulanceStatus status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return status == AmbulanceStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return id + " @" + currentNode + " [" + status + "]";
    }
}
