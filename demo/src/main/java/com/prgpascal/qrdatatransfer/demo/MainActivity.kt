/*
 * Copyright (C) 2016 Riccardo Leschiutta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.prgpascal.qrdatatransfer.demo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.prgpascal.qrdatatransfer.activities.ClientTransferActivity
import com.prgpascal.qrdatatransfer.activities.ServerTransferActivity
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
        val chunkedTextToTransfer = ArrayList(editText.text.chunked(40))

        val intent: Intent
        if (iAmTheServer()) {
            intent = Intent(this, ServerTransferActivity::class.java)
            val bundle = Bundle()
            bundle.putStringArrayList(ServerTransferActivity.PARAM_MESSAGES, chunkedTextToTransfer)
            intent.putExtras(bundle)
        } else {
            intent = Intent(this, ClientTransferActivity::class.java)
        }

        startActivityForResult(intent, DATA_EXCHANGE_REQUEST)
    }

    private fun iAmTheServer(): Boolean {
        return modeChooserSwitch.isChecked
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DATA_EXCHANGE_REQUEST) {
            if (resultCode == RESULT_OK) {
                val messages: List<String> = data?.getStringArrayListExtra(ServerTransferActivity.PARAM_MESSAGES)
                        ?: emptyList()
                setOnFinishMessage(messages)
            }
        }
    }

    private fun setOnFinishMessage(receivedMessage: List<String>) {
        transferResultView.text = buildString {
            append(if (iAmTheServer()) getString(R.string.sent) else getString(R.string.received)).append("\n\n")
            receivedMessage.forEach {
                append(it)
            }
        }
        transferResultView.visibility = View.VISIBLE
    }
}
