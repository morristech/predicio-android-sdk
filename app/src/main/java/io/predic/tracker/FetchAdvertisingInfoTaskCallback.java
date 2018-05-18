package io.predic.tracker;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

interface FetchAdvertisingInfoTaskCallback {
    void onAdvertisingInfoTaskExecute(AdvertisingIdClient.Info advertisingInfo);
}
