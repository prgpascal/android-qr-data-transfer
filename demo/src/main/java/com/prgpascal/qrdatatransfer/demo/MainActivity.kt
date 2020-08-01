package com.prgpascal.qrdatatransfer.demo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.prgpascal.qrdatatransfer.TransferActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val DATA_EXCHANGE_REQUEST = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        modeChooserSwitch.setOnCheckedChangeListener { _, isChecked ->
            editText.visibility = if (isChecked) View.VISIBLE else View.GONE
            setStartButtonVisibility()
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                setStartButtonVisibility()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
            }
        })

        startButton.setOnClickListener {
            startTransfer()
        }
    }

    private fun setStartButtonVisibility() {
        if (!modeChooserSwitch.isChecked) {
            startButton.isEnabled = true
        } else {
            val textString = editText.text.toString()
            startButton.isEnabled = textString.isNotEmpty() && textString.isNotBlank()
        }
    }

    private fun startTransfer() {
        val iAmTheServer = modeChooserSwitch.isChecked
        val textToTransfer = editText.text // TODO: split text
        val messages = arrayListOf("H", "hy", "lorem", "bla")

        val intent = Intent(this, TransferActivity::class.java)
        val bundle = Bundle()
        bundle.putBoolean("i_am_the_server", iAmTheServer)
        bundle.putStringArrayList("messages", messages)
        intent.putExtras(bundle)

        startActivityForResult(intent, DATA_EXCHANGE_REQUEST)
    }

    private fun setOnFinishMessage(receivedMessage: List<String>, iAmTheServer: Boolean) {
        transferResultView.text = buildString {
            if (iAmTheServer) {
                append("Sent:\n")
            } else {
                append("Received:\n")
            }

            receivedMessage.forEach {
                append(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DATA_EXCHANGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                val iAmTheServer = data?.getBooleanExtra("i_am_the_server", false) ?: false
                var messages = emptyList<String>()
                if (!iAmTheServer) {
                    // I'm the Client, so get the data
                    messages = data?.getStringArrayListExtra("messages") ?: emptyList<String>()
                }
                setOnFinishMessage(messages, iAmTheServer)
            }
        }
    }
}
