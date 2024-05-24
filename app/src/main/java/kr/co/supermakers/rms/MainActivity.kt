package kr.co.supermakers.rms

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.client.android.BuildConfig
import kr.co.supermakers.rms.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()

    }

    private fun initView()= with(binding) {
        val baseUrl = kr.co.supermakers.rms.BuildConfig.MAIN_URL
        mainWebView.loadUrl(baseUrl)
    }
}