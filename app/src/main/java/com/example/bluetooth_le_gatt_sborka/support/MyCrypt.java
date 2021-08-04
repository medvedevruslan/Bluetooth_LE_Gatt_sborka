package com.example.bluetooth_le_gatt_sborka.support;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class MyCrypt {
    private static final String CRY_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "PBEWITHSHAAND256BITAES-CBC-BC";
    private static final String TAG = "MyCrypt";
    private static byte[] mIniVector;
    private static SecretKey mSecretKey = null;

    public static void initialize(String password) {
        Log.d(TAG, "initialize("+password+")");
        if (mSecretKey == null) {
            mSecretKey = generateKey(password.toCharArray(), password.toLowerCase().getBytes());
            mIniVector = new byte[16];
            int i = 0;
            while (true) {
                byte[] bArr = mIniVector;
                if (i < bArr.length) {
                    bArr[i] = 0;
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    private static SecretKey generateKey(char[] pw, byte[] salt) {
        try {
            return SecretKeyFactory.getInstance(KEY_ALGORITHM).generateSecret(new PBEKeySpec(pw, salt, 2048, 128));
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "generateKey:" + e.getMessage());
            return null;
        } catch (InvalidKeySpecException e2) {
            Log.e(TAG, "generateKey:" + e2.getMessage());
            return null;
        }
    }

    private static byte[] encrypt(byte[] src, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(CRY_ALGORITHM);
        cipher.init(1, key, new IvParameterSpec(mIniVector));
        return cipher.doFinal(src);
    }

    private static byte[] decrypt(byte[] src, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(CRY_ALGORITHM);
        cipher.init(2, key, new IvParameterSpec(mIniVector));
        return cipher.doFinal(src);
    }

    public static String encrypt(String source) {
        Log.d(TAG, "encrypt("+source+")");
        if (TextUtils.isEmpty(source)) {
            return "";
        }
        String result = "";
        try {
            result = Base64.encodeToString(encrypt(source.getBytes(), mSecretKey), 10);
        } catch (NoSuchPaddingException e) {
            Log.e(TAG, "encrypt:" + e.getMessage());
        } catch (NoSuchAlgorithmException e2) {
            Log.e(TAG, "encrypt:" + e2.getMessage());
        } catch (InvalidAlgorithmParameterException e3) {
            Log.e(TAG, "encrypt:" + e3.getMessage());
        } catch (InvalidKeyException e4) {
            Log.e(TAG, "encrypt:" + e4.getMessage());
        } catch (BadPaddingException e5) {
            Log.e(TAG, "encrypt:" + e5.getMessage());
        } catch (IllegalBlockSizeException e6) {
            Log.e(TAG, "encrypt:" + e6.getMessage());
        } catch (Exception e7) {
            Log.e(TAG, "encrypt:" + e7.getMessage());
        }
        Log.d(TAG, "encrypt: return("+ result+")");
        return result;
    }

    public static String decrypt(String source) {
        Log.d(TAG, "decrypt("+source+")");
        if (TextUtils.isEmpty(source)) {
            return "";
        }
        String result = "";
        try {
            result = new String(decrypt(Base64.decode(source, 10), mSecretKey));
        } catch (NoSuchPaddingException e) {
            Log.e(TAG, "decrypt:" + e.getMessage());
        } catch (NoSuchAlgorithmException e2) {
            Log.e(TAG, "decrypt:" + e2.getMessage());
        } catch (InvalidAlgorithmParameterException e3) {
            Log.e(TAG, "decrypt:" + e3.getMessage());
        } catch (InvalidKeyException e4) {
            Log.e(TAG, "decrypt:" + e4.getMessage());
        } catch (BadPaddingException e5) {
            Log.e(TAG, "decrypt:" + e5.getMessage());
        } catch (IllegalBlockSizeException e6) {
            Log.e(TAG, "decrypt:" + e6.getMessage());
        } catch (Exception e7) {
            Log.e(TAG, "decrypt:" + e7.getMessage());
        }
        Log.d(TAG, "decrypt: return("+result+")");
        return result;
    }
}

