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
# 1. FIXED FIREBASE SPECIFIC KEEP RULES (No Broad Wildcards Warning)
# -------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Target only components that utilize specific serializable properties
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
}

# Dynamic Model Classes Safeguard (Protects Firestore JSON deserialization architecture)
-keepclassmembers class com.example.nursingstudio.data.model.** {
    <fields>;
    <methods>;
}

# -------------------------------------------------------------------------
# 2. FIXED ANDROIDX & MATERIAL UI SPECIFIC KEEP RULES (Warnings Fully Resolved)
# -------------------------------------------------------------------------
# Instead of keeping whole library packages, we only safeguard layout reflections
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity
-dontwarn androidx.**
-dontwarn com.google.firebase.**

# -------------------------------------------------------------------------
# 3. VIEW BINDING HARDENING
# -------------------------------------------------------------------------
-keep class com.example.nursingstudio.databinding.** { *; }

# -------------------------------------------------------------------------
# 4. LOG STRIPPING FOR INFRASTRUCTURE SECURITY
# -------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}