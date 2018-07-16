# SDK Android - Predicio

## Before getting started
The SDK works on all Android versions from `4.0.0` and above.

It uses the following permissions:
* ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION - Requested to access location.
* INTERNET - Requested to access Predicio services.
* ACCESS_NETWORK_STATE - Requested to get carrier information
* ACCESS_WIFI_STATE - Requested to get wifi information

## How does it work ?
Our SDK is designed to run in the app background once installed on your app.

When launching, the SDK checks the user consent then starts collecting data periodically.

It only collects data you're sharing with us.
In order to minimize battery and network usage, our SDK collects and sends data from once every twenty minutes to every minute if high location activity is noticed.

## Install

Add this code to your root `build.gradle`:
```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
    ...
  }
}
```

Add this code to your app `build.gradle`:
```gradle
dependencies {
  ...
  compile 'com.github.team-predicio:android-sdk:2.1.3'
  ...
}
```

Then, synchronise your project.

## Use of SDK

### Before getting started

Initialize the SDK with your `API_KEY` inside an `Activity`.
```
PredicIO.initialize(context, "API_KEY");
```

### User Consent
Predicio SDK gives you access to 3 different functions to manage consent easily:


`checkOptIn` will check user consent regarding Predicio services:
```
// PredicIO server will send "OK" if registered. "KO" if not registered.
PredicIO.getInstance().checkOptIn(callback);
```

`showOptIn` will display a customizable pop-up including `title` and `message` to request user opt-in.
```
PredicIO.getInstance().showOptIn(title, message, activity, callback);
```

`setOptIn` will save user consent regarding Predicio services. Users will be considered as opted-in.
```
PredicIO.getInstance().setOptIn(callback);
```

To get the`checkOptIn` and `showOptIn` functions results, please use callback:
```
public interface HttpRequestResponseCallback {
  void onStringResponseSuccess(String response);
  void onError(VolleyError e);
}
```

### Data collection
After validating user consent, use the following functions to start collecting data:
```
//initialize user identity - pass user's email, we format this data with MD5
PredicIO.getInstance().setIdentity("your_user_email");

// start tracking the initialized identity
PredicIO.getInstance().startTrackingIdentity();

// start tracking user's applications installed on the device
PredicIO.getInstance().startTrackingApps();

// start tracking user's locations
PredicIO.getInstance().startTrackingLocation(activity);

//you can define the accuracy method by using PredicIO.LOCATION_FINE or PredicIO.LOCATION_COARSE, Fine location is used by default.
PredicIO.getInstance().startTrackingLocation(activity,PredicIO.LOCATION_COARSE);

// Track when user opens your application on foreground
PredicIO.getInstance().startTrackingForeground(activity);
```

You can stop collecting and sharing data at any moment using the following functions:
```
PredicIO.getInstance().stopTrackingIdentity();
PredicIO.getInstance().stopTrackingApps();
PredicIO.getInstance().stopTrackingLocation();
PredicIO.getInstance().stopTrackingForeground(activity.getApplication());
```

## Use-case
```
final MainActivity myActivity = this;
PredicIO.initialize(this, "9d5e3ecdeb4cdb7acfd63075ae046672");
PredicIO.getInstance().setIdentity("dev@predic.io");
PredicIO.getInstance().checkOptIn(new HttpRequestResponseCallback() {
  @Override
  public void onStringResponseSuccess(String response) {
    if(response.equals("KO")) {
      String title = "Privacy";
      String message = "Personalize your GDPR-compliant opt-in message here. For more details regarding GDPR please visit: https://www.eugdpr.org/";
      PredicIO.getInstance().showOptIn(title,message,myActivity,new HttpRequestResponseCallback() {
        @Override
        public void onStringResponseSuccess(String response) {
          PredicIO.getInstance().setOptIn(null);
          PredicIO.getInstance().startTrackingIdentity();
          PredicIO.getInstance().startTrackingApps();
          PredicIO.getInstance().startTrackingLocation(myActivity);
          PredicIO.getInstance().startTrackingForeground(myActivity);
        }
        @Override
        public void onError(VolleyError e) {}
      });
    }
    else if(response.equals("OK")) {
      PredicIO.getInstance().startTrackingIdentity();
      PredicIO.getInstance().startTrackingApps();
      PredicIO.getInstance().startTrackingLocation(myActivity);
      PredicIO.getInstance().startTrackingForeground(myActivity);
    }
  }
  @Override
  public void onError(VolleyError e) {}
});
```
   
You're all set! If any questions, please contact support@predic.io
