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
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.VolleyError;
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
    private static final PredicIO ourInstance = new PredicIO();
    private String apiKey;
    private String AAID;

    private final ApplicationLifecycleManager appLifecycleManager = new ApplicationLifecycleManager();

    static final String ACTION_TRACK_LOCATION = "io.predic.tracker.action.TRACK_LOCATION";
    static final String ACTION_TRACK_IDENTITY = "io.predic.tracker.action.TRACK_IDENTITY";
    static final String ACTION_TRACK_APPS = "io.predic.tracker.action.TRACK_APPS";
    static final String ACTION_TRACK_FOREGROUND = "io.predic.tracker.action.TRACK_FOREGROUND";

    private double latitude;
    private double longitude;
    private double accuracy;
    private String provider;

    private LocationCallback mLocationCallback = null;
    private FusedLocationProviderClient mFusedLocationClient = null;

    private String identity = null;

    private static int nbOccurrencesLocation = 0;

    public static PredicIO getInstance() {
        return ourInstance;
    }

    public static void initialize(Context context, String apiKey) {

        ourInstance.setApiKey(context,apiKey);
        HttpRequest.initialize(context.getApplicationContext());
    }

    public void checkOptin(Activity activity, final HttpRequestResponseCallback callback) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(activity.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                sendHttpCheckOptinRequest(callback);
            }
        });

        task.execute();
    }

    public void setOptIn(Activity activity, final HttpRequestResponseCallback callback) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(activity.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                sendHttpSetOptinRequest(callback);
            }
        });

        task.execute();
    }

    public void improveTrackingLocation(Context context) {
        Log.d("PREDICIO","improveTrackingLocation");
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Log.d("PREDICIO", "Location updated");
                }
            };

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(60000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }

    public void showOptIn(final String title, final String message, final Activity activity, final HttpRequestResponseCallback callback) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(activity.getApplicationContext(), new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();

                checkOptin(activity, new HttpRequestResponseCallback() {
                    @Override
                    public void onStringResponseSuccess(String response) {
                        if (response.equals("KO")) {
                            showDialog(title, message, activity, callback);
                        }
                    }

                    @Override
                    public void onError(VolleyError e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });
            }
        });

        task.execute();
    }

    public void setIdentity(Context context, String email) {
        if (email != null) {
            
            if(email.matches("^[0-9a-f]{32}$"))
            {
                identity = email;
            }
            else if(email.contains("@"))
            {
                identity = getMD5(email.toLowerCase());
            }
            else
            {
                identity = null;
            }

            if(identity != null)
            {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("predicio_identity", identity);
                editor.commit();
            }
        }
    }

    public String getIdentity() {
        return identity;
    }

    /* Start tracking */

    public void startTrackingLocation(Activity activity) {
        final Context context = activity.getApplicationContext();

        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        //checkingPermission
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    this.cancel();

                    //launch services
                    FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
                        @Override
                        public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                        AAID = advertisingInfo.getId();
                        startService(context, ACTION_TRACK_LOCATION, INTERVAL_TRACKING_LOCATION);
                        improveTrackingLocation(context);
                        }
                    });
                    task.execute();

                }
            }
        }, 5 * 1000, 5 * 1000);
    }

    public void startTrackingApps(final Context context) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
            @Override
            public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                AAID = advertisingInfo.getId();
                startService(context, ACTION_TRACK_APPS, INTERVAL_TRACKING_APPS);
            }
        });

        task.execute();
    }

    public void startTrackingIdentity(final Context context) {
        FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
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
        Intent it = new Intent(context, PredicIOReceiver.class);
        it.setAction(ACTION_TRACK_LOCATION);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);

        if (mFusedLocationClient != null && mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        savePreference(ACTION_TRACK_APPS, "false", context);
    }

    public void stopTrackingIdentity(Context context) {
        Intent it = new Intent(context, PredicIOReceiver.class);
        it.setAction(ACTION_TRACK_IDENTITY);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        savePreference(ACTION_TRACK_APPS, "false", context);
    }

    public void stopTrackingApps(Context context) {
        Intent it = new Intent(context, PredicIOReceiver.class);
        it.setAction(ACTION_TRACK_APPS);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        savePreference(ACTION_TRACK_APPS, "false", context);
    }

    public void stopTrackingForeground(Application application) {
        application.unregisterActivityLifecycleCallbacks(appLifecycleManager);
        savePreference(ACTION_TRACK_FOREGROUND, "false", application.getApplicationContext());
    }

    /*\
    Utils
     */
    private boolean isSameLocation(double lat, double lon) {
        return GeoUtils.distance(lat, lon, latitude, longitude) < 0.01;
    }

    private String getBaseUrl() {
        return "https://" + getKID() + ".trkr.predic.io";
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
            FetchAdvertisingInfoTask task = new FetchAdvertisingInfoTask(context, new FetchAdvertisingInfoTaskCallback() {
                @Override
                public void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo) {
                    AAID = advertisingInfo.getId();
                }
            });

            task.execute();
        }
    }

    private static String getMD5(String str) {
        try {
            byte[] idInBytes = str.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] idDigest = md.digest(idInBytes);
            return new BigInteger(1, idDigest).toString(16);
        } catch (UnsupportedEncodingException e) {
            Log.e("PREDICIO", "MD5 algorithm is not supported.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e("PREDICIO", "MD5 algorithm does not exist.");
            return null;
        }
    }

    void setApiKey(Context context, String apiKey) {
        this.apiKey = apiKey;
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("api_key", this.apiKey);
        editor.commit();
    }

    String getKID() {
        return getMD5(AAID).substring(0, 2);
    }

    private void startService(Context context, String action, int interval) {
        Context appContext = context.getApplicationContext();
        Intent localIntent = new Intent(appContext, PredicIOReceiver.class);
        localIntent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(appContext, 0, localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pi);
        savePreference(action, "true", context);
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

    private void showDialog(String title, String message, final Activity activity, final HttpRequestResponseCallback callback) {

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
                        setOptIn(activity, callback);
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

    void savePreference(String key, String value, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /*
    HTTP requests
     */
    void warningNoApiKey() {
        if (apiKey == null) {
            Log.w("PREDICIO", "apiKey is null");
        }
    }

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
