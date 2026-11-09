# kotlinx.serialization: keep generated serializers for our JSON file formats.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.gymplanner.app.core.model.**$$serializer { *; }
-keepclassmembers class com.gymplanner.app.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.gymplanner.app.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
