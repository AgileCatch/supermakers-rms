package kr.co.supermakers.rms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.webkit.ValueCallback
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.focusone.skscms.util.BackPressedForFinish
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.client.android.BuildConfig
import com.google.zxing.client.android.Intents
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.gun0912.tedpermission.provider.TedPermissionProvider
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kr.co.supermakers.rms.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var backPressedForFinish: BackPressedForFinish

    companion object{
        const val TAG="Mainactivity"
        private const val REQUEST_CODE_MEDIA = 2001
        private const val REQUEST_CODE_CAMERA = 2002
        private const val REQUEST_CODE_NOTIFICATION = 2003

        // 파일 첨부
        private var mUploadMessage: ValueCallback<Uri?>? = null
        private var mUploadMessages: ValueCallback<Array<Uri>>? = null
        private var imageUri: Uri? = null

        // 파일첨부 resultCode.
        private const val RESULT_CODE_FILECHOOSER = 1001
        private const val RESULT_CODE_FILECHOOSER_LOLLIPOP = 1002
        private const val RESULT_CODE_ACTIVITY_APPLICATION_SETTINGS = 2005

        val UPLOAD_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //[1] 파일 첨부 callback 결과 처리
        if (requestCode == RESULT_CODE_FILECHOOSER || requestCode == RESULT_CODE_FILECHOOSER_LOLLIPOP) fileChooserResult(
            requestCode,
            resultCode,
            data
        ) else if (RESULT_CODE_ACTIVITY_APPLICATION_SETTINGS == requestCode) {
            // 권한 체크 수행
            permissionMedia()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
        initFireBase()
    }

    private fun initView()= with(binding) {
        val baseUrl = kr.co.supermakers.rms.BuildConfig.MAIN_URL
        mainWebView.loadUrl(baseUrl)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initFireBase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                token?.let {
                    showToast("토큰: $it")
                    Log.e(TAG, "토큰 값: $it")

                }
                permissionNotification()//알림권한요청
            } else {
                // 토큰을 가져오는 데 실패한 경우
                showToast("토큰을 가져오는 데 실패했습니다.")
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = with(binding) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mainWebView.canGoBack()) {
            val msg = ">>>>> canGoBack: [${mainWebView.url}]"
            Log.e(TAG, msg)
            val nIndex = 2
            val historyList = mainWebView.copyBackForwardList()
            var mallMainUrl = ""
            val webHistoryItem = historyList.getItemAtIndex(nIndex)
            if (webHistoryItem != null) {
                mallMainUrl = webHistoryItem.url
            }
            if (mainWebView.url.equals(mallMainUrl, ignoreCase = true)) {
                val backBtn: BackPressedForFinish = getBackPressedClass()
                backBtn.onBackPressed()
            } else {
                mainWebView.goBack() // 뒤로가기
            }
        } else if (keyCode == KeyEvent.KEYCODE_BACK && !mainWebView.canGoBack()) {
            val backBtn: BackPressedForFinish = getBackPressedClass()
            backBtn.onBackPressed()
        } else {
            return super.onKeyDown(keyCode, event)
        }
        return true
    }

    val mBarcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult? ->
        Log.e(TAG, "Barcode Scanner Callback is called with result: $result")
        if (result != null) {
            val originalIntent = result.originalIntent
            if (originalIntent != null) {
                if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                    permissionCamera()
                } else if (result.contents != null) {
                    Log.e(TAG, "Barcode : " + result.contents)
                    Log.e(TAG, "CallMethod : " + BaseWebView.mBarcodeCallMethod)
                    binding.mainWebView.loadUrl("javascript:" + BaseWebView.mBarcodeCallMethod + "('" + result.contents + "')")
                }
            }
        }
    }

    fun requestFCMToken(callbackMethod: String) {
        Log.e(TAG, "FCMToken result: $callbackMethod")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                token?.let {
                    val jsCallback = "$callbackMethod('$it')"
                    runOnUiThread {
                        binding.mainWebView.evaluateJavascript(jsCallback, null)
                    }
                }
            } else {
                showToast("FCM 토큰을 가져오는 데 실패했습니다.")
            }
        }
    }

    fun doFileAttach(uploadMsg: ValueCallback<Uri?>) {
        mUploadMessage = uploadMsg
        showAttachmentDialog(false)
    }

    fun doFileAttachs(uploadMsg: ValueCallback<Array<Uri>>?) {
        mUploadMessages = uploadMsg
        showAttachmentDialog(true)
    }

    private fun showAttachmentDialog(isLOLLIPOP: Boolean) {
        // Create AndroidExampleFolder at sdcard
        val imageStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "SCMS"
        )
        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs()
        }

        // Create camera captured image file path and name
        val file = File(
            imageStorageDir.toString() + File.separator + "IMG_" + System.currentTimeMillis()
                .toString() + ".jpg"
        )
        imageUri = Uri.fromFile(file)

        // Camera capture image intent
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.setType("*/*")

        // Create file chooser intent
        val chooserIntent = Intent.createChooser(i, "File Chooser")

        // Set camera intent to file chooser
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent))

        // On select image call onActivityResult method of activity
        if (isLOLLIPOP) {
            startActivityForResult(chooserIntent, RESULT_CODE_FILECHOOSER_LOLLIPOP)
        } else {
            startActivityForResult(chooserIntent, RESULT_CODE_FILECHOOSER)
        }
    }

    private fun fileChooserResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (mUploadMessage == null && mUploadMessages == null) {
                return
            }
            if (requestCode == RESULT_CODE_FILECHOOSER) {
                Log.e(TAG, "requestCode : RESULTCODE_FILECHOOSER || RESULTCODE_CAMERA")
                var result: Uri? = null
                result = if (data == null) {
                    imageUri
                } else {
                    data.data
                }
                mUploadMessage?.onReceiveValue(result)
                mUploadMessage = null
            } else if (requestCode == RESULT_CODE_FILECHOOSER_LOLLIPOP) {
                Log.e(
                    TAG,
                    "requestCode : RESULTCODE_FILECHOOSER_LOLLIPOP || RESULTCODE_CAMRERA_LOLLIPOP"
                )
                val result: Array<Uri>
                val uri: String = imageUri.toString()
                result = if (data == null || data.data == null) {
                    arrayOf(Uri.parse(uri))
                } else {
                    arrayOf(Uri.parse(data.dataString))
                }
                mUploadMessages?.onReceiveValue(result)
                mUploadMessages = null
            }
        } else {
            if (requestCode == RESULT_CODE_FILECHOOSER) {
                mUploadMessage?.onReceiveValue(null)
                mUploadMessage = null
            } else if (requestCode == RESULT_CODE_FILECHOOSER_LOLLIPOP) {
                mUploadMessages?.onReceiveValue(null)
                mUploadMessages = null
            }

            // 앨범접근은 필수 권한이기 때문에  resultCode == RESULT_OK 이다.
            // 반면에 카메라 접근 권한은 선택 권한 이기 때문에 사용자가 미허용시
            // resultCode != RESULT_OK 이어서 이곳에서 카메라 접근 권한을 노출 한다.
            permissionCamera()
        }
    }

    private fun permissionMedia() {
        TedPermission.create()
            .setRationaleMessage(R.string.string_common_permission)
            .setDeniedMessage(R.string.string_common_media_alert)
            .setPermissions(*UPLOAD_PERMISSIONS)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    //이미 권한이 있거나 사용자가 권한을 허용했을 때 호출
                    requestPermissions(
                        arrayOf(*UPLOAD_PERMISSIONS),
                        REQUEST_CODE_MEDIA
                    )

                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    //요청이 거부 되었을 때 호출
                }
            }).check()

    }

    // 미디어(카메라) 접근 권한
    private fun permissionCamera() {
        TedPermission.create()
            .setRationaleMessage(R.string.string_common_permission)
            .setDeniedMessage(R.string.string_common_camera_alert)
            .setPermissions(Manifest.permission.CAMERA)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    //이미 권한이 있거나 사용자가 권한을 허용했을 때 호출
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                            arrayOf(Manifest.permission.CAMERA),
                            REQUEST_CODE_CAMERA
                        )
                        Handler().post { mBarcodeLauncher.launch(ScanOptions()) }//바코드 실행
                    }
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    //요청이 거부 되었을 때 호출
                }
            }).check()

    }

    // 알림 접근 권한
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun permissionNotification() {
        TedPermission.create()
            .setDeniedMessage(R.string.string_common_notification_alert)
            .setPermissions(Manifest.permission.POST_NOTIFICATIONS)
            .setPermissionListener(object : PermissionListener {
                @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                override fun onPermissionGranted() {
                    //이미 권한이 있거나 사용자가 권한을 허용했을 때 호출
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_CODE_NOTIFICATION
                        )
                    }
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    //요청이 거부 되었을 때 호출
                }
            }).check()

    }


    private fun getBackPressedClass(): BackPressedForFinish {
        return backPressedForFinish
    }

    private fun showToast(message: String) {
        Toast.makeText(TedPermissionProvider.context, message, Toast.LENGTH_SHORT).show()
    }
}