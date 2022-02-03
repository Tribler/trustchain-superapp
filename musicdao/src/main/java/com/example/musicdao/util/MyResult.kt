package com.example.musicdao.util

sealed class MyResult<T> {
    data class Success<T>(val value: T) : MyResult<T>()
    data class Failure<T>(val message: String?) : MyResult<T>()
}
