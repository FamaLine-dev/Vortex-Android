package com.vortexstore.ui;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vortexstore.R;
import com.vortexstore.api.ApiClient;
import com.vortexstore.api.WhatsAppAPI;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OTPActivity extends AppCompatActivity {
    private EditText etPhone, etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6;
    private Button btnSendOTP, btnVerifyOTP;
    private TextView tvTimer, tvResend;
    private ProgressBar progressBar;
    
    private String otpId;
    private CountDownTimer countDownTimer;
    private WhatsAppAPI whatsAppAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);
        
        whatsAppAPI = ApiClient.getClient().create(WhatsAppAPI.class);
        
        initViews();
        setupListeners();
        setupOTPInputs();
    }

    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etOTP1 = findViewById(R.id.etOTP1);
        etOTP2 = findViewById(R.id.etOTP2);
        etOTP3 = findViewById(R.id.etOTP3);
        etOTP4 = findViewById(R.id.etOTP4);
        etOTP5 = findViewById(R.id.etOTP5);
        etOTP6 = findViewById(R.id.etOTP6);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnVerifyOTP = findViewById(R.id.btnVerifyOTP);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnSendOTP.setOnClickListener(v -> sendOTP());
        btnVerifyOTP.setOnClickListener(v -> verifyOTP());
        tvResend.setOnClickListener(v -> {
            if (tvResend.isEnabled()) {
                sendOTP();
            }
        });
    }

    private void setupOTPInputs() {
        EditText[] otpInputs = {etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6};
        
        for (int i = 0; i < otpInputs.length; i++) {
            final int index = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpInputs.length - 1) {
                        otpInputs[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        otpInputs[index - 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void sendOTP() {
        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            etPhone.setError("Phone number required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendOTP.setEnabled(false);

        Call<OTPResponse> call = whatsAppAPI.sendOTP(phone);
        call.enqueue(new Callback<OTPResponse>() {
            @Override
            public void onResponse(Call<OTPResponse> call, Response<OTPResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSendOTP.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    otpId = response.body().otpId;
                    Toast.makeText(OTPActivity.this, "OTP sent to your WhatsApp", Toast.LENGTH_SHORT).show();
                    startTimer();
                    enableOTPInputs(true);
                } else {
                    Toast.makeText(OTPActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OTPResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSendOTP.setEnabled(true);
                Toast.makeText(OTPActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOTP() {
        String otp = getOTPString();
        if (otp.length() < 6) {
            Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnVerifyOTP.setEnabled(false);

        String phone = etPhone.getText().toString().trim();
        Call<VerifyResponse> call = whatsAppAPI.verifyOTP(phone, otp);
        call.enqueue(new Callback<VerifyResponse>() {
            @Override
            public void onResponse(Call<VerifyResponse> call, Response<VerifyResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnVerifyOTP.setEnabled(true);
                
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(OTPActivity.this, "OTP verified successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to login or dashboard
                    finish();
                } else {
                    Toast.makeText(OTPActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<VerifyResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnVerifyOTP.setEnabled(true);
                Toast.makeText(OTPActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getOTPString() {
        StringBuilder otp = new StringBuilder();
        EditText[] inputs = {etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6};
        for (EditText input : inputs) {
            otp.append(input.getText().toString());
        }
        return otp.toString();
    }

    private void startTimer() {
        tvResend.setEnabled(false);
        tvResend.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Resend in " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("");
                tvResend.setEnabled(true);
                tvResend.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            }
        }.start();
    }

    private void enableOTPInputs(boolean enable) {
        EditText[] inputs = {etOTP1, etOTP2, etOTP3, etOTP4, etOTP5, etOTP6};
        for (EditText input : inputs) {
            input.setEnabled(enable);
            if (enable) {
                input.requestFocus();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
