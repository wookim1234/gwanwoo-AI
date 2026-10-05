package com.example.chatai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var llm: LlmInference? = null
    private lateinit var status: TextView
    private lateinit var chat: TextView
    private lateinit var input: EditText
    private val modelFile by lazy { File(filesDir, "model.task") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply { textSize = 14f; text = "모델 파일을 선택하세요" }
        chat = TextView(this).apply { textSize = 16f }
        val scroll = ScrollView(this).apply { addView(chat) }
        input = EditText(this).apply { hint = "메시지 입력" }
        val send = Button(this).apply { text = "보내기" }
        val pick = Button(this).apply { text = "모델 파일 선택 (.task)" }

        val row = LinearLayout(this)
        row.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(send)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(status)
            addView(pick)
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(row)
        }
        setContentView(root)

        pick.setOnClickListener {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(i, 1)
        }
        send.setOnClickListener { ask() }

        if (modelFile.exists()) loadModel()
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
                runOnUiThread { status.text = "준비 완료" }
            } catch (e: Exception) {
                runOnUiThread { status.text = "로딩 실패: ${e.message}" }
            }
        }
    }

    private fun ask() {
        val model = llm ?: run { status.text = "먼저 모델을 로드하세요"; return }
        val q = input.text.toString().trim()
        if (q.isEmpty()) return
        input.setText("")
        chat.append("나: $q\n\n")
        worker.execute {
            val answer = try { model.generateResponse(q) } catch (e: Exception) { "오류: ${e.message}" }
            runOnUiThread { chat.append("AI: $answer\n\n") }
        }
    }
}
