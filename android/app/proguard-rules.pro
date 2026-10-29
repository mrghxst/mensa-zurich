# kotlinx.serialization ships its own consumer rules; DTOs are annotated with @Serializable.
# Keep line numbers for readable stack traces in the private build.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# core-splashscreen 1.2.0 under R8: without this the system splash window never goes away on
# API 37 (the app draws underneath it). Verified on the emulator; the library is small.
-keep class androidx.core.splashscreen.** { *; }
