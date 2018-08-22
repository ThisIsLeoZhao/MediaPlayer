package com.example.leo.test;

public class Utils {
    public static String getTag(Object object) {
        return object.getClass().getSimpleName() + System.identityHashCode(object);
    }
}
