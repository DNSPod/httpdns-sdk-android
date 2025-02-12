package com.tencent.msdk.dns.base.lifecycle;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public final class ActivityLifecycleDetector {

    private static final String INSTRUMENTATION_FIELD_NAME = "mInstrumentation";

    private static List<ActivityLifecycleCallbacksWrapper> sActivityLifecycleCallbacks =
            new CopyOnWriteArrayList<>();

    private static boolean sDetected = false;

    public static void install(Context context) {
        detectActivityLifecycleV14(context);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            detectActivityLifecycleV14(context);
//        } else {
//            detectActivityLifecycle();
//        }
    }

    public static synchronized boolean registerActivityLifecycleCallbacks(
            ActivityLifecycleCallbacksWrapper lifecycleCallbacks) {
        if (sDetected) {
            sActivityLifecycleCallbacks.add(lifecycleCallbacks);
        }
        return sDetected;
    }

//    private static void detectActivityLifecycle() {
//        try {
//            ActivityThread activityThread = ActivityThread.currentActivityThread();
//            Field mInstrumentationField =
//                    ActivityThread.class.getDeclaredField(INSTRUMENTATION_FIELD_NAME);
//            mInstrumentationField.setAccessible(true);
//            final Instrumentation realInstrumentation =
//                    (Instrumentation) mInstrumentationField.get(activityThread);
//            if (null != realInstrumentation) {
//                sDetected = true;
//                Instrumentation instrumentation = new Instrumentation() {
//
//                    @Override
//                    public void callActivityOnCreate(Activity activity, Bundle icicle) {
//                        DnsLog.d("%s.onCreate", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityCreated(activity, icicle);
//                        }
//
//                        realInstrumentation.callActivityOnCreate(activity, icicle);
//                    }
//
//                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//                    @Override
//                    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle
//                    persistentState) {
//                        DnsLog.d("%s.onCreate", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityCreated(activity, icicle);
//                        }
//
//                        realInstrumentation.callActivityOnCreate(activity, icicle, persistentState);
//                    }
//
//                    @Override
//                    public void callActivityOnStart(Activity activity) {
//                        DnsLog.d("%s.onStart", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityStarted(activity);
//                        }
//
//                        realInstrumentation.callActivityOnStart(activity);
//                    }
//
//                    @Override
//                    public void callActivityOnResume(Activity activity) {
//                        DnsLog.d("%s.onResume", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityResumed(activity);
//                        }
//
//                        realInstrumentation.callActivityOnResume(activity);
//                    }
//
//                    @Override
//                    public void callActivityOnPause(Activity activity) {
//                        DnsLog.d("%s.onPause", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityPaused(activity);
//                        }
//
//                        realInstrumentation.callActivityOnPause(activity);
//                    }
//
//                    @Override
//                    public void callActivityOnStop(Activity activity) {
//                        DnsLog.d("%s.onStop", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityStopped(activity);
//                        }
//
//                        realInstrumentation.callActivityOnStop(activity);
//                    }
//
//                    @Override
//                    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState) {
//                        DnsLog.d("%s.onSaveInstanceState", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivitySaveInstanceState(activity, outState);
//                        }
//
//                        realInstrumentation.callActivityOnSaveInstanceState(activity, outState);
//                    }
//
//                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//                    @Override
//                    public void callActivityOnSaveInstanceState(Activity activity, Bundle outState,
//                    PersistableBundle outPersistentState) {
//                        DnsLog.d("%s.onSaveInstanceState", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivitySaveInstanceState(activity, outState);
//                        }
//
//                        realInstrumentation.callActivityOnSaveInstanceState(activity, outState, outPersistentState);
//                    }
//
//                    @Override
//                    public void callActivityOnDestroy(Activity activity) {
//                        DnsLog.d("%s.onDestroy", activity);
//
//                        for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
//                            lifecycleCallbacks.onActivityDestroyed(activity);
//                        }
//
//                        realInstrumentation.callActivityOnDestroy(activity);
//                    }
//
//                    // NOTE: 其他方法代理, 避免影响其他处理
//
//                    @Override
//                    public void onCreate(Bundle arguments) {
//                        realInstrumentation.onCreate(arguments);
//                    }
//
//                    @Override
//                    public void start() {
//                        realInstrumentation.start();
//                    }
//
//                    @Override
//                    public void onStart() {
//                        realInstrumentation.onStart();
//                    }
//
//                    @Override
//                    public boolean onException(Object obj, Throwable e) {
//                        return realInstrumentation.onException(obj, e);
//                    }
//
//                    @Override
//                    public void sendStatus(int resultCode, Bundle results) {
//                        realInstrumentation.sendStatus(resultCode, results);
//                    }
//
//                    // addResults: API26引入
////                    @Override
////                    public void addResults(Bundle results) {
////                        realInstrumentation.addResults(results);
////                    }
//
//                    @Override
//                    public void finish(int resultCode, Bundle results) {
//                        realInstrumentation.finish(resultCode, results);
//                    }
//
//                    @Override
//                    public void setAutomaticPerformanceSnapshots() {
//                        realInstrumentation.setAutomaticPerformanceSnapshots();
//                    }
//
//                    @Override
//                    public void startPerformanceSnapshot() {
//                        realInstrumentation.startPerformanceSnapshot();
//                    }
//
//                    @Override
//                    public void endPerformanceSnapshot() {
//                        realInstrumentation.endPerformanceSnapshot();
//                    }
//
//                    @Override
//                    public void onDestroy() {
//                        realInstrumentation.onDestroy();
//                    }
//
//                    @Override
//                    public Context getContext() {
//                        return realInstrumentation.getContext();
//                    }
//
//                    @Override
//                    public ComponentName getComponentName() {
//                        return realInstrumentation.getComponentName();
//                    }
//
//                    @Override
//                    public Context getTargetContext() {
//                        return realInstrumentation.getTargetContext();
//                    }
//
//                    // getProcessName: API26引入
////                    @Override
////                    public String getProcessName() {
////                        return realInstrumentation.getProcessName();
////                    }
//
//                    @Override
//                    public boolean isProfiling() {
//                        return realInstrumentation.isProfiling();
//                    }
//
//                    @Override
//                    public void startProfiling() {
//                        realInstrumentation.startProfiling();
//                    }
//
//                    @Override
//                    public void stopProfiling() {
//                        realInstrumentation.stopProfiling();
//                    }
//
//                    @Override
//                    public void setInTouchMode(boolean inTouch) {
//                        realInstrumentation.setInTouchMode(inTouch);
//                    }
//
//                    @Override
//                    public void waitForIdle(Runnable recipient) {
//                        realInstrumentation.waitForIdle(recipient);
//                    }
//
//                    @Override
//                    public void waitForIdleSync() {
//                        realInstrumentation.waitForIdleSync();
//                    }
//
//                    @Override
//                    public void runOnMainSync(Runnable runner) {
//                        realInstrumentation.runOnMainSync(runner);
//                    }
//
//                    @Override
//                    public Activity startActivitySync(Intent intent) {
//                        return realInstrumentation.startActivitySync(intent);
//                    }
//
//                    // startActivitySync: API28引入
////                    @androidx.annotation.NonNull
////                    @Override
////                    public Activity startActivitySync(@androidx.annotation.NonNull Intent intent, @androidx
// .annotation.Nullable Bundle options) {
////                        return realInstrumentation.startActivitySync(intent, options);
////                    }
//
//                    @Override
//                    public void addMonitor(ActivityMonitor monitor) {
//                        realInstrumentation.addMonitor(monitor);
//                    }
//
//                    @Override
//                    public ActivityMonitor addMonitor(IntentFilter filter, ActivityResult result, boolean block) {
//                        return realInstrumentation.addMonitor(filter, result, block);
//                    }
//
//                    @Override
//                    public ActivityMonitor addMonitor(String cls, ActivityResult result, boolean block) {
//                        return realInstrumentation.addMonitor(cls, result, block);
//                    }
//
//                    @Override
//                    public boolean checkMonitorHit(ActivityMonitor monitor, int minHits) {
//                        return realInstrumentation.checkMonitorHit(monitor, minHits);
//                    }
//
//                    @Override
//                    public Activity waitForMonitor(ActivityMonitor monitor) {
//                        return realInstrumentation.waitForMonitor(monitor);
//                    }
//
//                    @Override
//                    public Activity waitForMonitorWithTimeout(ActivityMonitor monitor, long timeOut) {
//                        return realInstrumentation.waitForMonitorWithTimeout(monitor, timeOut);
//                    }
//
//                    @Override
//                    public void removeMonitor(ActivityMonitor monitor) {
//                        realInstrumentation.removeMonitor(monitor);
//                    }
//
//                    @Override
//                    public boolean invokeMenuActionSync(Activity targetActivity, int id, int flag) {
//                        return realInstrumentation.invokeMenuActionSync(targetActivity, id, flag);
//                    }
//
//                    @Override
//                    public boolean invokeContextMenuAction(Activity targetActivity, int id, int flag) {
//                        return realInstrumentation.invokeContextMenuAction(targetActivity, id, flag);
//                    }
//
//                    @Override
//                    public void sendStringSync(String text) {
//                        realInstrumentation.sendStringSync(text);
//                    }
//
//                    @Override
//                    public void sendKeySync(KeyEvent event) {
//                        realInstrumentation.sendKeySync(event);
//                    }
//
//                    @Override
//                    public void sendKeyDownUpSync(int key) {
//                        realInstrumentation.sendKeyDownUpSync(key);
//                    }
//
//                    @Override
//                    public void sendCharacterSync(int keyCode) {
//                        realInstrumentation.sendCharacterSync(keyCode);
//                    }
//
//                    @Override
//                    public void sendPointerSync(MotionEvent event) {
//                        realInstrumentation.sendPointerSync(event);
//                    }
//
//                    @Override
//                    public void sendTrackballEventSync(MotionEvent event) {
//                        realInstrumentation.sendTrackballEventSync(event);
//                    }
//
//                    @Override
//                    public Application newApplication(ClassLoader cl, String className, Context context) throws
//                    ClassNotFoundException, IllegalAccessException, InstantiationException {
//                        return realInstrumentation.newApplication(cl, className, context);
//                    }
//
//                    @Override
//                    public void callApplicationOnCreate(Application app) {
//                        realInstrumentation.callApplicationOnCreate(app);
//                    }
//
//                    @Override
//                    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application
//                    application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id,
//                    Object lastNonConfigurationInstance) throws IllegalAccessException, InstantiationException {
//                        return realInstrumentation.newActivity(clazz, context, token, application, intent, info,
//                        title, parent, id, lastNonConfigurationInstance);
//                    }
//
//                    @Override
//                    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws
//                    ClassNotFoundException, IllegalAccessException, InstantiationException {
//                        return realInstrumentation.newActivity(cl, className, intent);
//                    }
//
//                    @Override
//                    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState) {
//                        realInstrumentation.callActivityOnRestoreInstanceState(activity, savedInstanceState);
//                    }
//
//                    // callActivityOnRestoreInstanceState: API21引入
////                    @Override
////                    public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState,
// PersistableBundle persistentState) {
////                        realInstrumentation.callActivityOnRestoreInstanceState(activity, savedInstanceState,
// persistentState);
////                    }
//
//                    @Override
//                    public void callActivityOnPostCreate(Activity activity, Bundle icicle) {
//                        realInstrumentation.callActivityOnPostCreate(activity, icicle);
//                    }
//
//                    // callActivityOnPostCreate: API21引入
////                    @Override
////                    public void callActivityOnPostCreate(Activity activity, Bundle icicle, PersistableBundle
// persistentState) {
////                        realInstrumentation.callActivityOnPostCreate(activity, icicle, persistentState);
////                    }
//
//                    @Override
//                    public void callActivityOnNewIntent(Activity activity, Intent intent) {
//                        realInstrumentation.callActivityOnNewIntent(activity, intent);
//                    }
//
//                    @Override
//                    public void callActivityOnRestart(Activity activity) {
//                        realInstrumentation.callActivityOnRestart(activity);
//                    }
//
//                    @Override
//                    public void callActivityOnUserLeaving(Activity activity) {
//                        realInstrumentation.callActivityOnUserLeaving(activity);
//                    }
//
//                    @Override
//                    public void startAllocCounting() {
//                        realInstrumentation.startAllocCounting();
//                    }
//
//                    @Override
//                    public void stopAllocCounting() {
//                        realInstrumentation.stopAllocCounting();
//                    }
//
//                    @Override
//                    public Bundle getAllocCounts() {
//                        return realInstrumentation.getAllocCounts();
//                    }
//
//                    @Override
//                    public Bundle getBinderCounts() {
//                        return realInstrumentation.getBinderCounts();
//                    }
//
//                    // getUiAutomation: API18引入
////                    @Override
////                    public UiAutomation getUiAutomation() {
////                        return realInstrumentation.getUiAutomation();
////                    }
//
//                    // getUiAutomation: API24引入
////                    @Override
////                    public UiAutomation getUiAutomation(int flags) {
////                        return realInstrumentation.getUiAutomation(flags);
////                    }
//
//                    // acquireLooperManager: API26引入
////                    @Override
////                    public TestLooperManager acquireLooperManager(Looper looper) {
////                        return realInstrumentation.acquireLooperManager(looper);
////                    }
//                };
//                mInstrumentationField.set(activityThread, instrumentation);
//            }
//        } catch (Throwable tr) {
//        }
//    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void detectActivityLifecycleV14(Context context) {
        Application application = ApplicationProvider.get(context);
        if (null != application) {
            sDetected = true;
            application.registerActivityLifecycleCallbacks(
                    new Application.ActivityLifecycleCallbacks() {

                        @Override
                        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                            DnsLog.d("%s.onCreate", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivityCreated(activity, savedInstanceState);
                            }
                        }

                        @Override
                        public void onActivityStarted(Activity activity) {
                            DnsLog.d("%s.onStart", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivityStarted(activity);
                            }
                        }

                        @Override
                        public void onActivityResumed(Activity activity) {
                            DnsLog.d("%s.onResume", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivityResumed(activity);
                            }
                        }

                        @Override
                        public void onActivityPaused(Activity activity) {
                            DnsLog.d("%s.onPause", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivityPaused(activity);
                            }
                        }

                        @Override
                        public void onActivityStopped(Activity activity) {
                            DnsLog.d("%s.onStop", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivityStopped(activity);
                            }
                        }

                        @Override
                        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                            DnsLog.d("%s.onSaveInstanceState", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivitySaveInstanceState(activity, outState);
                            }
                        }

                        @Override
                        public void onActivityDestroyed(Activity activity) {
                            DnsLog.d("%s.onDestroy", activity);

                            for (ActivityLifecycleCallbacksWrapper lifecycleCallbacks : sActivityLifecycleCallbacks) {
                                lifecycleCallbacks.onActivityDestroyed(activity);
                            }
                        }
                    });
        }
    }
}
