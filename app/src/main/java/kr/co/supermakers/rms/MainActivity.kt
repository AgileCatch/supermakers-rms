package kr.co.supermakers.rms

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.focusone.skscms.util.BackPressedForFinish
import com.google.zxing.client.android.BuildConfig
import kr.co.supermakers.rms.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var backPressedForFinish: BackPressedForFinish

    companion object{
        const val TAG="Mainactivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

    }

    private fun initView()= with(binding) {
        val baseUrl = kr.co.supermakers.rms.BuildConfig.MAIN_URL
        mainWebView.loadUrl(baseUrl)
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


    private fun getBackPressedClass(): BackPressedForFinish {
        return backPressedForFinish
    }
}