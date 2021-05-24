package com.tencent.msdk.dns.base.platform;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Platform {

    private static Platform sPlatform = findPlatform();

    private Platform() {
    }

    public static Platform get() {
        return sPlatform;
    }

    public Activity getActivity() {
        // NOTE: 假定getActivity是低频调用方法，不需要通过静态变量hold住反射的方法做缓存
        return null;
    }
    
    public void sendMessage(String msg, Runnable msgWrapperTask) {
        if (null != msgWrapperTask) {
            msgWrapperTask.run();
        }
    }

    private static Platform findPlatform() {
        Platform platform = CocosPlatform.buildIfSupported();
        if (null != platform) {
            return platform;
        }

        platform = UnityPlatform.buildIfSupported();
        if (null != platform) {
            return platform;
        }

        platform = UnrealPlatform.buildIfSupported();
        if (null != platform) {
            return platform;
        }

        return new Platform();
    }

    private static class CocosPlatform extends Platform {

        private static Class sActivityClass;

        private Activity mActivity = null;

        static {
            try {
                sActivityClass = Class.forName("org.cocos2dx.lib.Cocos2dxActivity");
            } catch (Throwable e) {
                sActivityClass = null;
            }
        }

        static Platform buildIfSupported() {
            if (null == sActivityClass) {
                return null;
            }
            return new CocosPlatform();
        }

        @Override
        public Activity getActivity() {
            if (null != mActivity) {
                return mActivity;
            }
            try {
                Method getContextMethod = sActivityClass.getMethod("getContext");
                Context context = (Context) getContextMethod.invoke(null);
                if (context instanceof Activity) {
                    mActivity = (Activity) context;
                }
            } catch (Throwable e) {
                DnsLog.d(e, "Get Activity failed");
            }
            return mActivity;
        }

        @Override
        public void sendMessage(String msg, Runnable msgWrapperTask) {
            if (null == msgWrapperTask) {
                return;
            }
            try {
                Activity activity = mActivity;
                if (null == activity) {
                    activity = getActivity();
                }
                Method runOnGLThread =
                        sActivityClass.getMethod("runOnGLThread", Runnable.class);
                runOnGLThread.invoke(activity, msgWrapperTask);
            } catch (Throwable e) {
                DnsLog.d(e, "Send message to cocos failed");
            }
        }
    }

    private static class UnityPlatform extends Platform {

        private static Class sUnityPlayerClass;

        static {
            try {
                sUnityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            } catch (Throwable e) {
                sUnityPlayerClass = null;
            }
        }

        static Platform buildIfSupported() {
            if (null == sUnityPlayerClass) {
                return null;
            }
            return new UnityPlatform();
        }

        @Override
        public Activity getActivity() {
            try {
                Field currentActivityField = sUnityPlayerClass.getField("currentActivity");
                return (Activity) currentActivityField.get(null);
            } catch (Throwable e) {
                DnsLog.d(e, "Get Activity failed");
                return null;
            }
        }

        @Override
        public void sendMessage(String msg, Runnable msgWrapperTask) {
            if (TextUtils.isEmpty(msg)) {
                return;
            }
            try {
                Method unitySendMessageMethod = sUnityPlayerClass.getMethod(
                        "UnitySendMessage", String.class, String.class, String.class);
                unitySendMessageMethod.invoke(
                        null, "GSDKCallBackGameObject", "GSDKStartCallBack", msg);
            } catch (Throwable e) {
                DnsLog.d(e, "Send message to unity failed");
            }
        }
    }

    private static class UnrealPlatform extends Platform {

        private static Class sActivityClass;

        static {
            try {
                sActivityClass = Class.forName("com.epicgames.ue4.GameActivity");
            } catch (Throwable e) {
                sActivityClass = null;
            }
        }

        static Platform buildIfSupported() {
            if (null == sActivityClass) {
                return null;
            }
            return new UnrealPlatform();
        }

        @Override
        public Activity getActivity() {
            try {
                Method getMethod = sActivityClass.getMethod("Get");
                return (Activity) getMethod.invoke(null);
            } catch (Throwable e) {
                DnsLog.d(e, "Get Activity failed");
                return null;
            }
        }
    }
}
