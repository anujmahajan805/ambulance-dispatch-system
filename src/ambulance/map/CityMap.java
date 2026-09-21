package ambulance.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * The city as a weighted, undirected graph. Nodes are named places and
 * edge weights are travel times in minutes.
 */
public class CityMap {

    private record Edge(String to, double minutes) { }

    private record QueueEntry(String node, double distance) { }

    private final Map<String, List<Edge>> adjacency = new HashMap<>();

    public void addLocation(String name) {
        adjacency.computeIfAbsent(name, k -> new ArrayList<>());
    }

    public boolean hasLocation(String name) {
        return adjacency.containsKey(name);
    }

    /** Adds a two-way road between two locations. */
    public void addRoad(String a, String b, double minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Travel time must be positive");
        }
        addLocation(a);
        addLocation(b);
        adjacency.get(a).add(new Edge(b, minutes));
        adjacency.get(b).add(new Edge(a, minutes));
    }

    /**
     * Dijkstra's algorithm from a single source. Runs in O((V + E) log V)
     * using a priority queue, and gives the shortest time to every node.
     */
    public ShortestPaths shortestPathsFrom(String source) {
        if (!hasLocation(source)) {
            throw new IllegalArgumentException("Unknown location: " + source);
        }

        Map<String, Double> distance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        Set<String> settled = new HashSet<>();
        PriorityQueue<QueueEntry> queue =
                new PriorityQueue<>((x, y) -> Double.compare(x.distance(), y.distance()));

        distance.put(source, 0.0);
        queue.add(new QueueEntry(source, 0.0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (!settled.add(current.node())) {
                continue; // stale entry, node already finalised
            }
            for (Edge edge : adjacency.get(current.node())) {
                double candidate = current.distance() + edge.minutes();
                if (candidate < distance.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    distance.put(edge.to(), candidate);
                    previous.put(edge.to(), current.node());
                    queue.add(new QueueEntry(edge.to(), candidate));
                }
            }
        }
        return new ShortestPaths(source, distance, previous);
    }
}
