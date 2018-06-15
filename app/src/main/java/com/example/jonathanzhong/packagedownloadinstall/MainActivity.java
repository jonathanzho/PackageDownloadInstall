package com.example.jonathanzhong.packagedownloadinstall;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

import java.io.File;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  private static final String APK_PACKAGE_NAME = "com.example.jonathan.payjoypackagemonitor";
  private static final String APK_FILE_NAME = "PayJoyPackageMonitor.apk";
  private static final String APK_DOWNLOAD_URL = "https://github.com/jonathanzho/resFiles/raw/master/apk/" + APK_FILE_NAME;

  private DownloadManager m_downloadManager;
  private BroadcastReceiver m_downloadCompleteReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate: start");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });

    // Customization starts from here.

    m_downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

    m_downloadCompleteReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
          long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
          openDownloadedAttachement(getApplicationContext(), downloadId);
        }
      }
    };

    boolean storageGranted = checkStoragePermissionBeforeDownloading();

    Log.v(TAG, "onCreate: storageGranted=[" + storageGranted + "]");

    Log.v(TAG, "onCreate: end");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  // New methods
  // ===========

  public void downloadFile(final Activity activity, final BroadcastReceiver receiver, final String url, final String fileName) {
    Log.d(TAG, "downloadFile");

    try {
      if (url != null && !url.isEmpty()) {
        Uri uri = Uri.parse(url);
        activity.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setMimeType(getMimeType(uri.toString()));
        request.setTitle(fileName);
        request.setDescription("Downloading attachment...");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        m_downloadManager.enqueue(request);
      }
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }
  }

  private String getMimeType(String url) {
    Log.d(TAG, "getMimeType");

    String type = null;
    String extension = MimeTypeMap.getFileExtensionFromUrl(url);

    if (extension != null) {
      MimeTypeMap mime = MimeTypeMap.getSingleton();
      type = mime.getMimeTypeFromExtension(extension);
    }

    return type;
  }

  private void openDownloadedAttachement(final Context context, final long downloadId) {
    Log.d(TAG, "openDownloadedAttachment");

    DownloadManager.Query query = new DownloadManager.Query();
    query.setFilterById(downloadId);
    Cursor cursor = m_downloadManager.query(query);

    if (cursor.moveToFirst()) {
      int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
      String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
      String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));

      if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL && downloadLocalUri != null) {
        openDownloadedAttachment2(context, Uri.parse(downloadLocalUri), downloadMimeType);
      }
    }

    cursor.close();
  }

  private void openDownloadedAttachment2(final Context context, Uri attachmentUri, final String attachmentMimeType) {
    Log.d(TAG, "openDownloadedAttachment2");

    if (attachmentUri != null) {
      if (ContentResolver.SCHEME_FILE.equals(attachmentUri.getScheme())) {
        File file = new File(attachmentUri.getPath());
        attachmentUri = FileProvider.getUriForFile(context, getApplicationContext().getPackageName(), file);
      }

      Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
      openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType);
      openAttachmentIntent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

      try {
        context.startActivity(openAttachmentIntent);
      } catch (ActivityNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean checkStoragePermissionBeforeDownloading() {
    if (Build.VERSION.SDK_INT >= 23) {
      if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
          == PackageManager.PERMISSION_GRANTED) {
        Log.v(TAG, "checkStoragePermissionBeforeDownloading: API 23 and up: YES");

        downloadFile(this, m_downloadCompleteReceiver, APK_DOWNLOAD_URL, APK_FILE_NAME);

        return true;
      } else {
        Log.v(TAG, "checkStoragePermissionBeforeDownloading: API 23 and up: NO. requesting...");

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        return false;
      }
    } else {
      Log.v(TAG, "checkStoragePermissionBeforeDownloading: API 22 and down: YES");

      downloadFile(this, m_downloadCompleteReceiver, APK_DOWNLOAD_URL, APK_FILE_NAME);

      return true;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int grantResults[]) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.v(TAG, "onRequestPermissionsResult: Storage permission GRANTED");

      downloadFile(this, m_downloadCompleteReceiver, APK_DOWNLOAD_URL, APK_FILE_NAME);
    } else {
      Log.v(TAG, "onRequestPermissionsResult: Storage permission DENIED");
    }
  }
}
