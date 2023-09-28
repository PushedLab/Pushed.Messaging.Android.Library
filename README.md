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
	implementation 'com.github.PushedLab:Pushed.Messaging.Android.Library:1.0.1'
    }
``` 

**Step 3.** Create your own Class extends BackgroundService class and override onBackgroundMessage(JSONObject) 
for handle messages even when your application is not running.

```kotlin
class MyBackgroundService:BackgroundService() {
    override fun onBackgroundMessage(message: JSONObject) {
        Log.d("Mybackground","MyBackground message: $message")
    }
```

**Step 4.** Add the following to your app's AndroidManifest.xml

```xml
    <application>
    ...
        <service
            android:enabled="true"
            android:exported="true"
            android:name=".MyBackgroundService"
            android:stopWithTask="false" />
    ...
    </application>
```

### Implementation

For init library you need create instace of PushedService 

```kotlin
        pushedService= PushedService(this,"Pushed","Service is active",
            ru.pushed.messaginglibrary.R.mipmap.ic_bg_service_small,MyBackgroundService::class.java)
```

```kotlin
// context - Context
// title,body,icon - params of service notification
// backgroundServiceClass - your own BackgroundService class
PushedService(private val context : Context, private val title:String, private val body:String, private val icon:Int, private val backgroundServiceClass: Class<*>);

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



