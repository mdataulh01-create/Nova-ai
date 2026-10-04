package com.example.core.ai

enum class AgentRole(
    val title: String,
    val iconName: String,
    val description: String,
    val defaultModel: String,
    val systemPrompt: String
) {
    COORDINATOR(
        title = "Coordinator",
        iconName = "Hub",
        description = "Breaks down high-level tasks and assigns work across specialized agents.",
        defaultModel = "gemini-3.5-flash",
        systemPrompt = "You are the Nova AI Coordinator Agent. Decompose user requests into structured, sequential development milestones for Architect, Developer, Reviewer, Tester, and Documentation agents."
    ),
    ARCHITECT(
        title = "Architect",
        iconName = "Architecture",
        description = "Designs architecture, modules, package structures, and identifies security/performance risks.",
        defaultModel = "gemini-3.1-pro-preview",
        systemPrompt = "You are the Nova AI Architect Agent. Define clean architecture, interface boundaries, data models, and edge cases for the given mobile/full-stack project."
    ),
    DEVELOPER(
        title = "Developer",
        iconName = "Code",
        description = "Writes production-ready code, creates files, and implements features cleanly.",
        defaultModel = "gemini-3.1-pro-preview",
        systemPrompt = "You are the Nova AI Developer Agent. Implement clean, idiomatic, and robust code adhering strictly to the project's language conventions and architecture."
    ),
    CODE_REVIEWER(
        title = "Code Reviewer",
        iconName = "Grading",
        description = "Analyzes proposed changes for correctness, security vulnerabilities, and code quality.",
        defaultModel = "gemini-3.5-flash",
        systemPrompt = "You are the Nova AI Code Reviewer Agent. Scrutinize code for memory leaks, concurrency bugs, security pitfalls, and style anti-patterns. Provide actionable line-by-line feedback."
    ),
    DEBUGGER(
        title = "Debugger",
        iconName = "BugReport",
        description = "Analyzes stack traces, compiler errors, and runtime failures to isolate root causes.",
        defaultModel = "gemini-3.1-pro-preview",
        systemPrompt = "You are the Nova AI Debugger Agent. Trace exceptions, analyze stack frames, explain why the bug occurred, and prescribe surgical code fixes."
    ),
    TESTER(
        title = "Tester",
        iconName = "CheckCircle",
        description = "Generates unit test suites, edge case validations, and test coverage strategies.",
        defaultModel = "gemini-3.5-flash",
        systemPrompt = "You are the Nova AI Tester Agent. Generate comprehensive unit tests (JUnit, MockK, CoroutinesTest) covering boundary conditions, error paths, and happy paths."
    ),
    DOCUMENTATION(
        title = "Docs Agent",
        iconName = "MenuBook",
        description = "Generates technical documentation, API docs, architecture diagrams, and guides.",
        defaultModel = "gemini-3.5-flash",
        systemPrompt = "You are the Nova AI Documentation Agent. Produce concise, clear Markdown documentation, setup steps, and architectural summaries."
    )
}

data class MultiAgentLog(
    val id: String = System.currentTimeMillis().toString(),
    val role: AgentRole,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
