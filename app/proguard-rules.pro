# Compose
-keep class androidx.compose.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson serialized model classes
-keep class com.nsai.notes.domain.model.MCPServer { *; }
-keep class com.nsai.notes.domain.model.SkillPlugin { *; }
-keep class com.nsai.notes.domain.model.AIProviderConfig { *; }

# Gson-deserialized inner classes
-keep class com.nsai.notes.data.remote.ai.BaseAIAdapter$ChatResponseBody { *; }
-keep class com.nsai.notes.data.remote.ai.BaseAIAdapter$ChatRequestBody { *; }
-keep class com.nsai.notes.data.remote.ai.BaseAIAdapter$ChatResponseBody$Choice { *; }
-keep class com.nsai.notes.data.remote.ai.BaseAIAdapter$ChatResponseBody$Message { *; }
-keep class com.nsai.notes.data.remote.ai.BaseAIAdapter$ChatResponseBody$Usage { *; }
-keep class com.nsai.notes.data.local.datastore.SettingsDataStore$Bookmark { *; }
-keep class com.nsai.notes.data.local.db.dao.NoteDao$NoteTagResult { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Markwon
-keep class org.commonmark.** { *; }
-dontwarn org.commonmark.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
