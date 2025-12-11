# Info Tab Feature Planning

## Round 1: Scoping & Requirements

### 1. Data Source & Depth

**Q:** Should we use `docker inspect` vs `docker ps`?
**User:** "Explain the difference?"
**Analysis & Recommendation:**

- **`docker ps`**: Provides a _summary_. Good for lists. Contains: ID, Image, Command, Status, Ports, Names. content is minimal.
- **`docker inspect`**: Provides _everything_. It returns a large JSON object with: - Full Environment Variables (`KEY=VALUE`) - Exact Volume Mounts (Host Path -> Container Path) - Full Network Settings (IP Address, Gateway, MacAddress) - State Details (Pid, StartedAt, FinishedAt, ExitCode) - Restart Policy, Entrypoint, Cmd
  **Conclusion:** We **must** use `docker inspect`. `docker ps` is insufficient for a "Details" tab. We will implement `DockerService.inspectContainer(id)`.

### 2. Presentation Format

**Q:** keys-value vs JSON vs Hybrid?
**User:** "hybrid."
**Recommendation:**
We will implement a **Hybrid View**:

- **Tab 1: "Overview" (User Friendly)**
  - Grouped interactions (Network, Volume, Config).
  - Readable lists for Env Vars (Key | Value).
  - Visual indicators for Status.
- **Tab 2: "Raw" (Developer Power)**
  - A selectable, scrollable Text/Code view displaying the full formatted JSON from `docker inspect`.
  - This satisfies the "verify exactly what Docker sees" use case.

### 3. Interactivity

**User:** "Definitely this need to have copy capabilities."
**Plan:**

- Every field in the "Overview" tab will be click-to-copy or have a small "copy" icon button next to it.
- The "Raw" view will have a "Copy All" button.

### 4. Updates

**User:** "Add the refresh button."
**Plan:**

- We will add a Refresh Icon Button in the top bar of the Info/Terminal content area (near the "Disconnect" terminal button location, or in a shared toolbar).

### 5. Highlights

**User:** "Any recommendations?"
**Recommendation:**
The top "Hero" section of the Info tab should show:

- **Status Icon/Text** (Green/Red)
- **Image Name** (Clickable/Copyable)
- **Container Name**
- **IP Address** (if applicable)
- **Primary Port** maps (e.g., `0.0.0.0:8080 -> 80/tcp`)

---

## Final Agreed Specification

### 1. Data Model

- We will add `DockerService.inspectContainer(id)` which parses `docker inspect`.
- We will create a `ContainerInspection` data class to map the detailed JSON structure, keeping it separate from the lightweight `Container` list model.

### 2. UI Layout

- **Structure**: The Info tab will show the "Overview" (Formatted view) by default.
- **Raw JSON**: A "View Raw" toggle button will switch the content to the raw JSON text.
- **Components**:
  - **Hero Section**: Status, Name, Image, IP, Ports.
  - **Environment Variables**: Displayed as a **Table** (Key | Value).
  - **Volume Mounts**: List of mounts, explicitly labeling "Bind" vs "Volume" types.
  - **Network**: Network names, IP addresses, Gateways.

### 3. Interactivity

- **Copy**: All field values in the Overview will be copyable. The Raw view will have "Copy All".
- **Refresh**: A Refresh button will be added to the toolbar to re-fetch data.

### 4. Technical Details

- **Icons**: Use standard `androidx.compose.material.icons` (Storage, Hub, dns, etc.).
- **State**: `ContainerDetailsScreen` will load data asynchronously on `init` and on `refresh` click.
