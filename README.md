# Colimator 🐳

**Colimator** is a lightweight, native desktop GUI for managing [Colima](https://github.com/abiosoft/colima) and Docker containers on macOS. Built with **Kotlin Multiplatform** and **Compose Desktop**, it offers a high-performance, resource-efficient alternative to resource-heavy Electron-based solutions.

![Status](https://img.shields.io/badge/Status-Active_Development-green) ![Platform](https://img.shields.io/badge/Platform-macOS-lightgrey) ![Stack](https://img.shields.io/badge/Stack-Kotlin_Multiplatform-purple)

## 🚀 Features

### Current MVP Features

- **VM Management**:
  - Real-time status tracking of the Colima Virtual Machine.
  - One-click Start, Stop, and Restart controls.
  - Profile management (switch between different Colima profiles).
- **Container Management**:
  - List active and stopped containers.
  - Start, Stop, and Delete containers.
  - View container details (Status, Ports, Image, ID).
- **Image Management**:
  - List local Docker images.
  - Sort images by name or size.
  - Delete unused images.
- **System Integration**:
  - Native macOS system tray icon (Control app visibility and quick actions).
  - Checks for `colima` and `docker` dependencies on startup.
- **User Experience**:
  - Modern Material Design 3 interface.
  - Developer-focused Dark Mode.
  - Native performance (JVM-based, low RAM overhead).

### Planned Features

- **Volume Management**: List and manage Docker volumes.
- **Terminal Integration**: Embedded terminal for direct `docker exec` sessions.
- **Log Streaming**: Real-time container log viewing window.
- **Advanced Configuration**: GUI editor for Colima settings (CPU, RAM, Disk).

## 🛠 Tech Stack

- **Language**: Kotlin 2.x
- **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Desktop)
- **Runtime**: JDK 25 (targeting JVM 21 bytecode)
- **Build System**: Gradle 9.x
- **Architecture**: MVVM (Model-View-ViewModel)

## 📋 Prerequisites

Before running Colimator, ensure you have the following installed on your macOS system:

1.  **Colima**: The container runtime.
    ```bash
    brew install colima
    ```
2.  **Docker Client**: The CLI tool to interact with the runtime.
    ```bash
    brew install docker
    ```
3.  **Java Development Kit (JDK)**: JDK 21 or higher is required to run the application.

## 🏃‍♂️ Getting Started

### Running Locally

1.  **Clone the repository**:

    ```bash
    git clone https://github.com/yourusername/colimator.git
    cd colimator
    ```

2.  **Run the application**:
    ```bash
    ./gradlew run
    ```
    _Note: The first run may take a moment to download dependencies._

### Building the Installer

To create a standalone `.dmg` installer for macOS:

```bash
./gradlew packageReleaseDmg
```

The installer will be generated in `composeApp/build/compose/binaries/main/dmg/`.

## 🏗 Project Structure

```
composeApp/src/
├── commonMain/kotlin/com/colimator/
│   ├── App.kt                    # Main application entry & navigation
│   ├── service/                  # Business logic & CLI wrappers (ColimaService, DockerService)
│   ├── ui/                       # Compose UI screens (Dashboard, Containers, Images)
│   ├── viewmodel/                # ViewModels for state management
│   └── domain/                   # Domain models
└── desktopMain/kotlin/com/colimator/
    ├── main.kt                   # Desktop entry point
    └── service/                  # Desktop-specific implementations (e.g., JvmShellExecutor)
```

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'Add some amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## 📄 License

This project is licensed under the MIT License

---

_Built with ❤️ for the developer community._
