# Android app updater

Simple solution to update your android app from you server without Play Market

**Dependencies**
> implementation 'com.loopj.android:android-async-http:1.4.9'

**How to use?**

Create folder on your server and put inside latests update.apk with update.json.
When you need to update you app, update *latestVersion* in update.json and upgrade new APK to server.

```json
{
  "status": "success",
  "latestVersion": "1.0",
  "url": "https://example.com/update/update.apk"
}
```

The AppUpdate constructor can be called from your activity:

```java
new AppUpdate(this)
    .setUpdateUrl(<insert URL to update.json>)
    .setCurrentVersion(BuildConfig.VERSION_NAME)
    .setDialogTitle("An update is available")
    .setDialogContent("An update is available for the app. Please update to work correctly.")
    .setButtonUpdateText("Update")
    .setButtonCancelText("Remind me later")
    .setDownloadTitle("Application update")
    .setDialogIcon(R.drawable.ic_cloud_download_white_48px)
    .check();
```

If you want to send POST params to server, append *.setCheckParams(<post_data>)* before *.check()*
```java
  HashMap<String, String> params = new HashMap<>();
  params.put("key", "value");
  
  new AppUpdate(this)
    ...
  .setCheckParams(params)
  .check();
```

Updater gets *latestVersion* and compare it with BuildConfig.VERSION_NAME. 
If update is available, user will be notify with Dialog. Updater can download
APK file and run system instalator, which remove current application and install new in background.
  
   

