package com.example.leo.test

object Utils {
    fun getTag(obj: Any) = obj.javaClass.simpleName + System.identityHashCode(obj)
}
