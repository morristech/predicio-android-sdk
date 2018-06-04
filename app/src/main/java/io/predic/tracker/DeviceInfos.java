package io.predic.tracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;

class DeviceInfos {

    private Context context;

    private String manufacturer;
    private String model;
    private String OS;
    private String OSVersion;
    private boolean isCharging;
    private String carrierName;
    private String wifiSSID;
    private String wifiBSSID;
    private String connectionType;
    private Intent batteryIntent;

    DeviceInfos(Context context)
    {
        this.context = context;
        batteryIntent = context.registerReceiver(
    null,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        );
        this.manufacturer = Build.MANUFACTURER;
        this.model = Build.MODEL;
        this.OS = "Android";
        this.OSVersion = Build.VERSION.RELEASE;
    }

    String getParams()
    {
        updateWifiInfo();
        updateConnectionType();
        updateIsCharging();
        updateCarrierName();

        String params = "";
        params += "OS=" + OS;
        params += "&OSV=" + OSVersion;
        params += "&manufacturer=" + manufacturer;
        params += "&model=" + model;
        params += "&isChargin" + ((isCharging) ? "1" : "0");
        params += "&connectionType=" + connectionType;
        params += "&carrierName=" + carrierName;
        params += "&wifiSSID=" + wifiSSID;
        params += "&wifiBSSID=" + wifiBSSID;

        return params;
    }

    void updateWifiInfo() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            if (wifiInfo != null) {
                wifiSSID = wifiInfo.getSSID();
                wifiBSSID = wifiInfo.getBSSID();
            };
        }
    }

    void updateConnectionType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            connectionType = "none";
            return;
        }

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.isConnected() == false) {
            connectionType = "none";
            return;
        }

        int type = networkInfo.getType();
        switch (type) {
            case ConnectivityManager.TYPE_WIFI:
                connectionType = "wifi";
                break;
            case ConnectivityManager.TYPE_MOBILE:
                connectionType = "cellular";
                break;
            default:
                connectionType = "unknown";
        }
    }

    void updateIsCharging() {
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);
    }

    void updateCarrierName() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null ) {
            carrierName = telephonyManager.getNetworkOperatorName();
        }
    }

}
