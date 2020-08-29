package com.prgpascal.qrdatatransfer.utils

data class QrMessage(
        val message: String,
        val currentQrNumber: Int,
        val totalQrNumber: Int
)