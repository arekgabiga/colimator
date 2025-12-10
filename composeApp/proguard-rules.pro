# ProGuard rules
# -dontnote is for classes, not resource files. The manifest notes are harmless.

# Keep kotlinx.serialization classes
-keep class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.json.** { *; }
-keep class kotlinx.serialization.internal.** { *; }
-keep class kotlinx.serialization.descriptors.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, AnnotationDefault

# Keep generated serializers
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepnames class *$$serializer {
    static kotlinx.serialization.internal.GeneratedSerializer INSTANCE;
}
-keepclassmembers class * {
    static **$$serializer serializer(...);
}
-keepclasseswithmembers class * {
    static **$$serializer serializer(...);
}
-keep class *$$serializer { *; }

# Keep companion objects of serializable classes
-keepclasseswithmembers class * {
    public static ** Companion;
}

# PTY4J native library loading - required for terminal emulation
-keep class com.pty4j.** { *; }
-keep class com.pty4j.unix.** { *; }
-keep class com.pty4j.windows.** { *; }
-keepclassmembers class com.pty4j.** {
    native <methods>;
}

# JediTerm terminal emulator
-keep class com.jediterm.** { *; }

# JNA (Java Native Access) - used by PTY4J for native calls
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.ptr.** { *; }
-keepclassmembers class * extends com.sun.jna.Structure {
    <fields>;
}
-keepclassmembers class * implements com.sun.jna.Callback {
    *;
}
