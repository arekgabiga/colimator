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
