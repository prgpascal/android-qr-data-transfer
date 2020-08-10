package com.prgpascal.qrdatatransfer.utils

class EncodeException : Exception {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String, cause: Throwable) : super(message, cause)
}