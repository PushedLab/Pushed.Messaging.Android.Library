# Pushed Messaging Android library

Android library to use the Pushed Messaging.

To learn more about Pushed Messaging, please visit the [Pushed website](https://pushed.dev)

## Getting Started

**Step 1.** Add required repositories to your root `settings.gradle` file:

```gradle
// settings.gradle
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
    implementation 'com.github.PushedLab:Pushed.Messaging.Android.Library:1.4.7' // Check for the latest version
}
```

## Configuring Push Service Providers

By default, the library includes dependencies for FCM, HPK, and RuStore Push for backward compatibility. If you don't need all of them, you can easily exclude them.

**This is the recommended approach for optimizing your app size.**

### Excluding Unused Providers

In your app's `build.gradle`, modify the library dependency to exclude the providers you don't need.

```gradle
dependencies {
    implementation('com.github.PushedLab:Pushed.Messaging.Android.Library:1.4.7') {
        // Example: Exclude HPK and RuStore, keeping only FCM
        exclude group: 'com.huawei.hms', module: 'push'
        exclude group: 'ru.rustore.sdk', module: 'pushclient'
    }

    // If you need to exclude FCM, you have to exclude its components too
    implementation('com.github.PushedLab:Pushed.Messaging.Android.Library:1.4.7') {
        exclude group: 'com.google.firebase', module: 'firebase-messaging'
    }
}
```

When you exclude a provider, remember to also set its corresponding flag to `false` during `PushedService` initialization.

### Provider-specific Setup

If you are using a provider, you still need to perform its specific setup (e.g., adding plugins and config files).

#### Firebase Cloud Messaging (FCM)

1.  **Plugin:** Apply the Google Services plugin in `app/build.gradle` (`id 'com.google.gms.google-services'`) and add the classpath to your root `build.gradle`.
2.  **Config File:** Place your `google-services.json` file in the `app/` directory.

#### Huawei Push Kit (HPK)

1.  **Plugin:** Apply the AGConnect plugin in `app/build.gradle` (`id 'com.huawei.agconnect'`) and add the classpath to your root `build.gradle`.
2.  **Config File:** Place your `agconnect-services.json` file in the `app/` directory.

#### RuStore Push

1.  **Config:** Add your project ID to `AndroidManifest.xml` inside the `<application>` tag.
    ```xml
    <meta-data
        android:name="ru.rustore.sdk.pushclient.project_id"
        android:value="Your RuStore project ID" />
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

Create an instance of `PushedService` in your Activity or Application class. The library automatically detects which push providers are available based on the dependencies you've included in your project.

```kotlin
// In your Activity's onCreate
pushedService = PushedService(
    context = this,
    messageReceiverClass = MyMessageReceiver::class.java,
    channel = "messages" // Notification channel for pushes shown by the library
)
```

**Constructor Parameters:**
*   `context`: `Context` - The application context.
*   `messageReceiverClass`: `Class<*>?` - Your custom receiver for background messages.
*   `channel`: `String?` - The ID of the notification channel to use. If `null`, the library will not show its own notifications.
*   `enableLogger`: `Boolean` - Enables local logging for debugging.
*   `askPermissions`: `Boolean` - If `true`, the library will automatically request permissions for notifications and background work on the first launch.
*   `applicationId`: `String?` - Your Application ID from the Pushed panel.

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



