# Nova AI System Architecture

## Architecture Overview

Nova AI is built around a Clean Architecture / MVVM pattern optimized for mobile development tools:

```
┌─────────────────────────────────────────────────────────────┐
│                       Jetpack Compose                       │
│  Chat │ Projects │ Editor │ Terminal │ Agents │ Git │ Settings│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                    Core Business Engines                    │
│                                                             │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐  │
│  │ StorageManager  │  │ CodingAgent      │  │ ProcessMgr │  │
│  │ (SAF & Sandbox) │  │ (Tools & Diffs)  │  │ (Terminal) │  │
│  └─────────────────┘  └──────────────────┘  └────────────┘  │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐  │
│  │ KeystoreHelper  │  │ MultiAgentFlow   │  │ GitManager │  │
│  │ (AES Encryption)│  │ (Role Pipeline)  │  │ (Vcs Track)│  │
│  └─────────────────┘  └──────────────────┘  └────────────┘  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                   Data & Persistence Layer                  │
│                                                             │
│  ┌───────────────────────┐       ┌────────────────────────┐ │
│  │ Room Local Database   │       │ Firebase Auth & Cloud  │ │
│  │ (Conversations & Log) │       │ (Opt-In Sync / Backup) │ │
│  └───────────────────────┘       └────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Scoped Storage Implementation

1. **Storage Structure**:
   ```
   NovaAI/
   ├── Projects/     (User code workspaces)
   ├── Files/        (Individual files)
   ├── Downloads/    (Remote downloads)
   ├── Backups/      (Project snapshots .zip)
   ├── Exports/      (Exported bundles)
   ├── Temp/         (Temporary build files)
   ├── Terminal/     (Standalone shell workspace)
   ├── Linux/        (User-space rootfs placeholder)
   └── Config/       (Tool configurations)
   ```

2. **Access Modes**:
   - **App-Private Mode**: `context.filesDir/NovaAI/` provides instant, guaranteed read/write access without requiring user permissions. It is accessible by local process execution.
   - **SAF Mode**: When authorized via `ACTION_OPEN_DOCUMENT_TREE`, `takePersistableUriPermission` retains persistent access to an external device directory.

## Terminal Execution & Safety Engine

- Android sandboxing does not provide root or allow arbitrary package access.
- Commands execute inside the project directory via `ProcessBuilder("sh", "-c", command)`.
- Dangerous commands like `rm -rf /` or fork bombs are blocked immediately.
- Destructive commands like `rm`, `chmod`, `kill` trigger a confirmation dialog with full command visibility.

## API Key Security

- Locally configured keys are encrypted using AES-GCM via the Android Keystore.
- When generating logs, exports, or AI prompts, `KeystoreHelper.redactSecrets` scrubs key signatures (`AIza...`, `sk-...`, `Bearer [REDACTED]`).
