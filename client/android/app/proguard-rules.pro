# kotlinx.serialization keeps generated serializers via @Serializable; the
# Kotlin compiler plugin emits them, but R8 needs these to retain metadata.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class org.myhab.voice.**$$serializer { *; }
-keepclassmembers class org.myhab.voice.** {
    *** Companion;
}
-keepclasseswithmembers class org.myhab.voice.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Native methods on the vendored microWakeWord class are bound via JNI
# RegisterNatives against this exact class/package — keep it intact.
-keep class io.homeassistant.companion.android.microwakeword.** { *; }
