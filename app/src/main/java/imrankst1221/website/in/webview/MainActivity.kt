package imrankst1221.website.`in`.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var mContext: Context
    private var mLoaded = false

    // set your custom url here
    private var URL = "https://absensi.smkmaarif9kebumen.sch.id/"

    // for attach files
    private var mCameraPhotoPath: String? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var doubleBackToExitPressedOnce = false

    private lateinit var btnTryAgain: Button
    private lateinit var mWebView: WebView
    private lateinit var prgs: ProgressBar
    private var viewSplash: View? = null
    private lateinit var layoutSplash: RelativeLayout
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var layoutNoInternet: RelativeLayout

    // ActivityResult launcher for permissions (multiple)
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // optional: log permission results
        perms.entries.forEach { Log.d(TAG, "${it.key} = ${it.value}") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        mContext = this
        mWebView = findViewById(R.id.webview)
        prgs = findViewById(R.id.progressBar)
        btnTryAgain = findViewById(R.id.btn_try_again)
        viewSplash = findViewById(R.id.view_splash)
        layoutWebview = findViewById(R.id.layout_webview)
        layoutNoInternet = findViewById(R.id.layout_no_internet)
        layoutSplash = findViewById(R.id.layout_splash)

        // request runtime permissions early (camera + location + storage fallback)
        requestRuntimePermissions()

        // request for show website
        requestForWebview()

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            prgs.visibility = View.VISIBLE
            layoutSplash.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            requestForWebview()
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf<String>()
        perms.add(android.Manifest.permission.CAMERA)
        perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        // READ_EXTERNAL_STORAGE for older devices; WRITE limited by manifest maxSdkVersion
        perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissionsLauncher.launch(perms.toTypedArray())
    }

    private fun requestForWebview() {
        if (!mLoaded) {
            requestWebView()
            Handler().postDelayed({
                prgs.visibility = View.VISIBLE
                mWebView.visibility = View.VISIBLE
            }, 3000)
        } else {
            mWebView.visibility = View.VISIBLE
            prgs.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestWebView() {
        if (internetCheck(mContext)) {
            mWebView.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            mWebView.loadUrl(URL)
        } else {
            prgs.visibility = View.GONE
            mWebView.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.VISIBLE
            return
        }

        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        val settings = mWebView.settings
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.domStorageEnabled = true
        settings.setSupportMultipleWindows(false)
        // Deprecated calls removed (setRenderPriority, setAppCacheEnabled, databaseEnabled)
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                if (url == null) return true
                Log.d(TAG, "URL: $url")
                if (internetCheck(mContext)) {
                    view.loadUrl(url)
                } else {
                    prgs.visibility = View.GONE
                    mWebView.visibility = View.GONE
                    layoutSplash.visibility = View.GONE
                    layoutNoInternet.visibility = View.VISIBLE
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (prgs.visibility == View.GONE) prgs.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                mLoaded = true
                if (prgs.visibility == View.VISIBLE) prgs.visibility = View.GONE
                Handler().postDelayed({ layoutSplash.visibility = View.GONE }, 2000)
            }
        }

        // file attach + permissions + camera and location permission request for WebView JS
        mWebView.webChromeClient = object : WebChromeClient() {

            // Handle JS permission requests (camera / microphone / location)
            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) {
                    super.onPermissionRequest(request)
                    return
                }
                // check runtime permissions before granting
                val required = request.resources
                val granted = mutableListOf<String>()
                for (res in required) {
                    when (res) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            if (checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                granted.add(res)
                            }
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                granted.add(res)
                            }
                        }
                        PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID,
                        PermissionRequest.RESOURCE_MIDI,
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE_OUTPUT -> {
                            // ignore
                        }
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE, PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                            // handled above
                        }
                        else -> {
                            // location requests are sometimes sent via request.origin — grant if location permission present
                            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                granted.add(res)
                            }
                        }
                    }
                }
                if (granted.isNotEmpty()) {
                    request.grant(granted.toTypedArray())
                } else {
                    request.deny()
                    // optionally request runtime permissions here
                    requestRuntimePermissions()
                }
            }

            // file chooser (camera + gallery)
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                // Ensure CAMERA permission exists before launching camera
                if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestRuntimePermissions()
                    // will return; web will call again after user grants
                }

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent != null && takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        mCameraPhotoPath = photoFile?.absolutePath
                    } catch (ex: IOException) {
                        Log.e(TAG, "Unable to create Image File", ex)
                    }

                    if (photoFile != null) {
                        // Use FileProvider for Android N+
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "${applicationContext.packageName}.fileprovider",
                            photoFile
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        takePictureIntent = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray = if (takePictureIntent != null) arrayOf(takePictureIntent) else arrayOfNulls<Intent>(0)

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                return true
            }
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        // use app-specific external dir (no WRITE_EXTERNAL_STORAGE required for this)
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: filesDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null

        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.fromFile(File(mCameraPhotoPath)))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                } else if (data.clipData != null) {
                    val cd = data.clipData!!
                    val uris = Array(cd.itemCount) { i -> cd.getItemAt(i).uri }
                    results = uris
                }
            }
        }

        mFilePathCallback?.onReceiveValue(results)
        mFilePathCallback = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }

        if (doubleBackToExitPressedOnce) {
            return super.onKeyDown(keyCode, event)
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        return true
    }

    companion object {
        internal var TAG = "---MainActivity"
        const val INPUT_FILE_REQUEST_CODE = 1
        const val EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION"

        fun internetCheck(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
