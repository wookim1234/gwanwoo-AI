package com.example.chatai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val worker = Executors.newSingleThreadExecutor()

    private var llm: LlmInference? = null

    private lateinit var chatBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var drawer: LinearLayout

    private val modelFile by lazy {
        File(filesDir, "model.task")
    }

    private val prefs by lazy {
        getSharedPreferences("chat_history", MODE_PRIVATE)
    }

    private var currentChatId: String = ""
    private var currentMessages = JSONArray()

    private val colorBg = Color.WHITE
    private val colorText = Color.rgb(30, 30, 30)
    private val colorSub = Color.rgb(110, 110, 110)
    private val colorUser = Color.rgb(230, 238, 255)
    private val colorAi = Color.rgb(245, 245, 245)
    private val colorLine = Color.rgb(225, 225, 225)

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }

    private fun rounded(
        color: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        createUI()

        if (modelFile.exists()) {
            loadModel()
        }

        createNewChat()
    }

    // --------------------------------------------------
    // UI
    // --------------------------------------------------

    private fun createUI() {

        // ---------- 상단바 ----------

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 25f
            setTextColor(colorText)
            gravity = Gravity.CENTER
            setOnClickListener {
                showDrawer()
            }
        }

        val title = TextView(this).apply {
            text = "gwanwoo AI"
            textSize = 19f
            setTextColor(colorText)
            gravity = Gravity.CENTER
        }

        val newChatButton = TextView(this).apply {
            text = "＋"
            textSize = 28f
            setTextColor(colorText)
            gravity = Gravity.CENTER
            setOnClickListener {
                createNewChat()
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(8))

            addView(
                menuButton,
                LinearLayout.LayoutParams(
                    dp(52),
                    dp(52)
                )
            )

            addView(
                title,
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f
                )
            )

            addView(
                newChatButton,
                LinearLayout.LayoutParams(
                    dp(52),
                    dp(52)
                )
            )
        }

        // ---------- 상태 ----------

        status = TextView(this).apply {
            text = "모델 파일을 선택하세요"
            textSize = 12f
            setTextColor(colorSub)
            gravity = Gravity.CENTER
            setPadding(
                dp(10),
                dp(2),
                dp(10),
                dp(2)
            )
        }

        // ---------- 채팅 영역 ----------

        chatBox = LinearLayout
