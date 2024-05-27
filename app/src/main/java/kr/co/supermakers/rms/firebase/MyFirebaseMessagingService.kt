package kr.co.supermakers.rms.firebase

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kr.co.supermakers.rms.BaseWebView
import kr.co.supermakers.rms.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val KEY_TOKEN = "token"

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.e(KEY_TOKEN, "token : $token")
        sendRegistrationToServer(token)
    }

//    FCM에서 메시지를 수신할 때 호출되는 콜백
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(KEY_TOKEN, "From: ${remoteMessage.from}")

        //데이터를 저장할 해시맵 생성
        val dataMap = hashMapOf<String, String?>()

        //메시지와 제목을 추출해 해시맵에 저장
        with(remoteMessage.data) {
            Log.d(KEY_TOKEN, "Message data payload : ${remoteMessage.data}")
            dataMap.put("url", getOrElse("url") { "" })
        }
        remoteMessage.notification?.run {
            Log.d(KEY_TOKEN, "Message Notification: $body")
            dataMap.put("title", title)
            dataMap.put("msg", body)
        }

        //로그 출력 후, 알림 표시 메서드 호출
        Log.d(KEY_TOKEN, dataMap.toString())
        sendNotification(dataMap)
    }

    //받은 데이터를 바탕으로 푸시 알림을 생성
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun sendNotification(dataMap: HashMap<String, String?>) {
        //메시지나 제목이 비어있는 경우, 알림 생성하지 않고 종료
        if ((dataMap["msg"].isNullOrEmpty() || dataMap["title"].isNullOrEmpty())) return

        // 클릭 시 열릴 인텐트 생성
        val intent = Intent(this, BaseWebView::class.java)
        dataMap["url"]?.let { if (it.trim().isNotEmpty()) intent.putExtra("url", it) }

        // PendingIntent 에 넣기 전에 Flag를 추가
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)


        // PendingIntent 설정
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

        // NotificationManager 가져오기
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val channelId = "skscms"

        // NotificationCompat.Builder를 사용하여 알림 설정
        val build = NotificationCompat.Builder(this, channelId).apply {
            setContentIntent(contentIntent)
            setContentTitle(dataMap["title"])
            setSmallIcon(R.mipmap.ic_launcher)
            setLargeIcon(largeIcon)
            setAutoCancel(true)
            setTicker(dataMap["title"])
            setContentText(dataMap["msg"])
        }


        // 안드로이드 버전에 따라 알림 채널 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "skscms"
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        //알림 표시
        val notify = build.build()
        notify.flags = notify.flags or Notification.FLAG_AUTO_CANCEL
        notificationManager.notify(1, notify)
    }

    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
    }

}