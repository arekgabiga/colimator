# Product Requirements Document (PRD) - Colimator

## 1. Executive Summary

**Colimator** is a desktop GUI application designed to act as a lightweight, high-performance alternative to Docker Desktop for macOS users who utilize **Colima** as their container runtime. The primary goal is to provide a graphical interface for managing the Colima Virtual Machine and Docker containers without the heavy resource footprint of Electron-based apps.

## 2. Problem Statement

Developers using Colima currently rely heavily on the CLI (`colima start`, `docker ps`, etc.). While efficient for power users, it lacks the convenience of a dashboard for quick status checks, visual container management, and one-click start/stop functioning resident in the system tray. Existing GUI solutions (Docker Desktop) are resource-intensive and often require paid licenses for commercial use.

## 3. Technology Stack

- **Core Platform**: JVM (Kotlin Multiplatform targeting Desktop)
- **Language**: Kotlin 2.2.21 + JDK 25
- **UI Framework**: Compose Multiplatform (Material Design 3)
- **Build Tool**: Gradle 9.x (Kotlin DSL)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Integration**: Direct CLI execution via `java.lang.ProcessBuilder` (Colima & Docker CLI)

## 4. Key Features (MVP)

### 4.1. VM Management

- **Status Indicators**: Real-time display of Colima VM status (Running, Stopped, Starting, Error).
- **Control**: Start, Stop, and Restart functionality.
- **Configuration**: Basic editor for Colima config (CPU, RAM, Disk Size) - _Planned Future MVP Item_.

### 4.2. Container Management

- **Listing**: View active and stopped containers (parsed from `docker ps --format json`).
- **Actions**: Start, Stop, Delete (Kill) containers.
- **Inspection**:
  - Basic details (Ports, Image, ID).
  - **Logs**: Copy-pasteable command hints to view logs in terminal.
  - **Exec**: Copy-pasteable command hints to enter container shell.

### 4.3. System Integration

- **Tray Icon**:
  - Persistent icon in the macOS menu bar.
  - Left-click: Toggle main window / dashboard.
  - Right-click: Quick actions context menu (Start/Stop Colima, Quit App).
- **Onboarding**:
  - Strict Dependency Check: Verify `colima` and `docker` are installed on launch.
  - Alert user and exit if dependencies are missing.

## 5. User Interface (UI/UX)

- **Design System**: Material Design 3 (Google).
- **Navigation**: Vertical Sidebar (Rail) for switching views (Dashboard, Containers, Images, Volumes).
- **Theme**: Dark Mode optimized for developers.
- **Performance**: Native JVM startup speed, low RAM overhead compared to web-based GUIs.

## 6. Future Scope (Post-MVP)

- Image and Volume Management tabs.
- Integrated Terminal emulator for direct `docker exec` within the app.
- Real-time log streaming in a separate window.
- Support for multiple Colima profiles.
