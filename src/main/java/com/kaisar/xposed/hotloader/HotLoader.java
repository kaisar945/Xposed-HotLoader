package com.kaisar.xposed.hotloader;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Process;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed module hot loader
 * example:
 * Add a new meta-data in the application Manifest#application tag
 * <meta-data
 * android:name="xposed_init"
 * android:value="your.xposed.init.class" />
 */
public final class HotLoader implements IXposedHookLoadPackage
{

    private static final String TAG = "HotLoader";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
    {
        if(!lpparam.packageName.equals("com.android.chrome")) return;
        try
        {
            boolean isSystem = "android".equals(lpparam.packageName);
            IXposedHookLoadPackage xposedModuleEntry = getModuleEntry(isSystem);
            xposedModuleEntry.handleLoadPackage(lpparam);
            logd(String.format("hot load [%s] pid=%s success", lpparam.packageName, Process.myPid()));
        }
        catch(Throwable throwable)
        {
            if(throwable instanceof XposedHelpers.InvocationTargetError)
            {
                throwable = throwable.getCause();
            }
            loge(String.format("hot load [%s] pid=%s failed", lpparam.packageName, Process.myPid()), throwable);
            XposedBridge.log(throwable);
        }
    }

    private String getModulePackageName()
    {
        BaseDexClassLoader loader = (BaseDexClassLoader)getClass().getClassLoader();
        Object pathList = XposedHelpers.getObjectField(loader, "pathList");
        Object dexElement = Array.get(XposedHelpers.getObjectField(pathList, "dexElements"), 0);
        File path = (File)XposedHelpers.getObjectField(dexElement, "path");
        String name = path.getParentFile().getName();
        return name.split("-")[0];
    }

    private IXposedHookLoadPackage getModuleEntry(boolean isSystem) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, XmlPullParserException, NoSuchMethodException, InvocationTargetException
    {
        PackageInfo packageInfo = isSystem ? getPackageInfoFromParser() : getPackageInfoFromService();
        Objects.requireNonNull(packageInfo, "can't found module package info");
        String xposedInit = packageInfo.applicationInfo.metaData.getString("xposed_init");
        PathClassLoader classLoader = new PathClassLoader(packageInfo.applicationInfo.sourceDir, IXposedHookLoadPackage.class.getClassLoader());
        return (IXposedHookLoadPackage)classLoader.loadClass(xposedInit).newInstance();
    }

    private PackageInfo getPackageInfoFromParser() throws XmlPullParserException, IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        String modulePackageName = getModulePackageName();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        try(FileReader fr = new FileReader("/data/system/packages.xml"))
        {
            parser.setInput(fr);
            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT)
            {
                if(eventType == XmlPullParser.START_TAG)
                {
                    if("package".equals(parser.getName()) && modulePackageName.equals(parser.getAttributeValue(null, "name")))
                    {
                        String codePath = new File(parser.getAttributeValue(null, "codePath"), "base.apk").getPath();
                        PackageInfo packageInfo = getPackageManagerCompat().getPackageArchiveInfo(codePath, PackageManager.GET_META_DATA);
                        packageInfo.applicationInfo.sourceDir = packageInfo.applicationInfo.publicSourceDir = codePath;
                        return packageInfo;
                    }
                }
                eventType = parser.next();
            }
        }
        throw new FileNotFoundException("can't found module code path");
    }

    private PackageInfo getPackageInfoFromService() throws ClassNotFoundException
    {
        String modulePackageName = getModulePackageName();
        @SuppressLint("PrivateApi") IBinder binder = (IBinder)XposedHelpers.callStaticMethod(Class.forName("android.os.ServiceManager"), "checkService", "package");
        @SuppressLint("PrivateApi") IInterface packageService = (IInterface)XposedHelpers.callStaticMethod(Class.forName("android.content.pm.IPackageManager$Stub"), "asInterface", binder);
        return (PackageInfo)XposedHelpers.callMethod(packageService, "getPackageInfo", modulePackageName, PackageManager.GET_META_DATA, 0);
    }

    private PackageManager getPackageManagerCompat() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        @SuppressLint("PrivateApi") Class<?> ApplicationPackageManagerClass = Class.forName("android.app.ApplicationPackageManager");
        @SuppressLint("PrivateApi") Class<?> ContextImplClass = Class.forName("android.app.ContextImpl");
        @SuppressLint("PrivateApi") Class<?> IPackageManagerClass = Class.forName("android.content.pm.IPackageManager");
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        {
            Constructor<?> constructor = ApplicationPackageManagerClass.getDeclaredConstructor(ContextImplClass, IPackageManagerClass);
            constructor.setAccessible(true);
            return (PackageManager)constructor.newInstance(null, null);
        }
        else
        {
            @SuppressLint("PrivateApi") Class<?> IPermissionManagerClass = Class.forName("android.permission.IPermissionManager");
            Constructor<?> constructor = ApplicationPackageManagerClass.getDeclaredConstructor(ContextImplClass, IPackageManagerClass, IPermissionManagerClass);
            constructor.setAccessible(true);
            return (PackageManager)constructor.newInstance(null, null, null);
        }
    }

    private void logd(String message)
    {
        if(Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, message);
        }
    }

    private void loge(String message, Throwable throwable)
    {
        if(Log.isLoggable(TAG, Log.ERROR))
        {
            Log.e(TAG, message, throwable);
        }
    }

}
