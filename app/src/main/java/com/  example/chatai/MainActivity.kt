package com.example.chatai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val worker = Executors.newSingleThreadExecutor()

    private var llm: LlmInference? = null

    private lateinit var chatBox: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var drawer: LinearLayout
    private lateinit var historyBox: LinearLayout

    private val modelFile by lazy {
        File(filesDir, "model.task")
    }

    private val prefs by lazy {
        getSharedPreferences("gwanwoo_ai_chats", MODE_PRIVATE)
    }

    private var currentChatId = ""
    private var currentMessages = JSONArray()

    private val colorBg = Color.WHITE
    private val colorText = Color.rgb(35, 35, 35)
    private val colorSub = Color.rgb(110, 110, 110)
    private val colorUser = Color.rgb(230, 239, 255)
    private val colorAi = Color.rgb(245, 245, 245)
    private val colorLine = Color.rgb(225, 225, 225)
    private val colorButton = Color.rgb(50, 50, 50)

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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
        } else {
            status.text = "모델 파일을 선택하세요"
        }

        createNewChat()
    }

    // ==================================================
    // 화면 만들기
    // ==================================================

    private fun createUI() {

        // -----------------------------
        // 상단 메뉴 버튼
        // -----------------------------

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 28f
            setTextColor(colorText)
            gravity = Gravity.CENTER

            setOnClickListener {
                showDrawer()
            }
        }

        // -----------------------------
        // 가운데 제목
        // -----------------------------

        val title = TextView(this).apply {
            text = "gwanwoo AI"
            textSize = 20f
            setTextColor(colorText)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        // -----------------------------
        // 새 채팅 버튼
        // -----------------------------

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

            addView(
                menuButton,
                LinearLayout.LayoutParams(
                    dp(55),
                    dp(55)
                )
            )

            addView(
                title,
                LinearLayout.LayoutParams(
                    0,
                    dp(55),
                    1f
                )
            )

            addView(
                newChatButton,
                LinearLayout.LayoutParams(
                    dp(55),
                    dp(55)
                )
            )
        }

        // ==================================================
        // 모델 상태
        // ==================================================

        status = TextView(this).apply {
            text = "모델 파일을 선택하세요"
            textSize = 13f
            setTextColor(colorSub)
            gravity = Gravity.CENTER
            setPadding(
                dp(10),
                dp(3),
                dp(10),
                dp(3)
            )

            // 상태 글자를 누르면 모델 선택
            setOnClickListener {
                openModelPicker()
            }
        }

        // ==================================================
        // 채팅 영역
        // ==================================================

        chatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(16),
                dp(10),
                dp(16),
                dp(20)
            )
        }

        scroll = ScrollView(this).apply {
            setBackgroundColor(colorBg)
            isFillViewport = true
            addView(chatBox)
        }

        // ==================================================
        // 입력창
        // ==================================================

        input = EditText(this).apply {
            hint = "메시지를 입력하세요"
            textSize = 16f

            setTextColor(colorText)
            setHintTextColor(colorSub)

            background = rounded(
                Color.rgb(245, 245, 245),
                24
            )

            setPadding(
                dp(17),
                dp(12),
                dp(17),
                dp(12)
            )

            maxLines = 5
        }

        // ==================================================
        // 보내기 버튼
        // ==================================================

        val send = TextView(this).apply {
            text = "➤"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER

            background = rounded(
                Color.rgb(50, 110, 235),
                50
            )

            setOnClickListener {
                ask()
            }
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(10)
            )

            setBackgroundColor(Color.WHITE)

            addView(
                input,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    rightMargin = dp(8)
                }
            )

            addView(
                send,
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48)
                )
            )
        }

        // ==================================================
        // 메인 화면
        // ==================================================

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)

            addView(header)

            addView(
                status,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(30)
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

            addView(inputRow)
        }

        // ==================================================
        // 왼쪽 메뉴
        // ==================================================

        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)

            setPadding(
                dp(16),
                dp(25),
                dp(16),
                dp(10)
            )

            visibility = View.GONE
        }

        // 메뉴 제목
        val drawerTitle = TextView(this).apply {
            text = "gwanwoo AI"
            textSize = 22f
            setTextColor(colorText)
            setTypeface(null, Typeface.BOLD)

            setPadding(
                dp(8),
                dp(10),
                dp(8),
                dp(20)
            )
        }

        drawer.addView(drawerTitle)

        // 새 채팅
        val newChat = TextView(this).apply {
            text = "＋  새 채팅"
            textSize = 17f
            setTextColor(colorText)
            gravity = Gravity.CENTER_VERTICAL

            background = rounded(
                Color.rgb(245, 245, 245),
                12
            )

            setPadding(
                dp(15),
                0,
                dp(15),
                0
            )

            setOnClickListener {
                createNewChat()
                hideDrawer()
            }
        }

        drawer.addView(
            newChat,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                bottomMargin = dp(15)
            }
        )

        // 최근 채팅
        val historyTitle = TextView(this).apply {
            text = "최근 채팅"
            textSize = 14f
            setTextColor(colorSub)

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
        }

        drawer.addView(historyTitle)

        // 채팅 목록
        val historyScroll = ScrollView(this)

        historyBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        historyScroll.addView(historyBox)

        drawer.addView(
            historyScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // ==================================================
        // 전체 화면
        // ==================================================

        val root = FrameLayout(this)

        root.addView(
            main,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            drawer,
            FrameLayout.LayoutParams(
                dp(310),
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START
            }
        )

        setContentView(root)
    }

    // ==================================================
    // 새 채팅
    // ==================================================

    private fun createNewChat() {

        currentChatId = System.currentTimeMillis().toString()
        currentMessages = JSONArray()

        chatBox.removeAllViews()

        val welcome = TextView(this).apply {
            text = "무엇을 도와드릴까요?\n\ngwanwoo AI에게 질문해 보세요"
            textSize = 21f
            setTextColor(colorText)
            gravity = Gravity.CENTER
        }

        chatBox.addView(
            welcome,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        input.setText("")

        status.text =
            if (llm != null) {
                "● 준비 완료"
            } else {
                "모델 파일을 선택하세요"
            }

        refreshHistory()
    }

    // ==================================================
    // 말풍선
    // ==================================================

    private fun addBubble(
        text: String,
        isUser: Boolean
    ): TextView {

        val bubble = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(colorText)

            setPadding(
                dp(15),
                dp(11),
                dp(15),
                dp(11)
            )

            background = rounded(
                if (isUser) {
                    colorUser
                } else {
                    colorAi
                },
                18
            )
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.gravity =
            if (isUser) {
                Gravity.END
            } else {
                Gravity.START
            }

        params.topMargin = dp(8)

        params.leftMargin =
            if (isUser) {
                dp(45)
            } else {
                0
            }

        params.rightMargin =
            if (isUser) {
                0
            } else {
                dp(45)
            }

        chatBox.addView(
            bubble,
            params
        )

        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }

        return bubble
    }

    // ==================================================
    // 질문 보내기
    // ==================================================

    private fun ask() {

        val model = llm

        if (model == null) {
            status.text = "먼저 모델 파일을 선택하세요"
            openModelPicker()
            return
        }

        val question = input.text
            .toString()
            .trim()

        if (question.isEmpty()) {
            return
        }

        input.setText("")

        // 첫 질문이면 환영 문구 제거
        if (currentMessages.length() == 0) {
            chatBox.removeAllViews()
        }

        // 사용자 말풍선
        addBubble(
            question,
            true
        )

        // 사용자 메시지 저장
        currentMessages.put(
            JSONObject().apply {
                put("role", "user")
                put("text", question)
            }
        )

        saveCurrentChat()

        // AI 말풍선
        val reply = addBubble(
            "생각 중...",
            false
        )

        worker.execute {

            val answer = try {

                val prompt = buildPrompt()

                model.generateResponse(prompt)

            } catch (e: Exception) {

                "오류: ${e.message}"
            }

            runOnUiThread {

                reply.text = answer

                currentMessages.put(
                    JSONObject().apply {
                        put("role", "assistant")
                        put("text", answer)
                    }
                )

                saveCurrentChat()

                refreshHistory()

                scroll.post {
                    scroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }
            }
        }
    }

    // ==================================================
    // 이전 대화를 포함한 프롬프트
    // ==================================================

    private fun buildPrompt(): String {

        val prompt = StringBuilder()

        prompt.append(
            "다음은 사용자와 AI의 이전 대화이다.\n"
        )

        prompt.append(
            "이전 대화의 맥락을 참고해서 자연스럽게 답변해라.\n\n"
        )

        for (i in 0 until currentMessages.length()) {

            val message =
                currentMessages.getJSONObject(i)

            val role =
                message.getString("role")

            val text =
                message.getString("text")

            if (role == "user") {
                prompt.append("사용자: ")
            } else {
                prompt.append("AI: ")
            }

            prompt.append(text)
            prompt.append("\n")
        }

        prompt.append("\nAI:")

        return prompt.toString()
    }

    // ==================================================
    // 현재 채팅 저장
    // ==================================================

    private fun saveCurrentChat() {

        if (currentMessages.length() == 0) {
            return
        }

        val chats = JSONArray(
            prefs.getString(
                "chats",
                "[]"
            )
        )

        var found = false

        for (i in 0 until chats.length()) {

            val chat =
                chats.getJSONObject(i)

            if (
                chat.getString("id") ==
                currentChatId
            ) {

                chat.put(
                    "messages",
                    currentMessages
                )

                found = true
                break
            }
        }

        if (!found) {

            val firstMessage =
                currentMessages
                    .getJSONObject(0)
                    .getString("text")

            val title =
                if (firstMessage.length > 25) {
                    firstMessage.substring(0, 25)
                } else {
                    firstMessage
                }

            val newChat = JSONObject().apply {

                put(
                    "id",
                    currentChatId
                )

                put(
                    "title",
                    title
                )

                put(
                    "time",
                    System.currentTimeMillis()
                )

                put(
                    "messages",
                    currentMessages
                )
            }

            val newChats = JSONArray()

            // 최신 채팅을 맨 위에
            newChats.put(newChat)

            for (i in 0 until chats.length()) {
                newChats.put(
                    chats.getJSONObject(i)
                )
            }

            prefs.edit()
                .putString(
                    "chats",
                    newChats.toString()
                )
                .apply()

            return
        }

        prefs.edit()
            .putString(
                "chats",
                chats.toString()
            )
            .apply()
    }

    // ==================================================
    // 채팅 목록
    // ==================================================

    private fun refreshHistory() {

        historyBox.removeAllViews()

        val chats = JSONArray(
            prefs.getString(
                "chats",
                "[]"
            )
        )

        for (i in 0 until chats.length()) {

            val chat =
                chats.getJSONObject(i)

            val id =
                chat.getString("id")

            val title =
                chat.optString(
                    "title",
                    "새 채팅"
                )

            val row =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(5),
                        dp(2),
                        dp(2),
                        dp(2)
                    )
                }

            // 채팅 제목
            val titleView =
                TextView(this).apply {

                    text = title
                    textSize = 15f
                    setTextColor(colorText)

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setSingleLine(true)

                    ellipsize =
                        TextUtils.TruncateAt.END

                    setPadding(
                        dp(8),
                        0,
                        dp(5),
                        0
                    )

                    setOnClickListener {

                        loadChat(id)
                        hideDrawer()
                    }
                }

            row.addView(
                titleView,
                LinearLayout.LayoutParams(
                    0,
                    dp(48),
                    1f
                )
            )

            // 삭제 버튼
            val delete =
                TextView(this).apply {

                    text = "⋮"
                    textSize = 22f
                    setTextColor(colorSub)
                    gravity = Gravity.CENTER

                    setOnClickListener {
                        deleteChat(id)
                    }
                }

            row.addView(
                delete,
                LinearLayout.LayoutParams(
                    dp(40),
                    dp(48)
                )
            )

            historyBox.addView(row)

            // 구분선
            val line = View(this).apply {
                setBackgroundColor(colorLine)
            }

            historyBox.addView(
                line,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                )
            )
        }
    }

    // ==================================================
    // 이전 채팅 불러오기
    // ==================================================

    private fun loadChat(id: String) {

        val chats = JSONArray(
            prefs.getString(
                "chats",
                "[]"
            )
        )

        for (i in 0 until chats.length()) {

            val chat =
                chats.getJSONObject(i)

            if (
                chat.getString("id") == id
            ) {

                currentChatId = id

                currentMessages =
                    chat.getJSONArray(
                        "messages"
                    )

                chatBox.removeAllViews()

                for (
                    j in 0 until currentMessages.length()
                ) {

                    val message =
                        currentMessages
                            .getJSONObject(j)

                    val role =
                        message.getString(
                            "role"
                        )

                    val text =
                        message.getString(
                            "text"
                        )

                    addBubble(
                        text,
                        role == "user"
                    )
                }

                scroll.post {
                    scroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }

                return
            }
        }
    }

    // ==================================================
    // 채팅 삭제
    // ==================================================

    private fun deleteChat(id: String) {

        val oldChats = JSONArray(
            prefs.getString(
                "chats",
                "[]"
            )
        )

        val newChats = JSONArray()

        for (i in 0 until oldChats.length()) {

            val chat =
                oldChats.getJSONObject(i)

            if (
                chat.getString("id") != id
            ) {
                newChats.put(chat)
            }
        }

        prefs.edit()
            .putString(
                "chats",
                newChats.toString()
            )
            .apply()

        if (currentChatId == id) {
            createNewChat()
        } else {
            refreshHistory()
        }
    }

    // ==================================================
    // 메뉴 열기
    // ==================================================

    private fun showDrawer() {
        refreshHistory()
        drawer.visibility = View.VISIBLE
    }

    private fun hideDrawer() {
        drawer.visibility = View.GONE
    }

    // ==================================================
    // 모델 선택
    // ==================================================

    private fun openModelPicker() {

        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

                type = "*/*"
            }

        startActivityForResult(
            intent,
            1
        )
    }

    // ==================================================
    // 모델 선택 결과
    // ==================================================

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

                        modelFile
                            .outputStream()
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
                        "모델 복사 실패: ${e.message}"
                }
            }
        }
    }

    // ==================================================
    // 모델 로딩
    // ==================================================

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
                }

            } catch (e: Exception) {

                runOnUiThread {

                    status.text =
                        "로딩 실패: ${e.message}"
                }
            }
        }
    }

    // ==================================================
    // 앱 종료
    // ==================================================

    override fun onDestroy() {

        worker.shutdown()

        super.onDestroy()
    }
}
