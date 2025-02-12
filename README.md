# Pushed Messaging Android library

Android library to use the Pushed Messaging.

To learn more about Pushed Messaging, please visit the [Pushed website](https://pushed.ru)

## Getting Started

If you are using Gradle to get a GitHub project into your build, you will need to:

**Step 1.** Add it in your root build.gradle at the end of repositories 

```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

**Step 2.** Add the dependency

```gradle
    dependencies {
	implementation 'com.github.PushedLab:Pushed.Messaging.Android.Library:1.3.0'
    }
``` 

**Step 3.** Create your own Class extends MessageReceiver class and override onBackgroundMessage(Context?,JSONObject) 
for handle messages even when your application is not running.

```kotlin
class MyMessageReceiver : MessageReceiver() {
    override fun onBackgroundMessage(context: Context?,message: JSONObject) {
        Log.d("Mybackground","MyBackground message: $message")
    }
```

**Step 4.** Add the following to your app's AndroidManifest.xml

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

For support Fcm, you must follow these steps:

**Step 1.** Add it in your root build.gradle add this dependencies 

```gradle
buildscript {
...
    dependencies {
        classpath 'com.google.gms:google-services:4.4.2'
        ...
    }
}

```

**Step 2.** Place your google-services.json in Android/app folder

**Step 3.** Add it in your app/build.gradle add this plugins 

```gradle
...
apply plugin: 'com.google.gms.google-services'
```

For support Hpk, you must follow these steps:

**Step 1.** Add it in your root build.gradle add this 

```gradle
buildscript {
...
    repositories {
        ...
        maven { url 'https://developer.huawei.com/repo/' }
    }
    dependencies {
        classpath 'com.huawei.agconnect:agcp:1.9.1.300'
        ...
    }
}
allprojects {
    repositories {
        ...
        maven { url 'https://developer.huawei.com/repo/' }
    }
}

```

**Step 2.** Place your agconnect-services.json in Android/app folder

**Step 3.** Add it in your app/build.gradle add this 

```gradle
...
dependencies {
    ...
    implementation 'com.huawei.hms:push:6.12.0.300'
}
...
apply plugin: 'com.huawei.agconnect' 
```

On Android, to support RuStore, you need to add the following to your AndroidManifest.xml

```xml
    <application>
    ...
        <meta-data
            android:name="ru.rustore.sdk.pushclient.project_id"
            android:value="Your RuStore project ID" />
    ...
    </application>
```


### Implementation

For init library you need create instace of PushedService 

```kotlin
        pushedService= PushedService(this,MyMessageReceiver::class.java)
```

```kotlin
// context - Context
// messageReceiverClass - your own messageReceiverClass class
PushedService(private val context : Context, messageReceiverClass: Class<*>?);

```

To start a service or bind to an active service, you need to call PushedService.start.

```kotlin
    override fun onResume() {
        super.onResume()
        // token - To send a message to a specific user, you need to know his Client token.
        token=pushedService.start(){message ->
            Log.d("MyActivity","Message received: $message")
            //return true if message handled.
            //if you return false then service call onBackgroundMessage.
            true
        }
        Log.d("MyActivity",Client token: $token")

    }
```

```kotlin
//OnMessage - Function for handle messages if you activity in foreground
//return Client token
PushedService.start(onMessage:(JSONObject)->Boolean):String?
```

For the library to work correctly, you need to call PushedService.unbind manually when the activity leaves the foreground.

```kotlin
    override fun onStop() {
        pushedService.unbindService()
        super.onStop()
    }
```



