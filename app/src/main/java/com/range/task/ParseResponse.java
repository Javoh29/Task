package com.range.task;

import com.google.gson.annotations.SerializedName;



public class ParseResponse {

    @SerializedName("success")
    private String success;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }
}
