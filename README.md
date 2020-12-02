# XposedHotLoader
## 这是什么?

```Xposed```模块热加载器,在开发中无需每次重启手机即可应用新的代码

## 如何使用?

1. 克隆```HotLoader```库到工程目录```git clone https://github.com/jrsen/Xposed-HotLoader.git libhotloader```

2. 编辑工程```settings.gradle```添加```include ':libhotloader'```将```hotloader```项目添加到项目工程

3. 编辑主项目```build.grade```添加依赖```implementation project(path: ':libhotloader')```

4. 编辑主项目```assets/xposed_init```文件使用```com.kaisar.xposed.hotloader.HotLoader```替换原来的内容，并且将您的Xposed初始化类在```AndroidManifest.xml```的Application标签中声明

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

5. 重启手机激活热加载功能

## 常见问题

- 更新模块后新的代码没有生效?

   所有改动都将在目标进程重启后应用,你可以手动杀死目标进程以应用新的模块


[English](README_en.md)