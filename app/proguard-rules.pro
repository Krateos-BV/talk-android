#
# Nextcloud Talk - Android Client
#
# SPDX-FileCopyrightText: 2017-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: GPL-3.0-or-later
#
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

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# Models are persisted as JSON in the Room database and preferences, so field names must survive an
# upgrade from a non-minified build.
-keepclassmembers class com.nextcloud.talk.** { <fields>; <init>(...); }

# LoganSquare finds a model's mapper by appending $$JsonObjectMapper to the model's class name.
-keep @com.bluelinelabs.logansquare.annotation.JsonObject class *
-keep class **$$JsonObjectMapper { *; }

# Parceler finds the generated wrapper by appending $$Parcelable to the model's class name.
-keep @org.parceler.Parcel class *
-keep class **$$Parcelable { *; }

-keep class org.webrtc.** { *; }
-keep class net.zetetic.** { *; }

# WorkManager stores worker class names; jobs queued before an upgrade must still resolve.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

-keepclassmembers class * { @org.greenrobot.eventbus.Subscribe <methods>; }
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
