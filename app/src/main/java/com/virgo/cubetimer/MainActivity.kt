package com.virgo.cubetimer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.File
import java.io.FileOutputStream


class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 计时过程中保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        webView = WebView(this)
        setContentView(webView)

        setUpWebView()
        webView.loadUrl(START_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        // localStorage / IndexedDB：成绩、设置、会话都靠它持久化
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        // 节拍器、8 秒观察提醒等音频无需用户手势即可播放
        settings.mediaPlaybackRequiresUserGesture = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url
                // 站内资源（含 blob/data）放行，其余一律交给系统浏览器
                if (url.host == APP_HOST || url.scheme == "blob" || url.scheme == "data") {
                    return false
                }
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (e: Exception) {
                    true
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    val intent = params.createIntent()
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    startActivityForResult(
                        Intent.createChooser(intent, getString(R.string.choose_file)),
                        REQUEST_FILE_CHOOSER
                    )
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }

        // 导出成绩/打乱时的 blob 下载桥
        webView.addJavascriptInterface(WebBridge(), "AndroidBridge")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FILE_CHOOSER) {
            val callback = filePathCallback
            filePathCallback = null
            if (callback != null) {
                var results: Array<Uri>? = null
                if (resultCode == RESULT_OK && data != null) {
                    val clipData = data.clipData
                    val dataString = data.dataString
                    results = when {
                        clipData != null -> Array(clipData.itemCount) { clipData.getItemAt(it).uri }
                        dataString != null -> arrayOf(Uri.parse(dataString))
                        else -> null
                    }
                }
                callback.onReceiveValue(results)
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("平台返回键的兼容实现")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            moveTaskToBack(true)
        }
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("AndroidBridge")
        webView.destroy()
        super.onDestroy()
    }

    /** 接收网页里 blob 下载 shim 传来的 base64 数据并落盘。 */
    private inner class WebBridge {
        @JavascriptInterface
        fun saveFile(name: String, base64: String) {
            val safeName = sanitize(name)
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToPublicDownloads(safeName, bytes)
                } else {
                    saveToAppDownloads(safeName, bytes)
                }
                toast(getString(R.string.saved_to, location))
            } catch (e: Exception) {
                toast(getString(R.string.save_failed, e.message ?: e.javaClass.simpleName))
            }
        }
    }

    /** Android 10+：写入公共「下载」目录，无需任何权限。 */
    private fun saveToPublicDownloads(name: String, bytes: ByteArray): String {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeOf(name))
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法创建下载条目")
        resolver.openOutputStream(uri).use { stream ->
            stream ?: throw IllegalStateException("无法打开输出流")
            stream.write(bytes)
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return "${Environment.DIRECTORY_DOWNLOADS}/$name"
    }

    /** Android 7~9：退回到应用私有外部目录（无需权限），路径随 Toast 提示给用户。 */
    private fun saveToAppDownloads(name: String, bytes: ByteArray): String {
        val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        val file = File(dir, name)
        FileOutputStream(file).use { it.write(bytes) }
        return file.absolutePath
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_").trim()
        return if (cleaned.isEmpty()) "virgo.txt" else cleaned
    }

    private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "json" -> "application/json"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        else -> "application/octet-stream"
    }

    private fun toast(message: String) {
        runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show() }
    }

    private companion object {
        const val APP_HOST = "appassets.androidplatform.net"
        const val START_URL = "https://appassets.androidplatform.net/assets/www/index.html"
        const val REQUEST_FILE_CHOOSER = 1001
    }
}