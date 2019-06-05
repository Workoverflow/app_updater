package app.example.ru;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * Class for update app without Google Play
 */

public class AppUpdate {

    /**
     * Create global object of AsyncHttpClient
     */
    private static AsyncHttpClient client = new AsyncHttpClient();

    /**
     * Application context
     */
    private Context context;

    /**
     * Remote url address to update.json
     */
    private String UPDATE_INFO_URL;

    /**
     * Remote url address to update.apk
     */
    private String UPDATE_APK_URL;

    /**
     * Current version of application (set by BuildConfig.VERSION_NAME)
     */
    private String CURRENT_VERSION;

    /**
     * Latest version of application (set by update.json -> latestVersion)
     */
    private String LATEST_VERSION;

    /**
     * Custom "update available" dialog title
     */
    private String DIALOG_TITLE;

    /**
     * Custom "update available" dialog content
     */
    private String DIALOG_CONTENT;

    /**
     * Custom "update available" dialog icon
     */
    private int DIALOG_ICON;

    /**
     * Custom text for positive button
     */
    private String BUTTON_UPDATE_TEXT;

    /**
     * Custom text for negative button
     */
    private String BUTTON_CANCEL_TEXT;

    /**
     * Custom title form download process
     */
    private String DOWNLOAD_TITLE;

    /**
     * Optional params for method check(). If params set, POST-request will extend with $_POST params
     */
    private RequestParams params = null;

    /**
     * Public constructor
     * @param context
     */
    public AppUpdate(Context context) {
        this.context = context;
    }

    /**
     * Set url to update.json (ex. https://example.com/updater/latest/update.json)
     * @param url
     * @return AppUpdate
     */
    public AppUpdate setUpdateUrl(String url) {
        this.UPDATE_INFO_URL = url;
        return this;
    }

    /**
     * Set current version of application (ex. BuildConfig.VERSION_NAME)
     * @param version
     * @return AppUpdate
     */
    public AppUpdate setCurrentVersion(String version) {
        this.CURRENT_VERSION = version;
        return this;
    }

    /**
     * Set custom "update available" dialog title
     * @param title
     * @return AppUpdate
     */
    public AppUpdate setDialogTitle(String title) {
        this.DIALOG_TITLE = title;
        return this;
    }

    /**
     * Set custom "update available" dialog context
     * @param content
     * @return AppUpdate
     */
    public AppUpdate setDialogContent(String content) {
        this.DIALOG_CONTENT = content;
        return this;
    }

    /**
     * Set custom text for positive button
     * @param text
     * @return AppUpdate
     */
    public AppUpdate setButtonUpdateText(String text) {
        this.BUTTON_UPDATE_TEXT = text;
        return this;
    }

    /**
     * Set custom text for negative button
     * @param text
     * @return AppUpdate
     */
    public AppUpdate setButtonCancelText(String text) {
        this.BUTTON_CANCEL_TEXT = text;
        return this;
    }

    /**
     * Set custom "update available" dialog icon
     * @param icon
     * @return AppUpdate
     */
    public AppUpdate setDialogIcon(int icon) {
        this.DIALOG_ICON = icon;
        return this;
    }

    /**
     * Set custom download process title
     * @param title
     * @return AppUpdate
     */
    public AppUpdate setDownloadTitle(String title) {
        this.DOWNLOAD_TITLE = title;
        return this;
    }

    /**
     * Set optional POST params. For example, append token to request or any POST data
     * @param data
     * @return AppUpdate
     */
    public AppUpdate setCheckParams(HashMap<String, String> data) {

        RequestParams params = new RequestParams();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            params.put(key, value);
        }
        this.params = params;
        return this;
    }

    /**
     * Make async https request to remote URL and get information about
     * latest version and apk update URL
     * @return AppUpdate
     */
    public AppUpdate check() {

        client.post(this.UPDATE_INFO_URL, this.params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String status = response.getString("status");
                    if (status.equals("success")) {
                        LATEST_VERSION = response.getString("latestVersion");
                        UPDATE_APK_URL = response.getString("url");

                        if (!CURRENT_VERSION.equals(LATEST_VERSION)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(DIALOG_TITLE);
                            builder.setMessage(DIALOG_CONTENT);
                            builder.setCancelable(false);
                            builder.setIcon(DIALOG_ICON);
                            builder.setPositiveButton(BUTTON_UPDATE_TEXT, (dialog, which) -> {
                                dialog.cancel();
                                install(UPDATE_APK_URL);
                            });
                            builder.setNegativeButton(BUTTON_CANCEL_TEXT, (dialog, which) -> dialog.cancel());
                            builder.show();
                        }
                    }
                } catch (JSONException e) {
                    Log.e("ERROR", e.getMessage());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable error) {
            }

        });

        return this;
    }

    /**
     * Make request to UPDATE_APK_URL, download it with DownloadManager and start APK installation
     * @param url
     */
    private void install(String url) {
        String destination = context.getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/update.apk";
        Common common = new Common();

        final File file = new File(destination);
        if (file.exists()) {
            file.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(DOWNLOAD_TITLE);
        request.setDestinationUri(Uri.fromFile(file));

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                    intent.setData(common.getUriFromFile(ctxt, file));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } else {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    install.setDataAndType(common.getUriFromFile(ctxt, file), manager.getMimeTypeForDownloadedFile(downloadId));
                    context.startActivity(install);
                }
                context.unregisterReceiver(this);
            }
        };

        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}
