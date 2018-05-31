package io.predic.tracker;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

class ApplicationLifecycleManager implements Application.ActivityLifecycleCallbacks {
    private int nbRunningActivities = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d("PREDICIO","activity create");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d("PREDICIO","activity destroy");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d("PREDICIO","activity resume");
        nbRunningActivities++;
        if (nbRunningActivities == 1) {
            Log.d("PREDICIO", "Application in foreground.");
            PredicIO.getInstance().sendHttpForegroundRequest(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d("PREDICIO","activity pause");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d("PREDICIO","activity start");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d("PREDICIO","activity stop");
        if(nbRunningActivities > 0) {
            nbRunningActivities = 0;
            Log.d("PREDICIO", "Application in background.");
            PredicIO.getInstance().sendHttpBackgroundRequest();
        }
    }
}

