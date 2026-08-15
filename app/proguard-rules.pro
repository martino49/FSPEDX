# FSPEDX ProGuard rules

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Gson
-keep class com.google.gson.** { *; }

# Keep model classes used in backup JSON
-keep class com.fspedx.booster.data.models.** { *; }

# Keep FSPEDX application entry points referenced from manifest
-keep class com.fspedx.booster.FSPEDXApplication { *; }
-keep class com.fspedx.booster.di.AppContainer { *; }