-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class nl.tudelft.trustchain.gossipML.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class nl.tudelft.trustchain.gossipML.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class nl.tudelft.trustchain.gossipML.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}