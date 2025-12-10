# Feature Planning: Embedded Terminal for Containers

## Goal

Allow users to open an interactive shell session inside a specific Docker container directly within the Colimator application.

## Core Technology

- **UI Widget**: [JediTerm](https://github.com/JetBrains/jediterm) (Swing-based terminal widget from IntelliJ Platform).
- **Backend**: `pty4j` (required for Pseudo-Terminal support).
- **Integration**: `SwingPanel` (Compose Multiplatform interoperability).

## Proposed Architecture

1.  **Container Execution**: Use `docker exec -it <container_id> <shell>` via `pty4j`.
    - _Shell_: Default behavior (try bash, fallback to sh).
    - _User_: Default container user (common practice).
2.  **UI Structure**:
    - **New Screen**: `ContainerDetailsScreen`. Accessed by clicking a container row.
    - **Navigation**: Refactor `Screen` enum to `sealed class` to pass `containerId`.
    - **Layout**: Tabs ("Info", "Terminal").
3.  **Persistence**:
    - Session closes when navigating away.
    - **Alert**: User is prompted to confirm if they try to leave an active session.

## Dependencies

- `com.jetbrains.jediterm:jediterm-pty`
- `org.jetbrains.pty4j:pty4j`

## Risks

- **Packaging**: Native libraries in `pty4j` need to play nice with the Mac App / DMG bundle.
