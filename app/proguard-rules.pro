# 保留代码行号信息，便于调试
-keepattributes SourceFile,LineNumberTable

# 避免混淆注解、内部类、泛型和匿名类
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# 腾讯Bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

# 四大组件保持不混淆
#-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.view.View

# 保持support和androidx库中的类不被混淆
-keep class android.support.** {*;}
-keep class androidx.** {*;}

# 保持继承的支持库类不被混淆
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**

# 保留自定义View类不被混淆
-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}


# 保留 android.os.SystemProperties 类及其 get 方法不被混淆
-keep class android.os.SystemProperties {
    public static java.lang.String get(java.lang.String, java.lang.String);
}

# 保持序列化相关类成员不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保持 butterknife 注解相关代码不被混淆
-keep @interface butterknife.**
-keepclasseswithmembers class * {
    @butterknife.* <fields>;
    @butterknife.* <methods>;
    @butterknife.On* <methods>;
}
# 配置自定义字典
#-obfuscationdictionary lloveyou.txt
#-classobfuscationdictionary lloveyou.txt
#-packageobfuscationdictionary lloveyou.txt

# JS 调用 Java 方法时需要保留的规则
-keepattributes *JavascriptInterface*
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# 允许修改访问权限、合并接口
-allowaccessmodification
-mergeinterfacesaggressively

# 减少不必要的警告和提示
-dontwarn **
-dontnote **

# 保留一些常用 Android 库和枚举相关的代码
-keepclassmembers enum * {
  public static **[] values();
  public static ** valueOf(java.lang.String);
}
-keep public class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 日志代码优化规则
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 避免优化
#-dontoptimize

# 其余通用配置
-dontpreverify
-forceprocessing
-verbose