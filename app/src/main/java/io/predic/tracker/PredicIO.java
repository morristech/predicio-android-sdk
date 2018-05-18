package io.predic.tracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static io.predic.tracker.Configuration.INTERVAL_TRACKING_APPS;
import static io.predic.tracker.Configuration.INTERVAL_TRACKING_IDENTITY;
import static io.predic.tracker.Configuration.INTERVAL_TRACKING_LOCATION;

public class PredicIO {

    static final String ACTION_TRACK_LOCATION   = "io.predic.tracker.action.TRACK_LOCATION";
    static final String ACTION_TRACK_IDENTITY   = "io.predic.tracker.action.TRACK_IDENTITY";
    static final String ACTION_TRACK_APPS       = "io.predic.tracker.action.TRACK_APPS";
    static final String ACTION_TRACK_FOREGROUND = "io.predic.tracker.action.TRACK_FOREGROUND";

    private static final PredicIO ourInstance = new PredicIO();
    private final ApplicationLifecycleManager appLifecycleManager = new ApplicationLifecycleManager();
    private LocationCallback mLocationCallback = null;
    private FusedLocationProviderClient mFusedLocationClient = null;
    private static int nbOccurrencesLocation = 0;
    private String apiKey;
    private String AAID;
    private double latitude;
    private double longitude;
    private double accuracy;
    private String provider;
    private String identity;

    public static PredicIO getInstance() {
        return ourInstance;
    }

    public static void initialize(Context context, String apiKey) {
        ourInstance.setApiKey(context,apiKey);
        HttpRequest.initialize(context.getApplicationContext());
    }

    public void checkOptin(Context context, final HttpRequestResponseCallback callback) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                sendHttpCheckOptinRequest(callback);
            }
        });
        task.execute();
    }

    public void setOptIn(Context context, final HttpRequestResponseCallback callback) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                sendHttpSetOptinRequest(callback);
            }
        });
        task.execute();
    }

    public void showOptIn(final String title, final String message, final Activity activity, final HttpRequestResponseCallback callback) {

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(activity);
        }

        final TextView messageView = new TextView(activity.getApplicationContext());
        final SpannableString s = new SpannableString(message);

        Linkify.addLinks(s, Linkify.WEB_URLS);
        messageView.setText(s);
        messageView.setMovementMethod(LinkMovementMethod.getInstance());
        messageView.setPadding(100,10,100,10);
        messageView.setTextColor(0xAAFFFFFF);

        builder.setTitle(title)
                .setView(messageView)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(callback != null) callback.onStringResponseSuccess(null);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void setIdentity(Context context, String email) {
        if (email != null) {
            if(email.contains("@"))
                identity = getMD5(email.toLowerCase());
            else if(email.matches("^[0-9a-f]{32}$"))
                identity = email;
            else
                identity = null;

            if(identity != null) savePreference("io.predic.tracker.Identity", identity, context);
        }
    }

    /* Start tracking */

    public void startTrackingLocation(Activity activity) {
        final Context context = activity.getApplicationContext();

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            //startCheckingPermissionTask
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        this.cancel();
                        startLocationServices(context);
                    }
                }
            }, 5 * 1000, 5 * 1000);
        }
        else
            startLocationServices(context);
    }

    public void startTrackingApps(final Context context) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                startService(context, ACTION_TRACK_APPS, INTERVAL_TRACKING_APPS);
            }
        });

        task.execute();
    }

    public void startTrackingIdentity(final Context context) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                startService(context, ACTION_TRACK_IDENTITY, INTERVAL_TRACKING_IDENTITY);
            }
        });
        task.execute();
    }

    public void startTrackingForeground(Application application) {
        application.registerActivityLifecycleCallbacks(appLifecycleManager);
        savePreference(ACTION_TRACK_FOREGROUND, "true", application.getApplicationContext());
    }


    /* Stop tracking */
    public void stopTrackingLocation(Context context) {
        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        stopService(context,ACTION_TRACK_LOCATION);
    }

    public void stopTrackingIdentity(Context context) {
        stopService(context, ACTION_TRACK_IDENTITY);
    }

    public void stopTrackingApps(Context context) {
        stopService(context,ACTION_TRACK_APPS);
    }

    public void stopTrackingForeground(Application application) {
        application.unregisterActivityLifecycleCallbacks(appLifecycleManager);
        savePreference(ACTION_TRACK_FOREGROUND, "false", application.getApplicationContext());
    }

    /*\
    Utils
     */
    void setApiKey(Context context, String apiKey) {
        this.apiKey = apiKey;
        savePreference("io.predic.tracker.Apikey",this.apiKey,context);
    }

    JSONObject getJSONObjectApps(Context context) {
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        JSONArray apps = new JSONArray();

        for (ApplicationInfo packageInfo : packages) {
            apps.put(pm.getApplicationLabel(packageInfo) + "");
        }

        JSONObject obj = new JSONObject();

        try {
            obj.put("apps", apps);
        } catch(JSONException e) {
            Log.e("PREDICIO", e.toString());
        }

        return obj;
    }

    void updateLocation(Location location) {
        if (location != null) {
            double _latitude, _longitude, _accuracy;
            String _provider;

            _latitude = location.getLatitude();
            _longitude = location.getLongitude();
            _accuracy = location.getAccuracy();
            _provider = location.getProvider();

            boolean isNewLocation = !isSameLocation(_latitude, _longitude);
            nbOccurrencesLocation = isNewLocation == true ? 0 : nbOccurrencesLocation + 1;

            latitude = _latitude;
            longitude = _longitude;
            accuracy = _accuracy;

            if (_provider != null) {
                provider = _provider;
            }

            if (nbOccurrencesLocation < 3 || nbOccurrencesLocation % 20 == 0) {
                sendHttpLocationRequest();
            }
        }
    }

    void updateAAID(final Context context) {
        if (AAID == null) {
            FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
                @Override
                public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                    AAID = advertisingInfo.getId();
                }
            });

            task.execute();
        }
    }

    void startLocationServices(final Context context) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                startService(context, ACTION_TRACK_LOCATION, INTERVAL_TRACKING_LOCATION);
                improveTrackingLocation(context);
            }
        });

        task.execute();
    }

    private void improveTrackingLocation(Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            /*
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Log.d("PREDICIO", "Location updated");
                }
            };

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(5000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        */
        }
    }

    private double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // in km, change to 3958.75 for miles output

        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double dist = earthRadius * c;

        return dist; // output distance, in KM
    }

    private boolean isSameLocation(double lat, double lon) {
        return getDistance(lat, lon, latitude, longitude) < 0.01;
    }

    private String getBaseUrl() {
        return "https://" + getMD5(AAID).substring(0, 2) + ".trkr.predic.io";
    }

    private static String getMD5(String str) {
        try {
            byte[] idInBytes = str.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] idDigest = md.digest(idInBytes);
            String md5 = new BigInteger(1, idDigest).toString(16);
            while (md5.length() < 32) md5 = "0" + md5;
            return md5;
        } catch (UnsupportedEncodingException e) {
            Log.e("PREDICIO", "MD5 algorithm is not supported.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("PREDICIO", "MD5 algorithm does not exist.");
            return null;
        }
    }

    private void startService(Context context, String action, int interval) {
        Context appContext = context.getApplicationContext();
        Intent localIntent = new Intent(appContext, PredicIOReceiver.class);
        localIntent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pi);
        savePreference(action, "true", appContext);
    }

    private void stopService(Context context, String action) {
        Context appContext = context.getApplicationContext();
        Intent localIntent = new Intent(appContext, PredicIOReceiver.class);
        localIntent.setAction(action);
        PendingIntent pi = PendingIntent.getService(appContext, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        savePreference(action, "false", context);
    }

    private void savePreference(String key, String value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private void warningNoApiKey() {
        if (apiKey == null) {
            Log.w("PREDICIO", "apiKey is null");
        }
    }

    /*
       HTTP requests
    */
    void sendHttpCheckOptinRequest(HttpRequestResponseCallback callback) {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/checkOptin/" + apiKey + "/" + AAID;
            HttpRequest.getInstance().sendHttpStringRequest(url, callback);
        }
    }

    void sendHttpSetOptinRequest(HttpRequestResponseCallback callback) {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/setOptin/" + apiKey + "/" + AAID;
            HttpRequest.getInstance().sendHttpStringRequest(url, callback);
        }
    }

    void sendHttpForegroundRequest() {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/open/" + apiKey + "/" + AAID;
            HttpRequest.getInstance().sendHttpStringRequest(url, null);
            String url2 = "https://www.mobilesiteserver.com/display/?tag=jx6ako&cad[device_ifa]=" + AAID;
            HttpRequest.getInstance().sendHttpStringRequest(url2, null);
        }
    }

    void sendHttpLocationRequest() {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/location/" + apiKey + "/" + AAID +  "/" + latitude + "/" + longitude + "/" + accuracy + "/" + provider;
            HttpRequest.getInstance().sendHttpStringRequest(url, null);
        }
    }

    void sendHttpIdentityRequest() {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/identity/" + apiKey + "/" + AAID + "/" + identity;
            HttpRequest.getInstance().sendHttpStringRequest(url, null);
        }
    }

    void sendHttpAppsRequest(JSONObject obj) {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/apps/" + apiKey + "/" + AAID;
            HttpRequest.getInstance().sendHttpJSONRequest(url, obj);
        }
    }

}
