# Andraste Tablet — R8 / ProGuard rules

# JSch (SSH) uses reflection for its config classes
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Gson — keep model classes' fields
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Compose keeps its own rules via the AGP; nothing extra needed.
