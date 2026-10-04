package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.ui.theme.NovaCyanLight
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "val", "var",
        "when", "while", "by", "catch", "constructor", "delegate", "dynamic",
        "field", "file", "finally", "get", "import", "init", "param", "property",
        "receiver", "set", "setparam", "where", "actual", "abstract", "annotation",
        "companion", "const", "crossinline", "data", "enum", "expect", "external",
        "final", "infix", "inline", "inner", "internal", "lateinit", "noinline",
        "open", "operator", "out", "override", "private", "protected", "public",
        "reified", "sealed", "suspend", "tailrec", "vararg"
    )

    private val PYTHON_KEYWORDS = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break",
        "class", "continue", "def", "del", "elif", "else", "except", "finally",
        "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
        "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
    )

    private val JS_KEYWORDS = setOf(
        "async", "await", "break", "case", "catch", "class", "const", "continue",
        "debugger", "default", "delete", "do", "else", "export", "extends", "finally",
        "for", "function", "if", "import", "in", "instanceof", "let", "new", "return",
        "super", "switch", "this", "throw", "try", "typeof", "var", "void", "while",
        "with", "yield", "true", "false", "null", "undefined"
    )

    fun highlightCode(text: String, extension: String): AnnotatedString {
        val ext = extension.lowercase()
        val keywords = when (ext) {
            "kt", "kts" -> KOTLIN_KEYWORDS
            "py" -> PYTHON_KEYWORDS
            "js", "ts", "jsx", "tsx" -> JS_KEYWORDS
            else -> KOTLIN_KEYWORDS
        }

        return buildAnnotatedString {
            append(text)

            // 1. Strings
            val stringRegex = Regex("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'")
            for (match in stringRegex.findAll(text)) {
                addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
            }

            // 2. Line Comments
            val lineCommentRegex = when (ext) {
                "py", "sh" -> Regex("#.*")
                else -> Regex("//.*")
            }
            for (match in lineCommentRegex.findAll(text)) {
                addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
            }

            // 3. Numbers
            val numberRegex = Regex("\\b\\d+(\\.\\d+)?([fFlL])?\\b")
            for (match in numberRegex.findAll(text)) {
                addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
            }

            // 4. Keywords
            val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
            for (match in wordRegex.findAll(text)) {
                val word = match.value
                if (keywords.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxKeyword), match.range.first, match.range.last + 1)
                } else if (word.firstOrNull()?.isUpperCase() == true) {
                    addStyle(SpanStyle(color = SyntaxType), match.range.first, match.range.last + 1)
                }
            }

            // 5. Annotations
            val annotationRegex = Regex("@[a-zA-Z_][a-zA-Z0-9_]*")
            for (match in annotationRegex.findAll(text)) {
                addStyle(SpanStyle(color = NovaCyanLight), match.range.first, match.range.last + 1)
            }
        }
    }
}
