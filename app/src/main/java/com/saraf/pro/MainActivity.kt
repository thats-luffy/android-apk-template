package com.saraf.pro

import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val pool = Executors.newFixedThreadPool(4)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(ApiBridge(), "SarafNative")
        web.loadUrl("file:///android_asset/index.html")
        setContentView(web)
    }
    inner class ApiBridge {
        @JavascriptInterface fun get(url: String, requestId: String) {
            pool.execute {
                var code = 0
                var body = ""
                try {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.requestMethod = "GET"
                    c.connectTimeout = 12000
                    c.readTimeout = 15000
                    c.setRequestProperty("User-Agent", "SarafAnalyzerPro/1.0 Android")
                    code = c.responseCode
                    body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
                    c.disconnect()
                } catch (e: Exception) {
                    body = JSONObject(mapOf("error" to (e.message ?: "network error"))).toString()
                }
                val js = "window.__sarafNativeResult(${JSONObject.quote(requestId)},${JSONObject.quote(body)},$code)"
                runOnUiThread { web.evaluateJavascript(js, null) }
            }
        }
    }
    override fun onDestroy() { pool.shutdownNow(); web.destroy(); super.onDestroy() }
}
