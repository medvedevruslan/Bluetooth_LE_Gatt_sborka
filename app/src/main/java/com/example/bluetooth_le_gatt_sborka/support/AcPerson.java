package com.example.bluetooth_le_gatt_sborka.support;

import java.io.Serializable;

/**
 * Этот класс содержит данные об объекте из БД:
 * <p>
 * - userCode - id-элемента,
 * </p><p>
 * - userName - имя элемента.
 * </p>
 * А также методы для оперирования с этими данными.
 */
public class AcPerson implements Serializable {
    /**
     * id-элемента
     */
    private String userCode = "";
    /**
     * имя элемента
     */
    private String userName = "";

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String code) {
        userCode = code;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String name) {
        userName = name;
    }
}

