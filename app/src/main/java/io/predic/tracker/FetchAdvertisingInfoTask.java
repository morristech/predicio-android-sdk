package io.predic.tracker;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.IOException;

class FetchAdvertisingInfoTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = FetchAdvertisingInfoTask.class.getSimpleName();

    private Context context;
    private FetchAdvertisingInfoTaskCallback callback;

    private AdvertisingIdClient.Info info;

    FetchAdvertisingInfoTask(Context context, FetchAdvertisingInfoTaskCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            info = AdvertisingIdClient.getAdvertisingIdInfo(context);
        } catch (IOException | GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
            Log.e(TAG, e.getMessage() == null ? e.toString() : e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (callback != null && info != null) {
            callback.onAdvertisingInfoTaskExecute(info);
        }
    }
}
