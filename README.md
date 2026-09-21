# Emergency Ambulance Dispatch System

A Java + DSA simulation of an emergency ambulance dispatch centre.

## Improved features
- Emergency calls are handled with a **Priority Queue**: CRITICAL > HIGH > MEDIUM > LOW.
- City roads are represented as a **weighted undirected graph**.
- **Dijkstra's algorithm** finds shortest travel times and routes.
- The nearest available ambulance is selected.
- The nearest hospital with a free bed is selected.
- Calls remain queued when resources are unavailable.
- Ambulance status is tracked: AVAILABLE, EN_ROUTE, BUSY.
- Hospital bed availability is tracked and displayed.
- A **Dispatch Report** calculates completed calls and average response/transport time.
- Input validation prevents invalid locations, road weights, and bed counts.
- Clean package structure separates model, map/algorithm, service, and reporting layers.

## DSA used
| Problem | Data structure / algorithm |
|---|---|
| Emergency priority | Priority Queue |
| City representation | Weighted Graph + Adjacency List |
| Shortest route | Dijkstra's Algorithm |
| Active dispatch lookup | HashMap |
| Entity collections | ArrayList |

## Complexity
Dijkstra with a priority queue runs in **O((V + E) log V)** for a graph with V vertices and E edges.

## Run in VS Code
1. Install JDK 17+ and the VS Code Extension Pack for Java.
2. Open this folder in VS Code.
3. Run `src/ambulance/Main.java`.

## Run from terminal
```bash
javac -d bin $(find src -name "*.java")
java -cp bin ambulance.Main
```

### Windows PowerShell
```powershell
Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName } > files.txt
javac -d bin @files.txt
java -cp bin ambulance.Main
```

## Web dashboard

The project now includes a browser-based dashboard in `web/` backed by the same Java DSA engine. It uses Java's built-in `HttpServer`, so no external framework or dependency is required.

Run the website from the project root:

```powershell
javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin ambulance.WebServer
```

Then open `http://localhost:8080` in Chrome or Edge. See `RUN_WEBSITE.md` for the complete steps.

## Suggested future upgrades
- JavaFX dashboard
- MySQL/JDBC persistence
- Live GPS/traffic data
- Login and role-based access
- JUnit test suite
- Response-time charts and CSV export
