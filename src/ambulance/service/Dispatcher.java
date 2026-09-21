package ambulance.service;

import ambulance.map.CityMap;
import ambulance.map.ShortestPaths;
import ambulance.model.Ambulance;
import ambulance.model.AmbulanceStatus;
import ambulance.model.EmergencyCall;
import ambulance.model.Hospital;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Receives emergency calls, keeps them in a priority queue (most severe first),
 * and assigns the nearest free ambulance and the nearest hospital with a free bed.
 */
public class Dispatcher {
    private final CityMap map;
    private final List<Ambulance> ambulances = new ArrayList<>();
    private final List<Hospital> hospitals = new ArrayList<>();
    private final PriorityQueue<EmergencyCall> pendingCalls = new PriorityQueue<>();
    private final Map<String, Dispatch> activeDispatches = new HashMap<>();

    public Dispatcher(CityMap map) {
        this.map = map;
    }

    public void addAmbulance(Ambulance ambulance) {
        requireLocation(ambulance.getCurrentNode());
        ambulances.add(ambulance);
    }

    public void addHospital(Hospital hospital) {
        requireLocation(hospital.getNode());
        hospitals.add(hospital);
    }

    public void reportEmergency(EmergencyCall call) {
        requireLocation(call.getLocationNode());
        pendingCalls.add(call);
    }

    public int getPendingCount() {
        return pendingCalls.size();
    }

    public int getAvailableAmbulanceCount() {
        int count = 0;
        for (Ambulance a : ambulances) if (a.isAvailable()) count++;
        return count;
    }

    public List<Ambulance> getAmbulances() {
        return Collections.unmodifiableList(ambulances);
    }

    public List<Hospital> getHospitals() {
        return Collections.unmodifiableList(hospitals);
    }

    /**
     * Tries to dispatch the most urgent pending call. If no ambulance is free or
     * no hospital has a bed, the call stays in the queue and the result is empty.
     */
    public Optional<Dispatch> dispatchNext() {
        EmergencyCall call = pendingCalls.peek();
        if (call == null) {
            return Optional.empty();
        }

        // One Dijkstra run from the patient gives distances to every ambulance and hospital,
        // because roads are two-way and travel times are symmetric.
        ShortestPaths fromPatient = map.shortestPathsFrom(call.getLocationNode());

        Ambulance ambulance = findNearestAmbulance(fromPatient);
        Hospital hospital = findNearestHospital(fromPatient);
        if (ambulance == null || hospital == null) {
            return Optional.empty();
        }

        pendingCalls.poll();
        ambulance.setStatus(AmbulanceStatus.EN_ROUTE);
        hospital.reserveBed();

        // Route for the ambulance to reach the patient (reverse of patient -> ambulance path).
        List<String> toPatient = new ArrayList<>(fromPatient.pathTo(ambulance.getCurrentNode()));
        java.util.Collections.reverse(toPatient);

        Dispatch dispatch = new Dispatch(
                call,
                ambulance,
                hospital,
                toPatient,
                fromPatient.distanceTo(ambulance.getCurrentNode()),
                fromPatient.pathTo(hospital.getNode()),
                fromPatient.distanceTo(hospital.getNode()));

        activeDispatches.put(ambulance.getId(), dispatch);
        return Optional.of(dispatch);
    }

    /** Dispatches calls in priority order until the queue is empty or resources run out. */
    public List<Dispatch> dispatchAll() {
        List<Dispatch> made = new ArrayList<>();
        Optional<Dispatch> next;
        while ((next = dispatchNext()).isPresent()) {
            made.add(next.get());
        }
        return made;
    }

    /** Marks an ambulance as having delivered its patient; it becomes free at the hospital. */
    public void completeDispatch(String ambulanceId) {
        Dispatch dispatch = activeDispatches.remove(ambulanceId);
        if (dispatch == null) {
            throw new IllegalArgumentException("No active dispatch for " + ambulanceId);
        }
        Ambulance ambulance = dispatch.ambulance();
        ambulance.setCurrentNode(dispatch.hospital().getNode());
        ambulance.setStatus(AmbulanceStatus.AVAILABLE);
    }

    private Ambulance findNearestAmbulance(ShortestPaths fromPatient) {
        Ambulance best = null;
        double bestTime = Double.POSITIVE_INFINITY;
        for (Ambulance a : ambulances) {
            if (!a.isAvailable()) {
                continue;
            }
            double time = fromPatient.distanceTo(a.getCurrentNode());
            if (time < bestTime) {
                bestTime = time;
                best = a;
            }
        }
        return best;
    }

    private Hospital findNearestHospital(ShortestPaths fromPatient) {
        Hospital best = null;
        double bestTime = Double.POSITIVE_INFINITY;
        for (Hospital h : hospitals) {
            if (!h.hasFreeBed()) {
                continue;
            }
            double time = fromPatient.distanceTo(h.getNode());
            if (time < bestTime) {
                bestTime = time;
                best = h;
            }
        }
        return best;
    }

    private void requireLocation(String node) {
        if (!map.hasLocation(node)) {
            throw new IllegalArgumentException("Unknown location: " + node);
        }
    }
}
