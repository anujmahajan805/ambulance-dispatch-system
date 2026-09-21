package ambulance.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Distances and routes from one source node to every reachable node. */
public class ShortestPaths {
    private final String source;
    private final Map<String, Double> distance;
    private final Map<String, String> previous;

    ShortestPaths(String source, Map<String, Double> distance, Map<String, String> previous) {
        this.source = source;
        this.distance = distance;
        this.previous = previous;
    }

    public String getSource() {
        return source;
    }

    public boolean isReachable(String node) {
        return distance.containsKey(node);
    }

    /** Travel time in minutes, or Double.POSITIVE_INFINITY if unreachable. */
    public double distanceTo(String node) {
        return distance.getOrDefault(node, Double.POSITIVE_INFINITY);
    }

    /** Ordered list of nodes from the source to the target, empty if unreachable. */
    public List<String> pathTo(String target) {
        if (!isReachable(target)) {
            return List.of();
        }
        List<String> path = new ArrayList<>();
        for (String at = target; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }
}
