package com.example.unis_rssol.global.util;

import java.util.UUID;

public class StoreCodeGenerator {
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}