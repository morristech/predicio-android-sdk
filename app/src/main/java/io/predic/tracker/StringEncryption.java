package io.predic.tracker;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class StringEncryption {

    static String getSHA256(String text) {
        try {
            byte[] idInBytes = text.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] idDigest = md.digest(idInBytes);
            String sha256 = new BigInteger(1, idDigest).toString(16);
            while (sha256.length() < 64) sha256 = "0" + sha256;
            return sha256;
        } catch (UnsupportedEncodingException e) {
            Log.e("PREDICIO", "SHA256 algorithm is not supported.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("PREDICIO", "SHA256 algorithm does not exist.");
            return null;
        }
    }

    static String getSHA1(String text) {
        try {
            byte[] idInBytes = text.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] idDigest = md.digest(idInBytes);
            String sha1 = new BigInteger(1, idDigest).toString(16);
            while (sha1.length() < 40) sha1 = "0" + sha1;
            return sha1;
        } catch (UnsupportedEncodingException e) {
            Log.e("PREDICIO", "SHA1 algorithm is not supported.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("PREDICIO", "SHA1 algorithm does not exist.");
            return null;
        }
    }

    static String getMD5(String str) {
        try {
            byte[] idInBytes = str.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] idDigest = md.digest(idInBytes);
            String md5 = new BigInteger(1, idDigest).toString(16);
            while (md5.length() < 32) md5 = "0" + md5;
            return md5;
        } catch (UnsupportedEncodingException e) {
            Log.e("PREDICIO", "MD5 algorithm is not supported.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("PREDICIO", "MD5 algorithm does not exist.");
            return null;
        }
    }
}