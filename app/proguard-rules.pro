# Release builds are shrunk and obfuscated (see buildTypes.release). Rules here are only for code R8 cannot see being used.

# Crash reports stay readable: keep file names and line numbers. Decode the rest with the build's mapping.txt
# (app/build/outputs/mapping/release/mapping.txt; keep a copy of it for every version you ship).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson fills these classes by field name when it reads the question bank from assets (MedMcqaRepository).
-keepattributes Signature,*Annotation*
-keep class com.medstudy.core.data.**Json { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
