package com.hari.MedicalPlan.helper;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class Utility {
    public static String generateHash(String value) {
        String digest = DigestUtils.sha256Hex(value);
        return digest;
    }

    public static String generateRandom256BitString() {
        String generatedString = RandomStringUtils.random(64, true, true);
        return  generatedString;
    }
}
