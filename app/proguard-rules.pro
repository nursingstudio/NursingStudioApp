# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# -------------------------------------------------------------------------
# 1. GENERAL SECURITY & OPTIMIZATION (2026 Standard)
# -------------------------------------------------------------------------
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Function names ko obfuscate (uljha) do
-repackageclasses ''
-allowaccessmodification

# Variable names aur methods ko 'a, b, c' me badal do
-useuniqueclassmembernames

# -------------------------------------------------------------------------
# 2. FIREBASE PROTECTION (Crucial for Auth & Firestore)
# -------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses
# --- FIX: Google/Firebase Optimized Keep Rules ---
# Poori library ke bajaye sirf annotations aur internals ko bachayein
-keep class com.google.firebase.** {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn com.google.firebase.**

# Model classes (Jo data Firestore se aata hai use protect karein)
# Replace 'your.package.name' with com.example.nursingstudio
-keepclassmembers class com.example.nursingstudio.data.model.** {
    <fields>;
    <methods>;
}

# -------------------------------------------------------------------------
# 3. MATERIAL DESIGN & ANDROIDX (UI Security)
# -------------------------------------------------------------------------
# --- FIX: Material & AndroidX (Strict Scope) ---
# In libraries ke liye full keep ki zaroorat nahi hoti, R8 inhe auto-handle karta hai
# Hum sirf wahi bacha rahe hain jo reflection use karte hain
-keep class com.google.android.material.** {
    public protected *;
}
-keep class androidx.appcompat.widget.** { *; }
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Activity
-dontwarn androidx.**

# -------------------------------------------------------------------------
# 4. VIEW BINDING PROTECTION
# -------------------------------------------------------------------------
-keep class com.example.nursingstudio.databinding.** { *; }

# -------------------------------------------------------------------------
# 5. REMOVE LOGS FOR PRODUCTION (World-Class Cleanliness)
# -------------------------------------------------------------------------
# Production app me koi bhi Log message nahi dikhna chahiye (Hacking risk)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}