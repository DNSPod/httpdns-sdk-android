package com.tencent.msdk.dns.base.lifecycle;

import android.app.Activity;
import android.os.Bundle;

public abstract class ActivityLifecycleCallbacksWrapper {

    public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }

    public void onActivityStarted(Activity activity) { }

    public void onActivityResumed(Activity activity) { }

    public void onActivityPaused(Activity activity) { }

    public void onActivityStopped(Activity activity) { }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

    public void onActivityDestroyed(Activity activity) { }
}
