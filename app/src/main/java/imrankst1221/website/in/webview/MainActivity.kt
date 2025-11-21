package com.marsa.absen

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true

        // WAJIB untuk AUTOFILL PASSWORD Google/Chrome
        settings.savePassword = true
        settings.saveFormData = true
        webView.isSaveEnabled = true
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

        // Performance ringan
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.domStorageEnabled = true

        // Tingkatkan kompatibilitas
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Biarkan Chrome handle input login
        webView.autofillClient = webView

        webView.webChromeClient = WebChromeClient()

        // Ganti URL ke web absensi kamu
        webView.loadUrl("https://absensi.smkmaarif9kebumen.sch.id/")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
