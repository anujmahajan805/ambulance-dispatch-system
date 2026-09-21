package ambulance;

import ambulance.map.CityMap;
import ambulance.model.*;
import ambulance.report.DispatchReport;
import ambulance.service.Dispatch;
import ambulance.service.Dispatcher;

import java.util.List;

/** Demo application for the Emergency Ambulance Dispatch System. */
public class Main {
    public static void main(String[] args) {
        CityMap city = buildCity();
        Dispatcher dispatcher = new Dispatcher(city);
        DispatchReport report = new DispatchReport();

        dispatcher.addAmbulance(new Ambulance("AMB-1", "Central Station"));
        dispatcher.addAmbulance(new Ambulance("AMB-2", "North Market"));
        dispatcher.addAmbulance(new Ambulance("AMB-3", "Airport Road"));

        dispatcher.addHospital(new Hospital("City General", "Central Station", 2));
        dispatcher.addHospital(new Hospital("Riverside Medical", "Riverside", 1));
        dispatcher.addHospital(new Hospital("University Hospital", "University", 3));

        List<EmergencyCall> calls = List.of(
            new EmergencyCall("Old Town", Severity.LOW, "Sprained ankle"),
            new EmergencyCall("Riverside", Severity.CRITICAL, "Cardiac arrest"),
            new EmergencyCall("Airport Road", Severity.HIGH, "Road accident"),
            new EmergencyCall("University", Severity.MEDIUM, "Allergic reaction")
        );

        System.out.println("========================================");
        System.out.println("   EMERGENCY AMBULANCE DISPATCH SYSTEM");
        System.out.println("========================================");
        System.out.println("\nCalls received:");
        for (EmergencyCall call : calls) {
            dispatcher.reportEmergency(call);
            System.out.println("  + " + call);
        }

        System.out.println("\nDispatching by priority...");
        dispatchAvailable(dispatcher, report);

        System.out.println(report.summary(dispatcher.getPendingCount()));
        System.out.println("Available ambulances: " + dispatcher.getAvailableAmbulanceCount());

        // Simulate completion of one ambulance so a waiting call can be served.
        for (Ambulance a : dispatcher.getAmbulances()) {
            if (a.getStatus() == AmbulanceStatus.EN_ROUTE) {
                System.out.println("Completing " + a.getId() + "...\n");
                dispatcher.completeDispatch(a.getId());
                break;
            }
        }

        dispatchAvailable(dispatcher, report);
        System.out.println(report.summary(dispatcher.getPendingCount()));
        System.out.println("Hospital status:");
        dispatcher.getHospitals().forEach(h -> System.out.println("  " + h));
    }

    private static void dispatchAvailable(Dispatcher dispatcher, DispatchReport report) {
        for (Dispatch d : dispatcher.dispatchAll()) {
            System.out.println("\n" + d);
            report.record(d);
        }
    }

    private static CityMap buildCity() {
        CityMap city = new CityMap();
        city.addRoad("Central Station", "North Market", 6);
        city.addRoad("Central Station", "Old Town", 4);
        city.addRoad("Central Station", "University", 8);
        city.addRoad("North Market", "Airport Road", 10);
        city.addRoad("North Market", "Riverside", 7);
        city.addRoad("Old Town", "Riverside", 5);
        city.addRoad("Old Town", "University", 6);
        city.addRoad("Riverside", "Airport Road", 9);
        city.addRoad("University", "Airport Road", 12);
        return city;
    }
}
