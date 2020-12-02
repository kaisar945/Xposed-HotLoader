# XposedHotLoader
## What is this?

```Xposed``` module hot loader, new code can be applied without restarting the phone every time during development

## How to use?

1. Clone ```HotLoader``` library project to the project directory ```git clone https://github.com/jrsen/Xposed-HotLoader.git libhotloader ```

2. Edit the project ```settings.gradle``` and add ```include':libhotloader'``` to add the ```hotloader``` library to the project

3. Edit the main project ```build.grade``` and add dependency ```implementation project(path:':libhotloader')```

4. Edit the main project ```assets/xposed_init``` file to replace the original content with ```com.kaisar.xposed.hotloader.HotLoader```, and place your Xposed initialization class in ```AndroidManifest.xml``` application tag like this e.g:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       package="com.kaisar.hotloader.example">
   
       <application >
           <meta-data
               android:name="xposed_init"
               android:value="your.xposed.init.class" />
       </application>
   
   </manifest>
   ```

5. Restart the phone to activate the hot reload function

## Q&A

- The new code does not take effect after updating the module?

   All changes will be applied after the target process restarts, you can manually kill the target process to apply the new module

[中文](README_zh.md)

