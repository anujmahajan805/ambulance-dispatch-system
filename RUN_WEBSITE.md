# Run the Ambulance Dispatch Website in VS Code

## 1. Open the project
Open the `ambulance-dispatch` folder in VS Code (the folder containing `src`, `web`, and `.vscode`).

## 2. Make sure Java is installed
Use JDK 17 or newer and the VS Code Extension Pack for Java.

## 3. Compile the project
Open **Terminal → New Terminal** and make sure the prompt ends with:

```text
ambulance-dispatch>
```

Run:

```powershell
javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
```

No output means compilation succeeded.

## 4. Start the website
Run:

```powershell
java -cp bin ambulance.WebServer
```

You should see:

```text
EMERGENCY AMBULANCE WEB SERVER
Website: http://localhost:8080
```

## 5. Open the website
Open Chrome or Edge and go to:

```text
http://localhost:8080
```

## 6. Run directly from VS Code
Open **Run and Debug** on the left and choose:

**Run Ambulance Website**

Then open `http://localhost:8080` in the browser.

## Website features
- Emergency call form
- Critical/High/Medium/Low priority
- Priority Queue dispatching
- Nearest ambulance selection
- Nearest hospital with a free bed
- Dijkstra shortest-route information
- Ambulance status dashboard
- Hospital bed dashboard
- Dispatch report and average response/transport time
- Demo scenario using the same four calls as `Main.java`
- Complete an active ambulance and make it available again

The website is a front end for the existing Java DSA dispatch engine; the original `Main.java` console program is preserved.

## If port 8080 is already in use
Run the server on another port, for example:

```powershell
java -cp bin ambulance.WebServer 8090
```

Then open:

```text
http://localhost:8090
```
