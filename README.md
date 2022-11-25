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
    implementation project(path: ':httpdns-sdk-android')
  }
```

### 使用指南

参考Android SDK文档 https://cloud.tencent.com/document/product/379/17655

### 初始化
i.初始化配置服务(可选，4.0.0版本开始支持)

在获取服务实例之前，我们可以通过初始化配置，设置服务的一些属性在SDK初始化时进行配置项传入。
```Java
DnsConfig dnsConfigBuilder = DnsConfig.Builder()
    //（必填）dns 解析 id，即授权 id，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
    .dnsId("xxx")
    //（必填）dns 解析 key，即授权 id 对应的 key（加密密钥），在申请 SDK 后的邮箱里，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
    .dnsKey("xxx")
    //（必填）Channel为desHttp()或aesHttp()时使用 119.29.29.98（默认填写这个就行），channel为https()时使用 119.29.29.99
    .dnsIp("xxx")
    //（可选）channel配置：基于 HTTP 请求的 DES 加密形式，默认为 desHttp()，另有 aesHttp()、https() 可选。（注意仅当选择 https 的 channel 需要选择 119.29.29.99 的dnsip并传入token，例如：.dnsIp('119.29.29.99').https().token('....') ）。
    .desHttp()
    //（可选，选择 https channel 时进行设置）腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于 HTTPS 校验。仅当选用https()时进行填写
    .token("xxx")
    //（可选）日志粒度，如开启Debug打印则传入"Log.VERBOSE"
    .logLevel(Log.VERBOSE)
    //（可选）填写形式："baidu.com", "qq.com"，预解析域名, 建议不要设置太多预解析域名, 当前限制为最多 10 个域名
    .preLookupDomains("baidu.com", "qq.com")
    //（可选）手动指定网络栈支持情况，仅进行 IPv4 解析传 1，仅进行 IPv6 解析传 2，进行 IPv4、IPv6 双栈解析传 3。默认为根据客户端本地网络栈支持情况发起对应的解析请求。
    .setCustomNetStack(3)
    //（可选）设置域名解析请求超时时间，默认为1000ms
    .timeoutMills(1000)
    //（可选）是否开启解析异常上报，默认false，不上报
    .enableReport(true)
    //（可选）[V4.1.0] 解析缓存自动刷新, 以域名形式进行配置，填写形式："baidu.com", "qq.com"。配置的域名会在 TTL * 75% 时自动发起解析请求更新缓存，实现配置域名解析时始终命中缓存。此项建议不要设置太多域名，当前限制为最多 10 个域名。与预解析分开独立配置。
    .persistentCacheDomains("baidu.com", "qq.com")
    //（可选）[V4.2.0] IP 优选，以 IpRankItem(hostname, port) 组成的 List 配置, port（可选）默认值为 8080。例如：IpRankItem("qq.com", 443)。sdk 会根据配置项进行 socket 连接测速情况对解析 IP 进行排序，IP 优选不阻塞当前解析，在下次解析时生效。当前限制为最多 10 项。
    .ipRankItems(ipRankItemList)
    //（可选）[V4.3.0] 设置是否允许使用过期缓存，默认false，解析时先取未过期的缓存结果，不满足则等待解析请求完成后返回解析结果。
    // 设置为true时，会直接返回缓存的解析结果，没有缓存则返回0;0，用户可使用localdns（InetAddress）进行兜底。且在无缓存结果或缓存已过期时，会异步发起解析请求更新缓存。因异步API（getAddrByNameAsync，getAddrsByNameAsync）逻辑在回调中始终返回未过期的解析结果，设置为true时，异步API不可使用。建议使用同步API （getAddrByName，getAddrsByName）。
    .setUseExpiredIpEnable(true)
    //（可选）[V4.3.0] 设置是否启用本地缓存（Room），默认false
    .setCachedIpEnable(true)
    // 以build()结束
    .build();
    
MSDKDnsResolver.getInstance().init(this, dnsConfigBuilder);
```

ii. 老版本初始化方法
>
- HTTP 协议服务地址为 `119.29.29.98`，HTTPS 协议服务地址为 `119.29.29.99`（仅当采用自选加密方式并`channel`为`Https`时使用`99`的IP）。
- 新版本 API 更新为使用 `119.29.29.99/98` 接入，同时原移动解析 HTTPDNS 服务地址 `119.29.29.29` 仅供开发调试使用，无 SLA 保障，不建议用于正式业务，请您尽快将正式业务迁移至 `119.29.29.99/98`。
- 具体以 [API 说明](https://cloud.tencent.com/document/product/379/54976) 提供的 IP 为准。
- 使用 SDK 方式接入 HTTPDNS，若 HTTPDNS 未查询到解析结果，则通过 LocalDNS 进行域名解析，返回 LocalDNS 的解析结果。

#### 默认使用 DES 加密
##### 默认不进行解析异常上报

```Java
// 以下鉴权信息可在腾讯云控制台（https://console.cloud.tencent.com/httpdns/configure）开通服务后获取

/**
 * 初始化 HTTPDNS（默认为 DES 加密）：如果接入了 MSDK，建议初始化 MSDK 后再初始化 HTTPDNS
 *
 * @param context 应用上下文，最好传入 ApplicationContext
 * @param appkey 业务 appkey，即 SDK AppID，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于上报
 * @param dnsid dns解析id，即授权id，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
 * @param dnskey dns解析key，即授权id对应的 key（加密密钥），在申请 SDK 后的邮箱里，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
 * @param dnsIp 由外部传入的dnsIp，可选："119.29.29.98"，以腾讯云文档（https://cloud.tencent.com/document/product/379/54976）提供的 IP 为准
 * @param debug 是否开启 debug 日志，true 为打开，false 为关闭，建议测试阶段打开，正式上线时关闭
 * @param timeout dns请求超时时间，单位ms，建议设置1000
 */
MSDKDnsResolver.getInstance().init(MainActivity.this, appkey, dnsid, dnskey, dnsIp, debug, timeout);
```

##### 手动开启异常解析上报
```Java
// 以下鉴权信息可在腾讯云控制台（https://console.cloud.tencent.com/httpdns/configure）开通服务后获取

/**
 * 初始化 HTTPDNS（默认为 DES 加密）：如果接入了 MSDK，建议初始化 MSDK 后再初始化 HTTPDNS
 *
 * @param context 应用上下文，最好传入 ApplicationContext
 * @param appkey 业务 appkey，即 SDK AppID，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于上报
 * @param dnsid dns解析id，即授权id，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
 * @param dnskey dns解析key，即授权id对应的 key（加密密钥），在申请 SDK 后的邮箱里，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
 * @param dnsIp 由外部传入的dnsIp，可选："119.29.29.98"（仅支持 http 请求，channel为DesHttp和AesHttp时选择），"119.29.29.99"（仅支持 https 请求，channel为Https时选择）以腾讯云文档（https://cloud.tencent.com/document/product/379/54976）提供的 IP 为准
 * @param debug 是否开启 debug 日志，true 为打开，false 为关闭，建议测试阶段打开，正式上线时关闭
 * @param timeout dns请求超时时间，单位ms，建议设置1000
 * @param enableReport 是否开启解析异常上报，默认false，不上报
 */
MSDKDnsResolver.getInstance().init(MainActivity.this, appkey, dnsid, dnskey, dnsIp, debug, timeout, enableReport);
```


#### 自选加密方式（DesHttp, AesHttp, Https）

```Java
/**
 * 初始化 HTTPDNS（自选加密方式）：如果接入了 MSDK，建议初始化 MSDK 后再初始化 HTTPDNS
 *
 * @param context 应用上下文，最好传入 ApplicationContext
 * @param appkey 业务 appkey，即 SDK AppID，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于上报
 * @param dnsid dns解析id，即授权id，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
 * @param dnskey dns解析key，即授权id对应的 key（加密密钥），在申请 SDK 后的邮箱里，腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于域名解析鉴权
 * @param dnsIp 由外部传入的dnsIp，可选："119.29.29.98"（仅支持 http 请求，channel为DesHttp和AesHttp时选择），"119.29.29.99"（仅支持 https 请求，channel为Https时选择）以腾讯云文档（https://cloud.tencent.com/document/product/379/54976）提供的 IP 为准
 * @param debug 是否开启 debug 日志，true 为打开，false 为关闭，建议测试阶段打开，正式上线时关闭
 * @param timeout dns请求超时时间，单位ms，建议设置1000
 * @param channel 设置 channel，可选：DesHttp（默认）, AesHttp, Https
 * @param token 腾讯云官网（https://console.cloud.tencent.com/httpdns）申请获得，用于 HTTPS 校验
 * @param enableReport 是否开启解析异常上报，默认false，不上报
 */
MSDKDnsResolver.getInstance().init(MainActivity.this, appkey, dnsid, dnskey, dnsIp, debug, timeout, channel, token, true);
```

#### 网络安全配置兼容

App targetSdkVersion >= 28(Android 9.0)情况下，系统默认不允许 HTTP 网络请求，详细信息参见 [Opt out of cleartext traffic](https://developer.android.com/training/articles/security-config#Opt%20out%20of%20cleartext%20traffic)。
这种情况下，业务侧需要将 HTTPDNS 请求使用的 IP 配置到域名白名单中：
- AndroidManifest 文件中配置。
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest ... >
    <application android:networkSecurityConfig="@xml/network_security_config"
                    ... >
        
    </application>
</manifest>
```
- XML 目录下添加 network_security_config.xml 配置文件。
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">119.29.29.98</domain>
        <domain includeSubdomains="false">119.28.28.98</domain>
    </domain-config>
</network-security-config>
```


