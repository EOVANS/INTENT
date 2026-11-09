# Room, WorkManager, Glance, kotlinx.serialization, and sharedview-kit ship
# consumer rules that AGP merges automatically. In particular, serialization
# keeps only the annotated models' companions/serializers; keeping every app
# Companion or every class with serializer() unnecessarily restricts R8.
# Add narrowly scoped rules here only for app-owned reflective/JNI entry points.

# Optional TLS providers used by OkHttp; Android uses the platform provider.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
