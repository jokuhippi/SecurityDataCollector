package com.securitydata.collector;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "SecurityDataCollector";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button collectButton = (Button) findViewById(R.id.collect_security_data_button);
        collectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collectSecurityData();
                Snackbar.make(findViewById(R.id.main), "Data collection done.",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void collectSecurityData() {
        // TODO collect other data and print to logcat
        collectAccessibilityApps();
        collectNotificationListenerApps();
        collectDefaultSmsApp();
    }

    /**
     * Prints accessibility apps to logcat in JSON format.
     */
    private void collectAccessibilityApps() {
        List<String> accessibilityApps = getAccessibilityApps(false);
        List<String> enabledAccessibilityApps = getAccessibilityApps(true);

        try {
            JSONObject appsObject = new JSONObject();
            JSONArray appsArray = new JSONArray();
            for (String app : accessibilityApps) {
                appsArray.put(app);
            }
            appsObject.put("allAccessibilityApps", appsArray);
            Log.i(LOG_TAG, appsObject.toString());

            appsObject = new JSONObject();
            appsArray = new JSONArray();
            for (String app : enabledAccessibilityApps) {
                appsArray.put(app);
            }
            appsObject.put("enabledAccessibilityApps", appsArray);
            Log.i(LOG_TAG, appsObject.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }
    }

    /**
     * Prints package name of default SMS app to logcat in JSON format.
     */
    private void collectDefaultSmsApp() {
        String defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(getApplicationContext());
        if(defaultSmsApp == null) {
            defaultSmsApp = "";
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("defaultSmsApp", defaultSmsApp);
            Log.i(LOG_TAG, jsonObject.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }
    }

    /**
     * Prints notification listener apps to logcat in JSON format.
     */
    private void collectNotificationListenerApps() {
        List<String> accessibilityApps = getNotificationListenerApps();
        try {
            JSONObject appsObject = new JSONObject();
            JSONArray appsArray = new JSONArray();
            for (String app : accessibilityApps) {
                appsArray.put(app);
            }
            appsObject.put("notificationListenerApps", appsArray);
            Log.i(LOG_TAG, appsObject.toString());
        } catch (JSONException e) {
            Log.e(LOG_TAG, "error: " + e.getMessage());
        }
    }

    /**
     * Returns package names of accessibility apps installed on device.
     * @param enabledOnly When true, only enabled accessibility apps are returned.
     *                    When false, all accessibility apps are returned (also disabled ones).
     * @return List of accessibility app package names.
     */
    private List<String> getAccessibilityApps(boolean enabledOnly) {
        ArrayList<String> accessibilityApps = new ArrayList<>();
        AccessibilityManager am = getApplicationContext().getSystemService(
                AccessibilityManager.class);
        List<AccessibilityServiceInfo> infos;
        if(enabledOnly) {
            infos = am.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        } else {
            infos = am.getInstalledAccessibilityServiceList();
        }
        for(AccessibilityServiceInfo info : infos) {
            // Same app may use many accessibility features, but we only want to list it once.
            if(!accessibilityApps.contains(info.getResolveInfo().serviceInfo.packageName)) {
                accessibilityApps.add(info.getResolveInfo().serviceInfo.packageName);
            }
        }
        return accessibilityApps;
    }

    /**
     * Returns package names of apps that can be allowed to access notifications in device settings.
     * This method only checks apps that have defined NotificationListenerService and
     * associated permission in their manifest, so returned list also contains apps that have
     * not yet been given access to notifications.
     * @return List of package names
     */
    private List<String> getNotificationListenerApps() {
        ArrayList<String> apps = new ArrayList<>();
        PackageManager pm = getApplicationContext().getPackageManager();
        Intent intent = new Intent("android.service.notification.NotificationListenerService");
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent,
                PackageManager.GET_META_DATA);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if(resolveInfo.serviceInfo != null) {
                String permission = resolveInfo.serviceInfo.permission;
                if (permission.equals("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")) {
                    apps.add(resolveInfo.serviceInfo.packageName);
                }
            }
        }
        return apps;
    }
}