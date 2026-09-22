package com.example.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

data class GeneratedCodeSnippet(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val language: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val isWebsite: Boolean = false,
    val isVisibleOnUi: Boolean = true
)

object LocalWebServer {
    private const val TAG = "LocalWebServer"
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var serverPort = 8088
    private var rootDir: File? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(context: Context): Int {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
            return serverPort
        }
        rootDir = File(context.filesDir, "websites").apply { mkdirs() }
        try {
            serverSocket = try {
                ServerSocket(8088)
            } catch (e: Exception) {
                ServerSocket(0)
            }
            serverPort = serverSocket!!.localPort
            isRunning = true
            Log.i(TAG, "LocalWebServer started on port $serverPort serving ${rootDir?.absolutePath}")

            scope.launch {
                while (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalWebServer", e)
        }
        return serverPort
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = BufferedOutputStream(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            var path = parts[1]

            if (path == "/" || path.isBlank()) {
                path = "/index.html"
            }
            val cleanPath = path.substringBefore("?").removePrefix("/")

            val requestedFile = File(rootDir, cleanPath)
            if (requestedFile.exists() && requestedFile.isFile) {
                val mimeType = when {
                    cleanPath.endsWith(".html", ignoreCase = true) -> "text/html; charset=utf-8"
                    cleanPath.endsWith(".css", ignoreCase = true) -> "text/css; charset=utf-8"
                    cleanPath.endsWith(".js", ignoreCase = true) -> "application/javascript; charset=utf-8"
                    cleanPath.endsWith(".json", ignoreCase = true) -> "application/json; charset=utf-8"
                    cleanPath.endsWith(".png", ignoreCase = true) -> "image/png"
                    cleanPath.endsWith(".jpg", ignoreCase = true) || cleanPath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    cleanPath.endsWith(".svg", ignoreCase = true) -> "image/svg+xml"
                    else -> "text/plain; charset=utf-8"
                }

                val bytes = requestedFile.readBytes()
                val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mimeType\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n"

                output.write(header.toByteArray())
                output.write(bytes)
                output.flush()
            } else {
                val notFound = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\n404 Not Found"
                output.write(notFound.toByteArray())
                output.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client request", e)
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    fun getPort(): Int = serverPort
}

object CodeStudioManager {
    private const val TAG = "CodeStudioManager"

    private val defaultWebsiteCode = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>shadow x rahul Test Site - Maya Your Pocket Companion</title>
    <link rel="stylesheet" href="style.css">
</head>
<body class="glow-orb top" aria-hidden="true"></div>
<div class="glow-orb bottom" aria-hidden="true"></div>

<!-- Header -->
<header class="site-header">
    <div class="header-container">
        <div class="brand">
            <span class="brand-art-pulse" aria-hidden="true">❤️</span>
            <span class="brand-title">Maya Companion</span>
        </div>
        <nav class="nav-links">
            <a href="#features">Features</a>
            <a href="#stats">Stats</a>
            <a href="#community">Community</a>
        </nav>
    </div>
</header>

<main class="hero-section">
    <div class="avatar-card">
        <div class="avatar-frame">
            <div class="avatar-glow"></div>
        </div>
    </div>
    <h1 class="hero-title">Maya AI Companion</h1>
    <p class="hero-subtitle">Smart Assistant • Live Voice • Code Synthesizer</p>
</main>
</body>
</html>
    """.trimIndent()

    private val _activeCode = MutableStateFlow<GeneratedCodeSnippet?>(null)
    val activeCode: StateFlow<GeneratedCodeSnippet?> = _activeCode.asStateFlow()

    private val _codeHistory = MutableStateFlow<List<GeneratedCodeSnippet>>(emptyList())
    val codeHistory: StateFlow<List<GeneratedCodeSnippet>> = _codeHistory.asStateFlow()

    fun setCode(snippet: GeneratedCodeSnippet) {
        _activeCode.value = snippet
        _codeHistory.value = listOf(snippet) + _codeHistory.value.take(20)
    }

    fun clearActiveCode() {
        _activeCode.value = null
    }

    fun isChromeInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.android.chrome", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun buildAndLaunchWebsite(
        context: Context,
        title: String,
        htmlContent: String,
        openInChrome: Boolean = true
    ): String {
        try {
            val websitesDir = File(context.filesDir, "websites").apply { mkdirs() }
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
            val fileName = "website_${cleanTitle}_${System.currentTimeMillis()}.html"
            val htmlFile = File(websitesDir, fileName)

            // Ensure htmlContent has proper DOCTYPE and viewport if not already included
            val finalHtml = if (!htmlContent.contains("<html", ignoreCase = true)) {
                """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$title</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background: #0f172a; color: #f8fafc; }
                    </style>
                </head>
                <body>
                    $htmlContent
                </body>
                </html>
                """.trimIndent()
            } else {
                htmlContent
            }

            htmlFile.writeText(finalHtml)

            val snippet = GeneratedCodeSnippet(
                title = title,
                language = "HTML/CSS/JS",
                code = finalHtml,
                filePath = htmlFile.absolutePath,
                isWebsite = true,
                isVisibleOnUi = true
            )
            setCode(snippet)

            if (openInChrome) {
                val serverPort = LocalWebServer.start(context)
                val runningUrl = "http://127.0.0.1:$serverPort/$fileName"
                val chromeAvailable = isChromeInstalled(context)

                if (chromeAvailable) {
                    val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(runningUrl)).apply {
                        setPackage("com.android.chrome")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(chromeIntent)
                        Log.d(TAG, "Opened website in Chrome: $runningUrl")
                        return "Website ready hai aur Chrome me open kar di hai."
                    } catch (e: Exception) {
                        Log.w(TAG, "Chrome direct launch failed, trying fallback intent", e)
                    }
                }

                // Fallback to default browser
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(runningUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    Log.d(TAG, "Opened website in browser: $runningUrl")
                    return if (chromeAvailable) {
                        "Website ready hai aur Chrome me open kar di hai."
                    } else {
                        "Website successfully ready hai, lekin Chrome me open nahi ho saki."
                    }
                } catch (e: Exception) {
                    // Try FileProvider intent as last resort
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        htmlFile
                    )
                    val fileIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/html")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fileIntent)
                    return "Website successfully ready hai, lekin Chrome me open nahi ho saki."
                }
            }

            return "Website '$title' built and displayed in UI background."
        } catch (e: Exception) {
            Log.e(TAG, "Error building website", e)
            return "Website build karte waqt error aaya hai. Main pehle us problem ko fix karungi."
        }
    }

    fun displayCode(
        title: String,
        language: String,
        code: String
    ): String {
        val snippet = GeneratedCodeSnippet(
            title = title,
            language = language.uppercase(),
            code = code,
            isWebsite = language.equals("html", ignoreCase = true) || language.equals("web", ignoreCase = true),
            isVisibleOnUi = true
        )
        setCode(snippet)
        return "Code for '$title' ($language) generated and displayed in UI background."
    }
}
