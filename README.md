# HTTPDNS SDK Android

## 原理介绍

HttpDNS服务的详细介绍可以参见文章[全局精确流量调度新思路-HttpDNS服务详解](https://cloud.tencent.com/developer/article/1035562)。
总的来说，HttpDNS作为移动互联网时代DNS优化的一个通用解决方案，主要解决了以下几类问题：

- LocalDNS劫持/故障
- LocalDNS调度不准确

HttpDNS的Android SDK，主要提供了基于HttpDNS服务的域名解析和缓存管理能力：

- SDK在进行域名解析时，优先通过HttpDNS服务得到域名解析结果，极端情况下如果HttpDNS服务不可用，则使用LocalDNS解析结果
- HttpDNS服务返回的域名解析结果会携带相关的TTL信息，SDK会使用该信息进行HttpDNS解析结果的缓存管理

## 接入指南

### 依赖版本

- SDK: 28

- NDK: 21.3

- CMake: 3.6

### 权限配置

```
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 如何导入

1. 导入httpdns-sdk-android module

2. 设置 BuildConfig.VERSION_NAME

在引用该SDK源码的项目的根目录中，`build.gradle`的顶部添加如下信息

```
// TODO: 发布时记得修改版本号，注意当前的versionCode是4位，点分的最后以0补全2位
ext.subVersionName = '3.3.0'
def versionArr = subVersionName.toString().split(/\D/)
ext.subVersionCode = Integer.valueOf(String.format('%d%02d%02d',
    versionArr[0].toInteger(), versionArr[1].toInteger(), versionArr[2].toInteger()))
```

   

3. 在App module 的build.gradle文件中，添加如下配置

```kotlin
android { 
  dependencies { 
    // ...
    implementation project(path: ':httpdns-sdk-android'
  }
```
### 初始化

参考Android SDK文档 https://cloud.tencent.com/document/product/379/17655
