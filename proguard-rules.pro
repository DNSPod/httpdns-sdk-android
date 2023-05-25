# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5 # 指定代码的压缩级别
-dontusemixedcaseclassnames # 是否使用大小写混合
-dontpreverify # 混淆时是否做预校验
-dontskipnonpubliclibraryclassmembers # 指定不去忽略非公共库的类成员
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/* # 混淆时所采用的算法
-ignorewarnings
-verbose
-printmapping 'proguardMapping.txt' # 指定映射文件名
-keepattributes SourceFile,LineNumberTable # 抛出异常时保留代码行数

# Jni调用相关, 在App中也需要Keep
-keep public class com.tencent.msdk.dns.base.jni.Jni {*;}

# 对外接口类
-keep public class com.tencent.msdk.dns.DnsService {*;}
-keep public class com.tencent.msdk.dns.core.IpSet {*;}
-keep public class com.tencent.msdk.dns.core.ipRank.IpRankItem {*;}
-keep public class com.tencent.msdk.dns.DnsConfig {*;}
-keep public class com.tencent.msdk.dns.HttpDnsResponseObserver {*;}
-keep public class com.tencent.msdk.dns.DnsConfig$Builder {*;}
-keep public class com.tencent.msdk.dns.base.executor.DnsExecutors$ExecutorSupplier {*;}

-keep class * implements com.tencent.msdk.dns.core.IDns$IStatistics {*;}
-keep class * implements com.tencent.msdk.dns.core.IStatisticsMerge {*;}
-keep public class com.tencent.msdk.dns.core.LookupResult {*;}
-keep public class com.tencent.msdk.dns.ILookedUpListener {*;}
-keep public class com.tencent.msdk.dns.base.log.ILogNode {*;}
-keep public class com.tencent.msdk.dns.base.report.IReporter {*;}

# 后向兼容接口类
-keep public class com.tencent.msdk.dns.MSDKDnsResolver {*;}
-keep public class com.tencent.msdk.dns.HttpDnsCache$ConnectivityChangeReceiver {*;}
