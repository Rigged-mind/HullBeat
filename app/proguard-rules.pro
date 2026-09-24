# R8 rules for the release build.
#
# app/build.gradle.kts already references this file, so its absence broke the
# release build - the debug build and `test` never touched it, which is why the
# gap was invisible from here.

# Room generates implementations by name and reflects on entity constructors.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# kotlinx.serialization keeps its generated serializers via a companion field
# that R8 cannot see is used. Losing them fails at runtime, not at build time,
# which makes it the worst kind of shrink error.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class app.hullbeat.** {
    *** Companion;
}
-keepclasseswithmembers class app.hullbeat.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The catalog and checklists are parsed from assets into these classes.
-keep,includedescriptorclasses class app.hullbeat.data.catalog.** { *; }

# ML Kit ships optional model downloaders we do not use.
-dontwarn com.google.mlkit.**

# java.time on API 26+ needs no desugaring, but WorkManager reflects on
# Worker subclasses by class name.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
