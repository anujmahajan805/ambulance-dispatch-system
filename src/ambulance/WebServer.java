package ambulance;

import ambulance.map.CityMap;
import ambulance.model.Ambulance;
import ambulance.model.AmbulanceStatus;
import ambulance.model.EmergencyCall;
import ambulance.model.Hospital;
import ambulance.model.Severity;
import ambulance.report.DispatchReport;
import ambulance.service.Dispatch;
import ambulance.service.Dispatcher;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lightweight web server for the Emergency Ambulance Dispatch System.
 * Uses the existing DSA/dispatch classes; no external libraries are required.
 */
public class WebServer {
    private static final int DEFAULT_PORT = 8080;
    private static final Path WEB_ROOT = Path.of("web").toAbsolutePath().normalize();

    private static final List<String> LOCATIONS = List.of(
            "Central Station", "North Market", "Old Town", "University",
            "Airport Road", "Riverside"
    );

    private final Dispatcher dispatcher;
    private final DispatchReport report = new DispatchReport();
    private final List<Dispatch> recentDispatches = new ArrayList<>();
    private HttpServer server;

    public WebServer() {
        CityMap city = buildCity();
        dispatcher = new Dispatcher(city);

        dispatcher.addAmbulance(new Ambulance("AMB-1", "Central Station"));
        dispatcher.addAmbulance(new Ambulance("AMB-2", "North Market"));
        dispatcher.addAmbulance(new Ambulance("AMB-3", "Airport Road"));

        dispatcher.addHospital(new Hospital("City General", "Central Station", 2));
        dispatcher.addHospital(new Hospital("Riverside Medical", "Riverside", 1));
        dispatcher.addHospital(new Hospital("University Hospital", "University", 3));
    }

    public void start(int port) throws IOException {
        // Bind to all network interfaces so the site can be accessed
        // from another PC and by cloud hosting services such as Render.
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/api/state", this::state);
        server.createContext("/api/emergency", this::emergency);
        server.createContext("/api/dispatch", this::dispatch);
        server.createContext("/api/complete", this::complete);
        server.createContext("/api/demo", this::demo);
        server.createContext("/", this::staticFile);
        server.setExecutor(null);
        server.start();

        System.out.println("========================================");
        System.out.println("  EMERGENCY AMBULANCE WEB SERVER");
        System.out.println("========================================");
        System.out.println("Website: http://localhost:" + port);
        System.out.println("Network access: http://<YOUR-PC-IP>:" + port);
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private synchronized void state(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "GET")) return;
        sendJson(exchange, 200, buildStateJson());
    }

    private synchronized void emergency(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) return;
        try {
            String body = readBody(exchange);
            String location = jsonValue(body, "location");
            String severityText = jsonValue(body, "severity");
            String description = jsonValue(body, "description");

            if (!LOCATIONS.contains(location)) {
                sendJson(exchange, 400, error("Unknown location"));
                return;
            }
            if (severityText == null || description == null || description.isBlank()) {
                sendJson(exchange, 400, error("Severity and description are required"));
                return;
            }

            Severity severity = Severity.valueOf(severityText.toUpperCase());
            dispatcher.reportEmergency(new EmergencyCall(location, severity, description.trim()));
            sendJson(exchange, 200, buildStateJson());
        } catch (Exception e) {
            sendJson(exchange, 400, error(e.getMessage()));
        }
    }

    private synchronized void dispatch(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) return;
        try {
            List<Dispatch> made = dispatcher.dispatchAll();
            for (Dispatch d : made) {
                report.record(d);
                recentDispatches.add(0, d);
            }
            sendJson(exchange, 200, buildStateJson());
        } catch (Exception e) {
            sendJson(exchange, 400, error(e.getMessage()));
        }
    }

    private synchronized void complete(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) return;
        try {
            String body = readBody(exchange);
            String ambulanceId = jsonValue(body, "ambulanceId");
            if (ambulanceId == null || ambulanceId.isBlank()) {
                sendJson(exchange, 400, error("Ambulance ID is required"));
                return;
            }
            dispatcher.completeDispatch(ambulanceId);
            sendJson(exchange, 200, buildStateJson());
        } catch (Exception e) {
            sendJson(exchange, 400, error(e.getMessage()));
        }
    }

    private synchronized void demo(HttpExchange exchange) throws IOException {
        if (!allow(exchange, "POST")) return;
        // Add the same four sample calls used by Main.java, then dispatch them.
        dispatcher.reportEmergency(new EmergencyCall("Old Town", Severity.LOW, "Sprained ankle"));
        dispatcher.reportEmergency(new EmergencyCall("Riverside", Severity.CRITICAL, "Cardiac arrest"));
        dispatcher.reportEmergency(new EmergencyCall("Airport Road", Severity.HIGH, "Road accident"));
        dispatcher.reportEmergency(new EmergencyCall("University", Severity.MEDIUM, "Allergic reaction"));
        List<Dispatch> made = dispatcher.dispatchAll();
        for (Dispatch d : made) {
            report.record(d);
            recentDispatches.add(0, d);
        }
        sendJson(exchange, 200, buildStateJson());
    }

    private void staticFile(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, error("Method not allowed"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path requested = WEB_ROOT.resolve(path.substring(1)).normalize();
        if (!requested.startsWith(WEB_ROOT) || !Files.exists(requested) || Files.isDirectory(requested)) {
            sendText(exchange, 404, "404 - Page not found", "text/plain; charset=UTF-8");
            return;
        }

        String contentType = switch (getExtension(requested.getFileName().toString())) {
            case "html" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript; charset=UTF-8";
            default -> "application/octet-stream";
        };
        byte[] bytes = Files.readAllBytes(requested);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private String buildStateJson() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"locations\":[");
        for (int i = 0; i < LOCATIONS.size(); i++) {
            if (i > 0) json.append(',');
            json.append(quote(LOCATIONS.get(i)));
        }
        json.append("],\"pendingCalls\":").append(dispatcher.getPendingCount());
        json.append(",\"availableAmbulances\":").append(dispatcher.getAvailableAmbulanceCount());
        json.append(",\"ambulances\":[");
        for (int i = 0; i < dispatcher.getAmbulances().size(); i++) {
            if (i > 0) json.append(',');
            Ambulance a = dispatcher.getAmbulances().get(i);
            json.append("{\"id\":").append(quote(a.getId()))
                    .append(",\"location\":").append(quote(a.getCurrentNode()))
                    .append(",\"status\":").append(quote(a.getStatus().name()))
                    .append('}');
        }
        json.append("],\"hospitals\":[");
        for (int i = 0; i < dispatcher.getHospitals().size(); i++) {
            if (i > 0) json.append(',');
            Hospital h = dispatcher.getHospitals().get(i);
            json.append("{\"name\":").append(quote(h.getName()))
                    .append(",\"location\":").append(quote(h.getNode()))
                    .append(",\"beds\":").append(h.getAvailableBeds())
                    .append('}');
        }
        json.append("],\"report\":{")
                .append("\"completed\":").append(report.getCompleted())
                .append(",\"pending\":").append(dispatcher.getPendingCount())
                .append(",\"avgResponse\":").append(formatNumber(report.getAverageResponseMinutes()))
                .append(",\"avgTransport\":").append(formatNumber(report.getAverageTransportMinutes()))
                .append("},\"dispatches\":[");
        int count = Math.min(recentDispatches.size(), 12);
        for (int i = 0; i < count; i++) {
            if (i > 0) json.append(',');
            Dispatch d = recentDispatches.get(i);
            json.append("{\"callId\":").append(d.call().getId())
                    .append(",\"severity\":").append(quote(d.call().getSeverity().name()))
                    .append(",\"description\":").append(quote(d.call().getDescription()))
                    .append(",\"patientLocation\":").append(quote(d.call().getLocationNode()))
                    .append(",\"ambulance\":").append(quote(d.ambulance().getId()))
                    .append(",\"hospital\":").append(quote(d.hospital().getName()))
                    .append(",\"toPatient\":").append(quote(String.join(" → ", d.routeToPatient())))
                    .append(",\"toHospital\":").append(quote(String.join(" → ", d.routeToHospital())))
                    .append(",\"response\":").append(formatNumber(d.minutesToPatient()))
                    .append(",\"transport\":").append(formatNumber(d.minutesToHospital()))
                    .append(",\"total\":").append(formatNumber(d.totalMinutes()))
                    .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private static String jsonValue(String json, String key) {
        String needle = "\"" + key + "\"";
        int start = json.indexOf(needle);
        if (start < 0) return null;
        int colon = json.indexOf(':', start + needle.length());
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(switch (c) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> c;
                });
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return null;
    }

    private static String quote(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private static String formatNumber(double value) {
        return String.format(java.util.Locale.US, "%.1f", value);
    }

    private static String error(String message) {
        return "{\"error\":" + quote(message == null ? "Unknown error" : message) + "}";
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static boolean allow(HttpExchange exchange, String method) throws IOException {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, error("Method not allowed"));
            return false;
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        return true;
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendText(exchange, status, json, "application/json; charset=UTF-8");
    }

    private static void sendText(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
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

    public static void main(String[] args) throws Exception {
        // Cloud hosts provide the port through the PORT environment variable.
        // Command-line argument still works for local/manual testing.
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            String envPort = System.getenv("PORT");
            port = (envPort == null || envPort.isBlank())
                    ? DEFAULT_PORT
                    : Integer.parseInt(envPort);
        }

        new WebServer().start(port);
    }
}
