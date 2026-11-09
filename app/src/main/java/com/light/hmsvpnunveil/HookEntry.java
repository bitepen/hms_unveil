package com.light.hmsvpnunveil;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET_VPN_PKG = "com.google.android.apps.privacy.wildlife";
    private static final String TARGET_SETTINGS_PKG = "com.android.settings";
    private static final String HMS_PKG = "com.huawei.hwid";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals(TARGET_VPN_PKG) && !lpparam.packageName.equals(TARGET_SETTINGS_PKG)) {
            return;
        }

        try {
            Class<?> appPmClass = XposedHelpers.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);

            // 兼顾 Android 13+ (ResolveInfoFlags) 与旧版 (int flags) 重载
            for (Method method : appPmClass.getDeclaredMethods()) {
                if ("queryIntentActivities".equals(method.getName())) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected void afterHookedMethod(MethodHookParam param) {
                            Intent intent = (Intent) param.args[0];
                            if (intent == null) return;

                            // 仅拦截匹配 ACTION_MAIN + CATEGORY_LAUNCHER 的查询
                            if (Intent.ACTION_MAIN.equals(intent.getAction()) 
                                    && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {

                                List<ResolveInfo> results = (List<ResolveInfo>) param.getResult();
                                if (results == null) {
                                    results = new ArrayList<>();
                                } else {
                                    // 保证列表可变，避免不可修改集合触发 UnsupportedOperationException
                                    results = new ArrayList<>(results);
                                }

                                // 检查是否已包含
                                for (ResolveInfo ri : results) {
                                    if (ri.activityInfo != null && HMS_PKG.equals(ri.activityInfo.packageName)) {
                                        return;
                                    }
                                }

                                PackageManager pm = (PackageManager) param.thisObject;
                                try {
                                    ApplicationInfo hmsAppInfo = pm.getApplicationInfo(HMS_PKG, 0);
                                    
                                    ResolveInfo fakeLauncher = new ResolveInfo();
                                    ActivityInfo aInfo = new ActivityInfo();
                                    aInfo.packageName = HMS_PKG;
                                    aInfo.name = "com.huawei.hms.core.activity.JumpActivity";
                                    aInfo.applicationInfo = hmsAppInfo;
                                    aInfo.exported = true;
                                    aInfo.enabled = true;
                                    fakeLauncher.activityInfo = aInfo;

                                    results.add(fakeLauncher);
                                    param.setResult(results);
                                } catch (PackageManager.NameNotFoundException ignored) {
                                    // 设备未安装 HMS Core
                                } catch (Throwable t) {
                                    XposedBridge.log("[HMSVPNUnveil] Error injecting ResolveInfo: " + t);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("[HMSVPNUnveil] Hook setup failed: " + t);
        }
    }
}