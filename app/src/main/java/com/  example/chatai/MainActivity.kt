package com.example.chatai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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

    private val modelFile by lazy {
        File(filesDir, "model.task")
    }

    // 색상
    private val backgroundColor = Color.parseColor("#FFFFFF")
    private val inputColor = Color.parseColor("#F3F3F3")
    private val aiBubbleColor = Color.parseColor("#F1F1F1")
    private val userBubbleColor = Color.parseColor("#E6F0FF")

    private val textColor = Color.parseColor("#202124")
    private val subTextColor = Color.parseColor("#6E6E73")
    private val blueColor = Color.parseColor("#3478F6")

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        // =========================
        // 상단 헤더
        // =========================

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(textColor)
        }

        val title = TextView(this).apply {
            text = "gwanwoo AI"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(textColor)
        }

        val modelButton = TextView(this).apply {
            text = "⋮"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(textColor)

            setOnClickListener {
                openModelPicker()
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(backgroundColor)

            setPadding(
                dp(16),
                dp(10),
                dp(16),
                dp(10)
            )

            addView(
                menuButton,
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48)
                )
            )

            addView(
                title,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                )
            )

            addView(
                modelButton,
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48)
                )
            )
        }

        // =========================
        // 처음 화면 안내
        // =========================

        val welcome = TextView(this).apply {
            text = "무엇을 도와드릴까요?"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER
        }

        val description = TextView(this).apply {
            text = "gwanwoo AI에게 질문해 보세요"
            textSize = 15f
            setTextColor(subTextColor)
            gravity = Gravity.CENTER
        }

        val welcomeBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            addView(
                welcome,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                description,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            )
        }

        // =========================
        // 채팅 영역
        // =========================

        chatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(12),
                dp(16),
                dp(16)
            )
        }

        chatBox.addView(
            welcomeBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
            addView(chatBox)
        }

        // =========================
        // 모델 상태
        // =========================

        status = TextView(this).apply {
            text = "모델 파일을 선택하세요"
            textSize = 12f
            setTextColor(subTextColor)
            gravity = Gravity.CENTER
            setPadding(
                dp(16),
                dp(5),
                dp(16),
                dp(5)
            )
        }

        // =========================
        // 입력창
        // =========================

        input = EditText(this).apply {
            hint = "메시지를 입력하세요"
            textSize = 16f

            setTextColor(textColor)
            setHintTextColor(Color.parseColor("#999999"))

            background = null

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )

            maxLines = 5
        }

        // =========================
        // 보내기 버튼
        // =========================

        val send = TextView(this).apply {
            text = "➤"
            textSize = 20f

            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            background = rounded(
                blueColor,
                50
            )

            setOnClickListener {
                ask()
            }
        }

        // =========================
        // 입력창 바깥
        // =========================

        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            background = rounded(
                inputColor,
                28
            )

            setPadding(
                dp(12),
                dp(6),
                dp(8),
                dp(6)
            )

            addView(
                input,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            addView(
                send,
                LinearLayout.LayoutParams(
                    dp(46),
                    dp(46)
                )
            )
        }

        // =========================
        // 하단 입력 영역
        // =========================

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setBackgroundColor(backgroundColor)

            setPadding(
                dp(16),
                dp(6),
                dp(16),
                dp(14)
            )

            addView(
                status,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                inputContainer,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        // =========================
        // 전체 화면
        // =========================

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)

            addView(
                header,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                scroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

            addView(
                bottom,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        setContentView(root)

        // 이미 모델이 있으면 자동 로딩
        if (modelFile.exists()) {
            loadModel()
        }
    }

    // =========================
    // 모델 선택
    // =========================

    private fun openModelPicker() {

        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }

        startActivityForResult(intent, 1)
    }

    // =========================
    // 채팅 버블
    // =========================

    private fun addBubble(
        text: String,
        isUser: Boolean
    ): TextView {

        val tv = TextView(this).apply {

            this.text = text

            textSize = 15f
            setTextColor(textColor)

            setPadding(
                dp(15),
                dp(11),
                dp(15),
                dp(11)
            )

            background = rounded(
                if (isUser)
                    userBubbleColor
                else
                    aiBubbleColor,
                18
            )

            maxWidth = dp(310)

            setLineSpacing(
                dp(2).toFloat(),
                1.05f
            )
        }

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    if (isUser)
                        Gravity.END
                    else
                        Gravity.START

                topMargin = dp(8)
            }

        // 처음 안내 문구 제거
        if (chatBox.childCount > 0) {
            val first = chatBox.getChildAt(0)

            if (first !is TextView) {
                chatBox.removeView(first)
            }
        }

        chatBox.addView(tv, params)

        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }

        return tv
    }

    // =========================
    // 파일 선택 결과
    // =========================

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode != 1 ||
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return

        status.text = "모델 복사 중..."

        worker.execute {

            try {

                contentResolver
                    .openInputStream(uri)
                    ?.use { inputStream ->

                        modelFile.outputStream()
                            .use { outputStream ->

                                inputStream.copyTo(
                                    outputStream
                                )
                            }
                    }

                runOnUiThread {
                    loadModel()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    status.text =
                        "복사 실패: ${e.message}"
                }
            }
        }
    }

    // =========================
    // 모델 로딩
    // =========================

    private fun loadModel() {

        status.text = "모델 로딩 중..."

        worker.execute {

            try {

                val options =
                    LlmInference
                        .LlmInferenceOptions
                        .builder()
                        .setModelPath(
                            modelFile.absolutePath
                        )
                        .setMaxTokens(1024)
                        .build()

                llm =
                    LlmInference
                        .createFromOptions(
                            this,
                            options
                        )

                runOnUiThread {

                    status.text =
                        "● 준비 완료"

                    status.setTextColor(
                        Color.parseColor("#20A464")
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    status.text =
                        "모델 로딩 실패"

                    status.setTextColor(
                        Color.RED
                    )
                }
            }
        }
    }

    // =========================
    // 질문 보내기
    // =========================

    private fun ask() {

        val model = llm

        if (model == null) {

            status.text =
                "먼저 모델 파일을 선택하세요"

            return
        }

        val question =
            input.text
                .toString()
                .trim()

        if (question.isEmpty()) {
            return
        }

        input.setText("")

        // 사용자 메시지
        addBubble(
            question,
            true
        )

        // AI 임시 메시지
        val reply =
            addBubble(
                "생각 중...",
                false
            )

        worker.execute {

            val answer =
                try {

                    model.generateResponse(
                        question
                    )

                } catch (e: Exception) {

                    "오류: ${e.message}"
                }

            runOnUiThread {

                reply.text = answer

                scroll.post {
                    scroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }
            }
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        worker.shutdown()

        llm = null
    }
}
