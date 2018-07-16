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

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static io.predic.tracker.Configuration.INTERVAL_TRACKING_APPS;
import static io.predic.tracker.Configuration.INTERVAL_TRACKING_IDENTITY;
import static io.predic.tracker.Configuration.INTERVAL_TRACKING_LOCATION;

public class PredicIO {

    static final String ACTION_TRACK_LOCATION           = "io.predic.tracker.action.TRACK_LOCATION";
    static final String ACTION_TRACK_IDENTITY           = "io.predic.tracker.action.TRACK_IDENTITY";
    static final String ACTION_TRACK_APPS               = "io.predic.tracker.action.TRACK_APPS";
    static final String ACTION_TRACK_FOREGROUND         = "io.predic.tracker.action.TRACK_FOREGROUND";

    public static final String LOCATION_FINE = "Fine";
    public static final String LOCATION_COARSE = "Coarse";

    private static final PredicIO ourInstance = new PredicIO();
    private final ApplicationLifecycleManager appLifecycleManager = new ApplicationLifecycleManager();
    private LocationCallback mLocationCallback = null;
    private FusedLocationProviderClient mFusedLocationClient = null;
    private static int nbOccurrencesLocation = 0;
    private static long intervalTracking = 60000;
    private Context context = null;
    private String apiKey;
    private String AAID;
    private double latitude;
    private double longitude;
    private double accuracy;
    private double altitude;
    private String provider;
    private String identity;
    private String locationAccuracyMethod;
    private int minMoveDistance =  10;
    private Pixel pixel;
    private int nbRunningActivities = 0;
    private DeviceInfos deviceInfos;
    private boolean webviewState;

    public static PredicIO getInstance() {
        return ourInstance;
    }

    public static void initialize(Context context, String apiKey) {
        initialize(context,apiKey,true);
    }

    public static void initialize(Context context, String apiKey, boolean webviewState) {

        if(context == null)
        {
            Log.e("PREDICIO","Initialized failed, context is null");
            return;
        }

        ourInstance.setContext(context.getApplicationContext());
        ourInstance.setApiKey(apiKey);
        ourInstance.setWebView(webviewState);
        HttpRequest.initialize(context.getApplicationContext());
    }

    public void checkOptIn(final HttpRequestResponseCallback callback) {
        if(context == null) return;
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                sendHttpCheckOptinRequest(callback);
            }
        });
        task.execute();
    }

    public void setOptIn(final HttpRequestResponseCallback callback) {
        if(context == null) return;
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                sendHttpSetOptinRequest(callback);
            }
        });
        task.execute();
    }

    public void showOptIn(final String title, final String message, final Activity activity, final HttpRequestResponseCallback callback) {
        if(context == null || activity == null) return;
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(activity);
        }

        final TextView messageView = new TextView(context);
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

    public void setIdentity(String email) {
        if(context == null) return;
        if (email != null && email.contains("@")) {
            identity = email;
            savePreference("io.predic.tracker.Identity", email);
        }
    }

    /* Start tracking */
    public void startTrackingLocation(final Activity activity) {
        this.startTrackingLocation(activity, LOCATION_FINE);
    }

    public void startTrackingLocation(final Activity activity, String accuracyMethod) {
        if(context == null) return;
        try {
            if (activity != null && webviewState == true && pixel == null) pixel = new Pixel(activity);
        }
        catch (Exception e) { }

        if(deviceInfos == null) deviceInfos = new DeviceInfos(context);

        setLocationAccuracy(accuracyMethod);

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if(activity != null) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        1
                );
                //startCheckingPermissionTask
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            this.cancel();
                            startLocationServices();
                        }
                    }
                }, 5 * 1000, 5 * 1000);
            }
        }
        else {
            startLocationServices();
        }
    }

    public void startTrackingApps() {
        if(context == null) return;
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
            AAID = advertisingInfo.getId();
            startService(ACTION_TRACK_APPS, INTERVAL_TRACKING_APPS);
            }
        });
        task.execute();
    }

    public void startTrackingIdentity() {
        if(context == null) return;
        if(identity != null) {
            FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
                @Override
                public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                    AAID = advertisingInfo.getId();
                    startService(ACTION_TRACK_IDENTITY, INTERVAL_TRACKING_IDENTITY);
                }
            });
            task.execute();
        }
        else {
            Log.e("PREDICIO","Fail to launch startTrackingIdentity, You are trying to tracking Identity but identity is not set");
        }
    }

    public void startTrackingForeground(Activity activity) {
        if(context == null) return;
        try {
            if (activity != null && webviewState == true && pixel == null) pixel = new Pixel(activity);
        }
        catch (Exception e) { }

        if(deviceInfos == null) deviceInfos = new DeviceInfos(context);

        onActivityResumed();

        if(activity != null) activity.getApplication().registerActivityLifecycleCallbacks(appLifecycleManager);
        savePreference(ACTION_TRACK_FOREGROUND, "true");
    }

    /* Stop tracking */
    public void stopTrackingLocation() {
        if(context == null) return;
        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        stopService(ACTION_TRACK_LOCATION);
    }

    public void stopTrackingIdentity() {
        if(context == null) return;
        stopService(ACTION_TRACK_IDENTITY);
    }

    public void stopTrackingApps() {
        if(context == null) return;
        stopService(ACTION_TRACK_APPS);
    }

    public void stopTrackingForeground(Application application) {
        if(context == null) return;
        application.unregisterActivityLifecycleCallbacks(appLifecycleManager);
        savePreference(ACTION_TRACK_FOREGROUND, "false");
    }

    /* Utils */
    void onActivityResumed() {
        nbRunningActivities++;
        if (nbRunningActivities == 1) {
            FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
                @Override
                public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                sendHttpForegroundRequest();
                }
            });
            task.execute();
        }
    }

    void onActivityStopped() {
        if(nbRunningActivities > 0) {
            nbRunningActivities = 0;
            sendHttpBackgroundRequest();
        }
    }

    void setLocationAccuracy(String accuracyMethod) {
        locationAccuracyMethod = (accuracyMethod == null) ? LOCATION_FINE : accuracyMethod;
        savePreference("io.predic.tracker.locationAccuracyMethod", locationAccuracyMethod);
    }

    void setContext(Context context) {
        this.context = context;
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        savePreference("io.predic.tracker.Apikey",this.apiKey);
    }

    void setWebView(boolean webviewState){
        this.webviewState = webviewState;
    }

    JSONObject getJSONObjectApps() {
        final PackageManager pm = context.getPackageManager();

        List<ApplicationInfo> packages;
        try {
            packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        } catch(Exception e) {
            return null;
        }
        JSONArray apps = new JSONArray();

        for (ApplicationInfo applicationInfo : packages) {
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) continue;
            try {
                JSONObject obj = new JSONObject();
                obj.put("package", applicationInfo.packageName);
                obj.put("name", pm.getApplicationLabel(applicationInfo));
                obj.put("time",new File(applicationInfo.sourceDir).lastModified());
                apps.put(obj);
            } catch(Exception e) { }
        }

        JSONObject obj = new JSONObject();
        try {
            obj.put("apps", apps);
        } catch(JSONException e) {
            Log.e("PREDICIO", e.toString());
        }

        return obj;
    }

    void receiveLocation(Location location) {
        if (location != null) {
            double _latitude, _longitude;

            _latitude = location.getLatitude();
            _longitude = location.getLongitude();

            boolean move = (getDistance(_latitude,_longitude,latitude,longitude) > (minMoveDistance/1000));
            nbOccurrencesLocation = (move) ? 0 : nbOccurrencesLocation + 1;

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            accuracy = location.getAccuracy();
            provider = location.getProvider();
            altitude = location.getAltitude();

            String priority = locationAccuracyMethod;

            long newIntervalTracking;
            if(nbOccurrencesLocation < 5) {
                newIntervalTracking = 60000;
                minMoveDistance = 10;
            }
            else if(nbOccurrencesLocation < 30) {
                newIntervalTracking = 3 * 60000;
                minMoveDistance = 100;
                priority = LOCATION_COARSE;
            }
            else {
                newIntervalTracking = 10 * 60000;
                minMoveDistance = 200;
                priority = LOCATION_COARSE;
            }

            int intervalRequest = nbOccurrencesLocation < 15 ? 5 : 15;

            if(newIntervalTracking != intervalTracking) {
                intervalTracking = newIntervalTracking;
                improveTrackingLocation(intervalTracking, priority);
            }

            if (nbOccurrencesLocation < 3 || nbOccurrencesLocation % intervalRequest == 0) {
                sendHttpLocationRequest();
            }
        }
    }

    void updateAAID() {
        if (AAID == null) {
            FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
                @Override
                public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                    AAID = advertisingInfo.getId();
                }
            });

            task.execute();
        }
    }

    void startLocationServices() {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                startService(ACTION_TRACK_LOCATION, INTERVAL_TRACKING_LOCATION);
                improveTrackingLocation(60000, locationAccuracyMethod);
            }
        });
        task.execute();
    }

    private void improveTrackingLocation(long interval, String priority) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {

            if (mFusedLocationClient != null && mLocationCallback != null) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
            if( mLocationCallback == null) {
                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                    Log.d("PREDICIO", "Location updated");
                    }
                };
            }
            if(mFusedLocationClient == null) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            }

            Log.d("PREDICIO","Location interval: "+interval + ", minMoveDistance: "+ minMoveDistance + ", Location method: " + priority);

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(interval);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority((priority.equals(LOCATION_FINE)) ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

        }
    }

    private double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371; // in km, change to 3958.75 for miles output

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);

        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double dist = earthRadius * c;

        return dist; // output distance, in KM
    }

    private String getBaseUrl() {
        return "https://" + StringEncryption.getMD5(AAID).substring(0, 2) + ".trkr.predic.io/sdk/2.0";
    }

    private void startService(String action, int interval) {
        Intent localIntent = new Intent(context, PredicIOReceiver.class);
        localIntent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pi);
        savePreference(action, "true");
    }

    private void stopService(String action) {
        Intent localIntent = new Intent(context, PredicIOReceiver.class);
        localIntent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        savePreference(action, "false");
    }

    private void savePreference(String key, String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private void warningNoApiKey() {
        if (apiKey == null) {
            Log.w("PREDICIO", "apiKey is null");
        }
    }

    /* HTTP requests */
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

            String deviceParams = (deviceInfos != null) ? "?" + deviceInfos.getParams() : "";

            String url = getBaseUrl() + "/open/" + apiKey + "/" + AAID + deviceParams;

            try {
                pixel.shoot(url);
            }
            catch(Exception e) {
                HttpRequest.getInstance().sendHttpStringRequest(url, null);
            }
        }
    }

    void sendHttpBackgroundRequest() {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {
            String url = getBaseUrl() + "/close/" + apiKey + "/" + AAID;
            HttpRequest.getInstance().sendHttpStringRequest(url, null);
        }
    }

    void sendHttpLocationRequest() {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null) {

            String deviceParams = (deviceInfos != null) ? "?" + deviceInfos.getParams() : "";

            String url = getBaseUrl() + "/location/" + apiKey + "/" + AAID +  "/" + latitude + "/" + longitude + "/" + altitude + "/" + accuracy + "/" + provider + deviceParams;

            try {
                pixel.shoot(url);
            }
            catch(Exception e) {
                HttpRequest.getInstance().sendHttpStringRequest(url, null);
            }
        }
    }

    void sendHttpIdentityRequest() {
        this.warningNoApiKey();
        if (AAID != null && apiKey != null && identity != null) {
            String url = getBaseUrl() + "/identity/" + apiKey + "/" + AAID + "/" + StringEncryption.getMD5(identity) + "/" + StringEncryption.getSHA1(identity) + "/" + StringEncryption.getSHA256(identity);
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
