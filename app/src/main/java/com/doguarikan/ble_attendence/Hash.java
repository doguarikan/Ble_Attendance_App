package com.doguarikan.ble_attendence;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Hash {
    @SerializedName("h")
    @Expose
    private String hash_code;

    public String getHash_code() {
        return hash_code;
    }

    public void setHash_code(String hash_code) {
        this.hash_code = hash_code;
    }
}
