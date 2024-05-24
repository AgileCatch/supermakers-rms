package kr.co.supermakers.rms

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.journeyapps.barcodescanner.ScanOptions

class BaseWebView : WebView {

    companion object {
        private const val TAG = "BaseWebView"
        lateinit var mWebView: BaseWebView
        var mContext: Context? = null
        var mBarcodeCallMethod = ""
        var mPushCallMethod = ""
        var mTokenCallMethod = ""

    }

    constructor(context: Context) : super(context) {
        mContext = context
        initializeOptions()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        initializeOptions()
    }

    init {
        initializeOptions()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeOptions() {
        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }

        // WebView 설정
        val webSettings: WebSettings = this.settings
        webSettings.apply {
            loadsImagesAutomatically = true
            javaScriptEnabled = true    // 웹페이지 자바스크립트 허용 여부
            setSupportMultipleWindows(true) //멀티윈도우를 지원할지 여부
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true //컨텐츠가 웹뷰보다 클때 스크린 크기에 맞추기
            useWideViewPort = true  // 화면 사이즈 맞추기 허용 여부

            domStorageEnabled = true     //DOM 로컬 스토리지 사용여부
            databaseEnabled = true  //database storage API 사용 여부
            allowFileAccess = true  //파일 액세스 허용 여부
            allowContentAccess = true    //Content URL 에 접근 사용 여부

            textZoom = 100  // system 글꼴 크기에 의해 변하는 것 방지

            setSupportZoom(true)    // 화면 줌 허용 여부
            builtInZoomControls = true  // 줌 아이콘
            displayZoomControls = false // 웹뷰 화면에 보이는 (+/-) 줌 아이콘


            // user-agent에 ",hazzys@LF" 등을 추가 하여 Web 에서 App 인지를 판단 하게 한다.
            setUserAgent(webSettings)

            // https -> http 호출 허용
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING   // 컨텐츠 사이즈 자동 맞추기
            cacheMode = WebSettings.LOAD_DEFAULT

        }

        // 서드파티 쿠키 허용.
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        // App <----> Javascript 통신객체 생성
//        addJavascriptInterface(AndroidScriptBridge(this), "superkitchen")

        // WebViewClient 설정
        webViewClient = MyWebViewClient()
        // WebChromeClient 설정
        webChromeClient = MyWebChromeClient()

    }

    class MyWebViewClient : WebViewClient() {
        //페이지 로딩 시작
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.e(TAG, "onPageStarted URL : $url")
            if (favicon != null) {
                // favicon이 null이 아닌 경우에 대한 처리
            } else {
                // favicon이 null인 경우에 대한 처리
            }
        }

        //오류 처리
        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            Log.e(TAG, "onReceivedError : $failingUrl")
            handleError(errorCode)
        }

        private fun handleError(errorCode: Int) {
            when (errorCode) {
                ERROR_AUTHENTICATION -> Log.e(TAG, "onReceivedError : 서버에서 사용자 인증 실패")
                ERROR_BAD_URL -> Log.e(TAG, "onReceivedError : 잘못된 URL")
                ERROR_CONNECT -> Log.e(TAG, "onReceivedError : 서버로 연결 실패")
                ERROR_FAILED_SSL_HANDSHAKE -> Log.e(TAG, "onReceivedError : SSL handshake 수행 실패")
                ERROR_FILE -> Log.e(TAG, "onReceivedError : 일반 파일 오류")
                ERROR_FILE_NOT_FOUND -> Log.e(TAG, "onReceivedError : 파일을 찾을 수 없습니다")
                ERROR_HOST_LOOKUP -> Log.e(TAG, "onReceivedError : 서버 또는 프록시 호스트 이름 조회 실패")
                ERROR_IO -> Log.e(TAG, "onReceivedError : 서버에서 읽거나 서버로 쓰기 실패")
                ERROR_PROXY_AUTHENTICATION -> Log.e(TAG, "onReceivedError : 프록시에서 사용자 인증 실패")
                ERROR_REDIRECT_LOOP -> Log.e(TAG, "onReceivedError : 너무 많은 리디렉션")
                ERROR_TIMEOUT -> Log.e(TAG, "onReceivedError : 연결 시간 초과")
                ERROR_TOO_MANY_REQUESTS -> Log.e(TAG, "onReceivedError : 페이지 로드중 너무 많은 요청 발생")
                ERROR_UNKNOWN -> Log.e(TAG, "onReceivedError : 일반 오류")
                ERROR_UNSUPPORTED_AUTH_SCHEME -> Log.e(TAG, "onReceivedError : 지원되지 않는 인증 체계")
                ERROR_UNSUPPORTED_SCHEME -> Log.e(TAG, "onReceivedError : URI가 지원되지 않는 방식")
            }
        }

        //페이지 로딩 완료
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            Log.e(TAG, "onPageFinished : $url")

            //[1] 앱 최초 기동 유무를 확인 -> MainActivity. stopAnimation() 에서 처리
            // 웹뷰의 RAM과 영구 저장소 사이에 쿠키 강제 동기화 수행 함.
            CookieManager.getInstance().flush()
        }
    }

    class MyWebChromeClient : WebChromeClient() {

    }


    private fun setUserAgent(settings: WebSettings?) {
        if (settings == null || mContext == null) return
        try {
            // UserAgent를 설정한다.
            settings.userAgentString += " [SKApp/Android]"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private class AndroidScriptBridge(webView: BaseWebView) {
        //private final Handler handler = new Handler();

        var bPushEnable = false
        var bAdEnable = false

        init {
            mWebView = webView
        }


        //바코드 기능
//        @JavascriptInterface
//        fun openBarcodeScanner(callMethod: String) {
//            mBarcodeCallMethod = callMethod
//
//            mWebView.post(Runnable {
//                Log.e(TAG, "openBarcodeScanner('$callMethod')")
//                (mContext as MainActivity).mBarcodeLauncher.launch(ScanOptions())
//            })
//        }
//
//        @JavascriptInterface
//        fun requestFCMToken(callbackMethod: String) {
//            mTokenCallMethod = callbackMethod
//
//            mWebView.post(Runnable {
//                Log.e(TAG, "requestFCMToken('$callbackMethod')")
//                (mContext as MainActivity).requestFCMToken(callbackMethod)
//            })
//        }


    }
}
