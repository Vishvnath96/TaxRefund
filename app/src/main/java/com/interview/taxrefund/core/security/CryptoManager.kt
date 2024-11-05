package com.interview.taxrefund.core.security


interface CryptoManager {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
    fun hash(input: String): String
}