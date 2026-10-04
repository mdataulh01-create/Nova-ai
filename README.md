# Nova AI — Full-Stack Native Android IDE & AI Coding Agent

Nova AI is a native Android IDE and multi-agent development environment built with Kotlin, Jetpack Compose, and Material Design 3.

## Key Features

1. **AI Chat & Model Hub**:
   - Multiple providers: Google Gemini, OpenAI, Anthropic, DeepSeek, and custom OpenAI-compatible proxies.
   - Per-conversation model selection (`gemini-3.5-flash`, `gemini-3.1-pro-preview`, `gpt-4o`, `claude-3-7-sonnet`, `deepseek-coder`).
   - Project context injection and Room database offline caching.

2. **Project Workspace & Explorer**:
   - Central storage in `Internal storage/NovaAI/` with dual-mode Scoped Storage (SAF DocumentTree + App-Private sandbox fallback).
   - Real file tree navigation with file type icons, sort orders, and hidden files toggle.
   - Full file operations: create, rename, duplicate, move, delete, and inspect file metadata.
   - In-project search across file names and line contents.
   - Backup creation, ZIP export, and path-traversal-safe ZIP extraction.

3. **Built-in Code Editor**:
   - Multi-tab file editing with unsaved change tracking and auto-saving.
   - Syntax highlighting for Kotlin, Java, Python, JavaScript, HTML, CSS, JSON, Markdown, and Shell scripts.
   - Search & replace with live query replacement.
   - Go to line number navigation and Save As functionality.
   - Responsive line number gutter and bottom file status bar.

4. **In-App Terminal**:
   - Real sandboxed command execution via Android JVM `ProcessBuilder` in project directory.
   - Interactive stdin/stdout/stderr streaming with exit codes and timings.
   - Command safety classification (`SAFE`, `REQUIRES_APPROVAL`, `BLOCKED`).

5. **Multi-Agent Coding Workflow**:
   - Specialized agent roles: Coordinator, Architect, Developer, Code Reviewer, Debugger, Tester, and Documentation.
   - Step-by-step planning and file change preview with color-coded diffs.
   - User approval gates before writing or modifying files.

6. **Git Version Control**:
   - Local Git repository status tracking.
   - File staging and unstaging checklist.
   - Commit creation with SHA-1 hashes and commit history log.
   - Branch creation and switching.

7. **Security & Privacy**:
   - Android Keystore AES-GCM credential encryption.
   - Automatic API key redaction in logs and exports.
   - Complies strictly with Android Scoped Storage and Google Play policies.
