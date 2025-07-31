# Pushed Messaging Android library

Android library to use the Pushed Messaging.

To learn more about Pushed Messaging, please visit the [Pushed website](https://pushed.dev)

## Getting Started

**Step 1.** Add JitPack to your root `build.gradle` file:

```gradle
// settings.gradle or build.gradle (for older Gradle versions)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url 'https://developer.huawei.com/repo/' } // Needed for HPK
    }
}
```

**Step 2.** Add the library dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.PushedLab:Pushed.Messaging.Android.Library:1.4.6' // Check for the latest version
}
```

## Adding Push Service Providers (Optional)

The library is designed to be modular. You only need to include the dependencies for the push services you intend to use.

### Firebase Cloud Messaging (FCM)

1.  Add the FCM dependency to your `app/build.gradle`:
    ```gradle
    dependencies {
        ...
        implementation 'com.google.firebase:firebase-messaging:24.1.0'
    }
    ```

2.  Apply the Google Services plugin in your `app/build.gradle`:
    ```gradle
    plugins {
        ...
        id 'com.google.gms.google-services'
    }
    ```

3.  Add the plugin classpath to your root `build.gradle`:
    ```gradle
    buildscript {
        dependencies {
            classpath 'com.google.gms:google-services:4.4.2' // Use the latest version
        }
    }
    ```

4.  Place your `google-services.json` file in the `app/` directory.

### Huawei Push Kit (HPK)

1.  Add the HPK dependency to your `app/build.gradle`:
    ```gradle
    dependencies {
        ...
        implementation 'com.huawei.hms:push:6.12.0.300'
    }
    ```

2.  Apply the AGConnect plugin in your `app/build.gradle`:
    ```gradle
    plugins {
        ...
        id 'com.huawei.agconnect'
    }
    ```

3.  Add the plugin classpath to your root `build.gradle`:
    ```gradle
    buildscript {
        dependencies {
            classpath 'com.huawei.agconnect:agcp:1.9.1.300' // Use the latest version
        }
    }
    ```
    *(Note: The Huawei repository should already be added in Step 1 of "Getting Started")*

4.  Place your `agconnect-services.json` file in the `app/` directory.

### RuStore Push

1.  Add the RuStore Push Client dependency to your `app/build.gradle`:
    ```gradle
    dependencies {
        ...
        implementation 'ru.rustore.sdk:pushclient:6.3.0'
    }
    ```

2.  Add your project ID to `AndroidManifest.xml`:
    ```xml
    <application>
        ...
        <meta-data
            android:name="ru.rustore.sdk.pushclient.project_id"
            android:value="Your RuStore project ID" />
        ...
    </application>
    ```

## Basic Setup

**Step 1.** Create a `MessageReceiver` to handle background messages.

```kotlin
class MyMessageReceiver : MessageReceiver() {
    override fun onBackgroundMessage(context: Context?, message: JSONObject) {
        Log.d("MyMessageReceiver", "Background message received: $message")
    }
}
```

**Step 2.** Register your receiver in `AndroidManifest.xml`.

```xml
<application>
    ...
    <receiver
        android:name=".MyMessageReceiver"                                               
        android:enabled="true"
        android:exported="true">
        <intent-filter>
            <action android:name="ru.pushed.action.MESSAGE" />
        </intent-filter>
    </receiver>
    ...
</application>
```

## Implementation

### Initializing the Service

Create an instance of `PushedService` in your Activity or Application class. Use the flags `enableFcm`, `enableHpk`, and `enableRuStore` to control which push providers are initialized.

```kotlin
// In your Activity's onCreate
pushedService = PushedService(
    context = this,
    messageReceiverClass = MyMessageReceiver::class.java,
    channel = "messages", // Notification channel for pushes shown by the library
    enableFcm = true,     // Initialize FCM if its dependency is present
    enableHpk = true,     // Initialize HPK if its dependency is present
    enableRuStore = true  // Initialize RuStore if its dependency is present
)
```

**Constructor Parameters:**
*   `context`: `Context` - The application context.
*   `messageReceiverClass`: `Class<*>?` - Your custom receiver for background messages.
*   `channel`: `String?` - The ID of the notification channel to use. If `null`, the library will not show its own notifications.
*   `enableLogger`: `Boolean` - Enables local logging for debugging.
*   `askPermissions`: `Boolean` - If `true`, the library will automatically request permissions for notifications and background work on the first launch.
*   `applicationId`: `String?` - Your Application ID from the Pushed panel.
*   `enableFcm`: `Boolean` - Enables initialization of Firebase Cloud Messaging.
*   `enableHpk`: `Boolean` - Enables initialization of Huawei Push Kit.
*   `enableRuStore`: `Boolean` - Enables initialization of RuStore Push.

### Handling Foreground Messages

To receive messages while your app is in the foreground, call `pushedService.start()` in `onResume`.

```kotlin
override fun onResume() {
    super.onResume()
    // The token is your client token for sending pushes to this specific user.
    val token = pushedService.start { message ->
        Log.d("MyActivity", "Foreground message received: $message")
        // Return true if the message is handled.
        // If you return false, the library will show a notification
        // and pass the message to your onBackgroundMessage receiver.
        true
    }
    Log.d("MyActivity", "Client token: $token")
}
```

### Unbinding the Service

To prevent memory leaks, unbind the service when your activity is no longer in the foreground.

```kotlin
override fun onStop() {
    pushedService.unbindService()
    super.onStop()
}
```

### Requesting Permissions Manually

If you set `askPermissions = false` during initialization, you can request permissions manually at any time.

```kotlin
// askNotification: Asks for permission to display notifications.
// askBackgroundWork: Asks for permission to ignore battery optimizations.
pushedService.askPermissions(askNotification = true, askBackgroundWork = true)
```



