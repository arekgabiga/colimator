# Log Streaming Feature Plan

## Goal

Implement a real-time log streaming tab for containers, allowing users to monitor container output directly within the application.

## Requirements Checklist

### Functional

- [x] **Real-time Streaming**: Logs stream live from the docker daemon (`docker logs -f`).
- [x] **Initial History**: Load the **last 1000 lines** on initialization.
- [x] **Timestamps**: Display raw docker timestamps inline.
- [x] **Ephemeral State**: Discard logs when navigating away to save memory.
- [x] **Memory Limit**: logical buffer limited to **5,000 lines** (FIFO).
- [x] **Error Handling**: Display static error message if stream disconnects (no auto-retry loops).

### UI/UX

- [x] **Position**: new "Logs" tab located between "Info" and "Terminal".
- [x] **Visuals**: Support **ANSI color codes** for readability.
- [x] **Scrolling**:
  - **Auto-scroll**: Active by default.
  - **Pause**: Auto-scroll pauses when user manually scrolls up.
- [x] **Interactivity**:
  - **Copy**: Button to copy current logs to clipboard.
  - **Word Wrap**: Long lines wrap to fit the viewport.

## Technical Implementation Strategy

### 1. Backend: Shell Streaming

We will not use JediTerm for logs to ensure a lightweight, native "document viewer" experience.

- **Modify `ShellExecutor`**: Add `executeStream(command, args): Flow<String>` to support long-running processes.
- **Modify `DockerService`**: Add `streamLogs(containerId, profileHeader): Flow<String>`.

### 2. ViewModel Logic

- `ContainerLogsViewModel` (or existing `ContainersViewModel` extended).
- Manage a `state: List<LogLine>` (or similar efficient structure).
- Handle parsing of ANSI codes into `AnnotatedString` or `SpanStyle`.

### 3. UI Component (`ContainerLogsTab`)

- **Container**: `LazyColumn`.
- **Optimization**: Use `key` in `items` for performance.
- **Auto-scroll**: Use `LazyListState.animateScrollToItem` when new logs arrive _unless_ `isUserScrolling`.

## Why Custom Compose over JediTerm?

While `JediTerm` is powerful, a custom implementation is preferred for logs because:

1.  **Read-Only UX**: Logs are better consumed as a selectable list/document rather than a terminal emulator.
2.  **Control**: We need precise control over scroll behavior (e.g., pausing auto-scroll on user interaction) which is difficult to customize in JediTerm.
3.  **Integration**: Seamlessly integrates with Compose theming and clipboard managers without Swing interoperability layers.

## Proposed Changes

### `commonMain`

- `com.colimator.app.service.ShellExecutor`: Add `executeStream` interface.
- `com.colimator.app.service.DockerService`: Add `logs` method.
- `com.colimator.app.ui.ContainerLogsTab.kt`: **[NEW]** UI implementation.
- `com.colimator.app.util.AnsiParser.kt`: **[NEW]** Helper to convert ANSI-colored strings to `AnnotatedString`.

### `desktopMain`

- `com.colimator.app.service.JvmShellExecutor`: Implement `executeStream` using `ProcessBuilder` and coroutine `flow`.
