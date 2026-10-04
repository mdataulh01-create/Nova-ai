package com.example.core.storage

import java.io.File

object TemplateManager {

    val availableTemplates = listOf(
        "Empty Project" to "A clean slate with a starter README.md",
        "Kotlin Console" to "Kotlin application with main runner and build script",
        "Python Project" to "Python application with starter script and requirements",
        "Web HTML/JS/CSS" to "Full front-end web project with index.html and scripts",
        "Markdown Notes" to "Technical documentation, knowledge base, and note-taking",
        "Shell Utilities" to "Bash scripts and automation tooling"
    )

    fun populateProject(projectDir: File, templateName: String) {
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }

        when (templateName) {
            "Kotlin Console" -> {
                val srcDir = File(projectDir, "src").apply { mkdirs() }
                File(srcDir, "Main.kt").writeText(
                    """
                    /**
                     * Nova AI - Kotlin Console Application
                     */
                    fun main() {
                        println("======================================")
                        println("🚀 Welcome to Nova AI Mobile IDE!")
                        println("Running native Kotlin project workspace")
                        println("======================================")
                        
                        val features = listOf("AI Agent", "Terminal", "Editor", "Git")
                        features.forEachIndexed { index, feature ->
                            println("${'$'}{index + 1}. ${'$'}feature - Ready")
                        }
                    }
                    """.trimIndent()
                )
                File(projectDir, "build.gradle.kts").writeText(
                    """
                    plugins {
                        kotlin("jvm") version "2.1.0"
                    }
                    
                    repositories {
                        mavenCentral()
                    }
                    
                    dependencies {
                        implementation(kotlin("stdlib"))
                    }
                    """.trimIndent()
                )
                File(projectDir, "README.md").writeText(
                    """
                    # ${projectDir.name}
                    
                    A native Kotlin console project created in **Nova AI**.
                    
                    ### Run
                    Open the in-app Terminal and execute your scripts.
                    """.trimIndent()
                )
            }

            "Python Project" -> {
                File(projectDir, "main.py").writeText(
                    """
                    # Nova AI - Python Starter Application
                    import sys
                    import os

                    def main():
                        print("🐍 Nova AI Python Environment Initialized")
                        print(f"Python Platform: {sys.platform}")
                        print(f"Current Directory: {os.getcwd()}")
                        print("AI Coding Agent is ready to inspect and assist!")

                    if __name__ == "__main__":
                        main()
                    """.trimIndent()
                )
                File(projectDir, "requirements.txt").writeText(
                    """
                    # Dependencies for ${projectDir.name}
                    requests>=2.31.0
                    """.trimIndent()
                )
                File(projectDir, "README.md").writeText(
                    """
                    # ${projectDir.name}
                    
                    Python application created in **Nova AI**.
                    Use the Nova AI coding agent to generate functions and unit tests.
                    """.trimIndent()
                )
            }

            "Web HTML/JS/CSS" -> {
                File(projectDir, "index.html").writeText(
                    """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>${projectDir.name} - Nova AI</title>
                        <link rel="stylesheet" href="style.css">
                    </head>
                    <body>
                        <div class="card">
                            <h1>⚡ Nova AI Web Project</h1>
                            <p>Built directly on native Android device storage.</p>
                            <button id="btnAction">Click Me</button>
                            <div id="output"></div>
                        </div>
                        <script src="app.js"></script>
                    </body>
                    </html>
                    """.trimIndent()
                )
                File(projectDir, "style.css").writeText(
                    """
                    body {
                        font-family: system-ui, -apple-system, sans-serif;
                        background: #090D16;
                        color: #E2E8F0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                    }
                    .card {
                        background: #0F172A;
                        padding: 2rem;
                        border-radius: 12px;
                        border: 1px solid #334155;
                        text-align: center;
                        box-shadow: 0 8px 30px rgba(0,0,0,0.5);
                    }
                    button {
                        background: #06B6D4;
                        color: #00363F;
                        border: none;
                        padding: 0.75rem 1.5rem;
                        border-radius: 8px;
                        font-weight: bold;
                        cursor: pointer;
                        margin-top: 1rem;
                    }
                    """.trimIndent()
                )
                File(projectDir, "app.js").writeText(
                    """
                    document.getElementById('btnAction').addEventListener('click', () => {
                        const out = document.getElementById('output');
                        out.textContent = '🚀 Generated and tested in Nova AI!';
                        out.style.marginTop = '1rem';
                        out.style.color = '#22D3EE';
                    });
                    """.trimIndent()
                )
                File(projectDir, "README.md").writeText(
                    """
                    # ${projectDir.name}
                    
                    Modern Web project created inside **Nova AI**.
                    """.trimIndent()
                )
            }

            "Markdown Notes" -> {
                File(projectDir, "index.md").writeText(
                    """
                    # 📝 Project Documentation & Notes
                    
                    Welcome to your Nova AI notebook!
                    
                    ## Key Modules
                    - [Architecture](architecture.md)
                    - [Roadmap](roadmap.md)
                    
                    ## Tasks
                    - [ ] Complete Stage 1 Architecture Review
                    - [ ] Test Storage Access Framework
                    - [ ] Execute Terminal Command Approval
                    """.trimIndent()
                )
                File(projectDir, "cheatsheet.md").writeText(
                    """
                    # 💡 Quick Cheatsheet
                    
                    ### Terminal Shortcuts
                    - `ls -la`: List files with permissions
                    - `pwd`: Show current working directory
                    - `cat <file>`: Inspect file content
                    
                    ### AI Coding Agent
                    - Reference files with `@filename` in prompts
                    - Ask for unit test suites and architectural diagrams
                    """.trimIndent()
                )
            }

            else -> {
                // Empty / Default
                File(projectDir, "README.md").writeText(
                    """
                    # ${projectDir.name}
                    
                    Created with **Nova AI** Native Android IDE.
                    
                    - Storage: Unified NovaAI workspace
                    - AI Integration: Active
                    """.trimIndent()
                )
            }
        }
    }
}
