package com.vortexstore.api;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface WhatsAppAPI {
    @POST("send-otp")
    Call<OTPResponse> sendOTP(@Query("phone") String phone);
    
    @POST("verify-otp")
    Call<VerifyResponse> verifyOTP(@Query("phone") String phone, @Query("code") String code);
}

class OTPResponse {
    public boolean success;
    public String message;
    public String otpId;
}

class VerifyResponse {
    public boolean success;
    public String message;
    public String token;
}
