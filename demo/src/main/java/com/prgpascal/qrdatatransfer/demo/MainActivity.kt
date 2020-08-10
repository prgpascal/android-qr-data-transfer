package com.prgpascal.qrdatatransfer.demo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.prgpascal.qrdatatransfer.activities.TransferActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val DATA_EXCHANGE_REQUEST = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        modeChooserSwitch.setOnCheckedChangeListener { _, _ ->
            setEditTextVisibility()
            setStartButtonEnabledOrNot()
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                setStartButtonEnabledOrNot()
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

    private fun setEditTextVisibility() {
        val isChecked = modeChooserSwitch.isChecked
        editText.visibility = if (isChecked) View.VISIBLE else View.GONE
    }

    private fun setStartButtonEnabledOrNot() {
        if (!modeChooserSwitch.isChecked) {
            startButton.isEnabled = true
        } else {
            val textString = editText.text.toString()
            startButton.isEnabled = textString.isNotEmpty() && textString.isNotBlank()
        }
    }

    private fun startTransfer() {
        val iAmTheServer = modeChooserSwitch.isChecked
        val chunkedTextToTransfer = ArrayList(editText.text.chunked(40))

        val intent = Intent(this, TransferActivity::class.java)
        val bundle = Bundle()
        bundle.putBoolean(TransferActivity.PARAM_I_AM_THE_SERVER, iAmTheServer)
        bundle.putStringArrayList(TransferActivity.PARAM_MESSAGES, chunkedTextToTransfer)
        intent.putExtras(bundle)

        startActivityForResult(intent, DATA_EXCHANGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DATA_EXCHANGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                val iAmTheServer = data?.getBooleanExtra(TransferActivity.PARAM_I_AM_THE_SERVER, false)
                        ?: false
                var messages = emptyList<String>()
                if (!iAmTheServer) {
                    messages = data?.getStringArrayListExtra(TransferActivity.PARAM_MESSAGES)
                            ?: emptyList()
                }
                setOnFinishMessage(messages, iAmTheServer)
            }
        }
    }

    private fun setOnFinishMessage(receivedMessage: List<String>, iAmTheServer: Boolean) {
        transferResultView.text = buildString {
            append(if (iAmTheServer) getString(R.string.sent) else getString(R.string.received)).append("\n\n")
            receivedMessage.forEach {
                append(it)
            }
        }
        transferResultView.visibility = View.VISIBLE
    }
}
