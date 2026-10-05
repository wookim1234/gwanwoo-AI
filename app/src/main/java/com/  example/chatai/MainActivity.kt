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
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var llm: LlmInference? = null
    private lateinit var status: TextView
    private lateinit var chatBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private val modelFile by lazy { File(filesDir, "model.task") }

    private val colorBg = Color.parseColor("#0F1115")
    private val colorCard = Color.parseColor("#1C1F26")
    private val colorAccent = Color.parseColor("#6C8CFF")
    private val colorUser = Color.parseColor("#2F4A8A")
    private val colorAi = Color.parseColor("#262A33")
    private val colorText = Color.parseColor("#EDEFF5")
    private val colorSub = Color.parseColor("#9AA3B2")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = colorBg

        val header = TextView(this).apply {
            text = "gwanwoo AI"
            textSize = 20f
            setTextColor(colorText)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundColor(colorCard)
        }

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(colorSub)
            text = "모델 파일을 선택하세요"
            setPadding(dp(20), dp(10), dp(20), dp(6))
        }

        val pick = Button(this).apply {
            text = "모델 파일 선택 (.task)"
            isAllCaps = false
            setTextColor(colorText)
            background = rounded(colorAccent, 12)
            setOnClickListener {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(i, 1)
            }
        }

        chatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        scroll = ScrollView(this).apply {
            setBackgroundColor(colorBg)
            addView(chatBox)
        }

        input = EditText(this).apply {
            hint = "메시지를 입력하세요"
            setHintTextColor(colorSub)
            setTextColor(colorText)
            background = rounded(colorAi, 22)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val send = Button(this).apply {
            text = "보내기"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(colorAccent, 22)
            setOnClickListener { ask() }
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(12))
            setBackgroundColor(colorCard)
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(8)
            })
            addView(send)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            addView(header)
            addView(status)
            addView(pick, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(4), dp(16), dp(8))
            })
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(inputRow)
        }
        setContentView(root)

        if (modelFile.exists()) loadModel()
    }

    private fun addBubble(text: String, isUser: Boolean): TextView {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(colorText)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(if (isUser) colorUser else colorAi, 16)
            maxWidth = dp(280)
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (isUser) Gravity.END else Gravity.START
            topMargin = dp(8)
        }
        chatBox.addView(tv, lp)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        return tv
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (requestCode != 1 || resultCode != RESULT_OK) return
        status.text = "모델 복사 중..."
        worker.execute {
            contentResolver.openInputStream(uri)?.use { inp ->
                modelFile.outputStream().use { inp.copyTo(it) }
            }
            runOnUiThread { loadModel() }
        }
    }

    private fun loadModel() {
        status.text = "모델 로딩 중..."
        worker.execute {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .build()
                llm = LlmInference.createFromOptions(this, options)
                runOnUiThread { status.text = "● 준비 완료" }
            } catch (e: Exception) {
                runOnUiThread { status.text = "로딩 실패: ${e.message}" }
            }
        }
    }

    private fun ask() {
        val model = llm
        if (model == null) {
            status.text = "먼저 모델 파일을 선택하고 로드하세요"
            return
        }
        val q = input.text.toString().trim()
        if (q.isEmpty()) return
        input.setText("")
        addBubble(q, true)
        val reply = addBubble("생각 중...", false)
        worker.execute {
            val answer = try {
                model.generateResponse(q)
            } catch (e: Exception) {
                "오류: ${e.message}"
            }
            runOnUiThread {
                reply.text = answer
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }
}
