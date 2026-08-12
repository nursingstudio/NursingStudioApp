# =========================================================================
# NURSING STUDIO - R8 STRICT OPTIMIZATION MATRIX (2026 GOLD STANDARD)
# =========================================================================

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-repackageclasses ''
-allowaccessmodification

# -------------------------------------------------------------------------
# 1. ANNOTATION & METADATA PRESERVATION
# -------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# -------------------------------------------------------------------------
# 2. FIREBASE & DATA MODELS (Targeted Member Keep Rules)
# -------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Preserve serialization fields for app data models
-keepclassmembers class com.example.nursingstudio.data.model.** {
    <fields>;
    public <init>(...);
}

# -------------------------------------------------------------------------
# 3. ANDROIDX, VIEWS & BINDING
# -------------------------------------------------------------------------
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity

-keep class com.example.nursingstudio.databinding.** { *; }

# -------------------------------------------------------------------------
# 4. HILT & VIEWMODEL REFLECTION (Scoped to Annotations & Constructors)
# -------------------------------------------------------------------------
# Keep constructors for ViewModels so default and custom factories can instantiate them
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep constructors annotated with @Inject or @HiltViewModel specifically
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# -------------------------------------------------------------------------
# 5. LOG STRIPPING FOR INFRASTRUCTURE SECURITY & PERFORMANCE
# -------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-dontwarn androidx.**
-dontwarn com.google.firebase.**