package kr.co.supermakers.rms

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.focusone.skscms.util.BackPressedForFinish
import com.google.firebase.messaging.FirebaseMessaging
import com.google.zxing.client.android.BuildConfig
import com.gun0912.tedpermission.provider.TedPermissionProvider
import kr.co.supermakers.rms.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var backPressedForFinish: BackPressedForFinish

    companion object{
        const val TAG="Mainactivity"
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
//                permissionNotification()//알림권한요청
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


    private fun getBackPressedClass(): BackPressedForFinish {
        return backPressedForFinish
    }

    private fun showToast(message: String) {
        Toast.makeText(TedPermissionProvider.context, message, Toast.LENGTH_SHORT).show()
    }
}