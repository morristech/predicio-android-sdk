package io.predic.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import static android.content.Intent.ACTION_BOOT_COMPLETED;

public class PredicIOReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("PREDICIO","onReceive");
        
        PredicIO.getInstance().updateAAID(context);
        HttpRequest.initialize(context);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String apiKey = settings.getString("api_key", null);
        String identity = settings.getString("predicio_identity", null);

        PredicIO.getInstance().setApiKey(context, apiKey);
        PredicIO.getInstance().setIdentity(context, identity);

        Log.d("PREDICIO", intent.getAction());

        if (intent.getAction().equals(PredicIO.ACTION_TRACK_APPS)) {
            JSONObject obj = PredicIO.getInstance().getJSONObjectApps(context);
            PredicIO.getInstance().sendHttpAppsRequest(obj);
        } else if (intent.getAction().equals(PredicIO.ACTION_TRACK_IDENTITY)) {
            PredicIO.getInstance().sendHttpIdentityRequest();
        } else if (intent.getAction().equals(PredicIO.ACTION_TRACK_LOCATION)) {
            try {
                FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    PredicIO.getInstance().updateLocation(location);
                                }
                            }
                        });
            } catch(SecurityException e) {
                Log.e("PREDICIO", "Location permissions not accepted");
            }
        } else if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            String trackingLocation = settings.getString(PredicIO.ACTION_TRACK_LOCATION, null);
            String trackingApps = settings.getString(PredicIO.ACTION_TRACK_APPS, null);
            String trackingIdentity = settings.getString(PredicIO.ACTION_TRACK_IDENTITY, null);

            Log.d("PREDICIO", "REBOOT");
            Log.d("PREDICIO", "Location=" + trackingLocation);
            Log.d("PREDICIO", "Apps=" + trackingApps);
            Log.d("PREDICIO", "Identity=" + trackingIdentity);

            if (trackingApps != null && trackingApps.equals("true")) {
                PredicIO.getInstance().startTrackingApps(context);
            }

            if (trackingIdentity != null && trackingIdentity.equals("true")) {
                PredicIO.getInstance().startTrackingIdentity(context);
            }

            if (trackingLocation != null && trackingLocation.equals("true")) {
                PredicIO.getInstance().onPermissionGranted(context);
            }
        }
    }
}
