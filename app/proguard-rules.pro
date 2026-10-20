# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
# Kotlinx serialization
-keepclassmembers class com.intent.reader.** {
    *** Companion;
}
-keepclasseswithmembers class com.intent.reader.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# OkHttp platform warnings are harmless
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
