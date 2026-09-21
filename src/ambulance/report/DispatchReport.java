package ambulance.report;

import ambulance.service.Dispatch;
import java.util.List;

/** Simple operational statistics for the dispatch centre. */
public class DispatchReport {
    private int completed;
    private double totalResponseMinutes;
    private double totalTransportMinutes;

    public void record(Dispatch d) {
        completed++;
        totalResponseMinutes += d.minutesToPatient();
        totalTransportMinutes += d.minutesToHospital();
    }

    public int getCompleted() { return completed; }
    public double getAverageResponseMinutes() {
        return completed == 0 ? 0 : totalResponseMinutes / completed;
    }
    public double getAverageTransportMinutes() {
        return completed == 0 ? 0 : totalTransportMinutes / completed;
    }

    public String summary(int pending) {
        return String.format(
            "\n=== Dispatch Report ===%nCompleted calls : %d%nPending calls   : %d%nAvg response   : %.1f min%nAvg transport   : %.1f min%n",
            completed, pending, getAverageResponseMinutes(), getAverageTransportMinutes());
    }
}
