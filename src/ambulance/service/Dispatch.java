package ambulance.service;

import ambulance.model.Ambulance;
import ambulance.model.EmergencyCall;
import ambulance.model.Hospital;

import java.util.List;

/** One dispatch decision: which ambulance goes where, and to which hospital. */
public record Dispatch(
        EmergencyCall call,
        Ambulance ambulance,
        Hospital hospital,
        List<String> routeToPatient,
        double minutesToPatient,
        List<String> routeToHospital,
        double minutesToHospital) {

    public double totalMinutes() {
        return minutesToPatient + minutesToHospital;
    }

    @Override
    public String toString() {
        return String.format(
                "%s%n   -> %s | pickup in %.0f min via %s%n   -> %s | %.0f min via %s | total %.0f min",
                call,
                ambulance.getId(), minutesToPatient, String.join(" > ", routeToPatient),
                hospital.getName(), minutesToHospital, String.join(" > ", routeToHospital),
                totalMinutes());
    }
}
