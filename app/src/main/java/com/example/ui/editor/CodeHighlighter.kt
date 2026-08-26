package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.SyntaxBuiltin
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxDecorators
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxVariable
import java.util.regex.Pattern

object CodeHighlighter {

    private val pythonKeywords = setOf(
        "and", "as", "assert", "async", "await", "break", "class", "continue",
        "def", "del", "elif", "else", "except", "finally", "for", "from",
        "global", "if", "import", "in", "is", "lambda", "nonlocal", "not",
        "or", "pass", "raise", "return", "try", "while", "with", "yield",
        "True", "False", "None"
    )

    private val pythonBuiltins = setOf(
        "print", "len", "range", "int", "str", "float", "list", "dict", "set",
        "tuple", "bool", "type", "open", "super", "enumerate", "zip", "map",
        "filter", "sum", "min", "max", "abs", "round", "input", "isinstance",
        "getattr", "setattr", "hasattr", "iter", "next", "self", "cls"
    )

    private val jsKeywords = setOf(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function",
        "if", "import", "in", "instanceof", "new", "return", "super", "switch",
        "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
        "let", "static", "enum", "await", "async", "null", "undefined", "true", "false"
    )

    private val sqlKeywords = setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "CREATE",
        "TABLE", "DROP", "ALTER", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON",
        "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "AND", "OR", "NOT",
        "NULL", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "INDEX", "VALUES", "AS"
    )

    fun highlight(text: String, fileName: String): AnnotatedString {
        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "py", "pyw" -> highlightPython(text)
            "json" -> highlightJson(text)
            "xml", "html", "svg" -> highlightXml(text)
            "yml", "yaml" -> highlightYaml(text)
            "js", "ts" -> highlightJs(text)
            "sql" -> highlightSql(text)
            "md", "markdown" -> highlightMarkdown(text)
            "sh", "bash" -> highlightShell(text)
            else -> buildAnnotatedString { append(text) }
        }
    }

    private fun highlightPython(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val len = text.length
            if (len == 0) return@buildAnnotatedString

            // Highlight Comments (# ...)
            val commentMatcher = Pattern.compile("#.*").matcher(text)
            while (commentMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
            }

            // Highlight Strings (""" ... """, ''' ... ''', " ... ", ' ... ')
            val stringMatcher = Pattern.compile("(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"\\n]*\"|'[^'\\n]*')").matcher(text)
            while (stringMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), stringMatcher.start(), stringMatcher.end())
            }

            // Highlight Decorators (@decorator)
            val decoratorMatcher = Pattern.compile("@[a-zA-Z0-9_.]+").matcher(text)
            while (decoratorMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxDecorators, fontWeight = FontWeight.SemiBold), decoratorMatcher.start(), decoratorMatcher.end())
            }

            // Highlight Numbers
            val numberMatcher = Pattern.compile("\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b").matcher(text)
            while (numberMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxNumber), numberMatcher.start(), numberMatcher.end())
            }

            // Highlight Functions & Classes (def foo / class Bar)
            val defMatcher = Pattern.compile("\\b(?:def|class)\\s+([a-zA-Z0-9_]+)").matcher(text)
            while (defMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Bold), defMatcher.start(1), defMatcher.end(1))
            }

            // Highlight Words (Keywords & Builtins)
            val wordMatcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").matcher(text)
            while (wordMatcher.find()) {
                val word = wordMatcher.group()
                if (pythonKeywords.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
                } else if (pythonBuiltins.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxBuiltin), wordMatcher.start(), wordMatcher.end())
                }
            }
        }
    }

    private fun highlightJson(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            // Keys
            val keyMatcher = Pattern.compile("\"([^\"]+)\"\\s*:").matcher(text)
            while (keyMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxVariable, fontWeight = FontWeight.SemiBold), keyMatcher.start(1), keyMatcher.end(1))
            }
            // Strings
            val stringMatcher = Pattern.compile(":\\s*\"([^\"]*)\"").matcher(text)
            while (stringMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), stringMatcher.start(1), stringMatcher.end(1))
            }
            // Numbers & Booleans
            val numMatcher = Pattern.compile("\\b(\\d+(?:\\.\\d+)?|true|false|null)\\b").matcher(text)
            while (numMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxNumber), numMatcher.start(), numMatcher.end())
            }
        }
    }

    private fun highlightXml(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            // Tags
            val tagMatcher = Pattern.compile("</?[a-zA-Z0-9_:-]+").matcher(text)
            while (tagMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), tagMatcher.start(), tagMatcher.end())
            }
            // Attributes
            val attrMatcher = Pattern.compile("\\s+([a-zA-Z0-9_:-]+)=").matcher(text)
            while (attrMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxVariable), attrMatcher.start(1), attrMatcher.end(1))
            }
            // Strings
            val strMatcher = Pattern.compile("\"[^\"]*\"").matcher(text)
            while (strMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), strMatcher.start(), strMatcher.end())
            }
        }
    }

    private fun highlightYaml(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            // Keys
            val keyMatcher = Pattern.compile("^\\s*([a-zA-Z0-9_.-]+):", Pattern.MULTILINE).matcher(text)
            while (keyMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxVariable, fontWeight = FontWeight.SemiBold), keyMatcher.start(1), keyMatcher.end(1))
            }
            // Comments
            val commentMatcher = Pattern.compile("#.*").matcher(text)
            while (commentMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
            }
            // Strings
            val strMatcher = Pattern.compile("\"[^\"]*\"|'[^']*'").matcher(text)
            while (strMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), strMatcher.start(), strMatcher.end())
            }
        }
    }

    private fun highlightJs(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val commentMatcher = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/").matcher(text)
            while (commentMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
            }
            val strMatcher = Pattern.compile("\"[^\"\\n]*\"|'[^'\\n]*'|`[^`]*`").matcher(text)
            while (strMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), strMatcher.start(), strMatcher.end())
            }
            val wordMatcher = Pattern.compile("\\b[a-zA-Z_$][a-zA-Z0-9_$]*\\b").matcher(text)
            while (wordMatcher.find()) {
                val word = wordMatcher.group()
                if (jsKeywords.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
                }
            }
        }
    }

    private fun highlightSql(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val wordMatcher = Pattern.compile("\\b[a-zA-Z_]+\\b").matcher(text)
            while (wordMatcher.find()) {
                val word = wordMatcher.group().uppercase()
                if (sqlKeywords.contains(word)) {
                    addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
                }
            }
            val strMatcher = Pattern.compile("'[^']*'").matcher(text)
            while (strMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), strMatcher.start(), strMatcher.end())
            }
        }
    }

    private fun highlightMarkdown(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            // Headers (# Header)
            val headerMatcher = Pattern.compile("^#{1,6}\\s+.*$", Pattern.MULTILINE).matcher(text)
            while (headerMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Bold), headerMatcher.start(), headerMatcher.end())
            }
            // Code blocks
            val codeMatcher = Pattern.compile("`[^`]+`").matcher(text)
            while (codeMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), codeMatcher.start(), codeMatcher.end())
            }
        }
    }

    private fun highlightShell(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val commentMatcher = Pattern.compile("#.*").matcher(text)
            while (commentMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
            }
            val strMatcher = Pattern.compile("\"[^\"]*\"|'[^']*'").matcher(text)
            while (strMatcher.find()) {
                addStyle(SpanStyle(color = SyntaxString), strMatcher.start(), strMatcher.end())
            }
        }
    }
}
