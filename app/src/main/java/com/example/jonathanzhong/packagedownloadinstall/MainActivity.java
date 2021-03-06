package com.example.jonathanzhong.packagedownloadinstall;

import android.Manifest;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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

  public void downloadFile(final Context context, final BroadcastReceiver receiver, final String url, final String fileName) {
    Log.d(TAG, "downloadFile");

    boolean apkInstalled = isPackageInstalled(APK_PACKAGE_NAME);
    if (apkInstalled) {
      Log.w(TAG, "downloadFile: APK=[" + APK_PACKAGE_NAME + "] is already installed. No downloading.");
      return;
    }

    // This should match the one in DownloadManager.Request.setDestinationUri().
    // It seems that DownloadManager cannot put file into other app's internal storage.
    // And the system Download directory is not easily accessible.
    // So let's try a public storage directory.
    // However, the file can easily be deleted.
    String apkFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName;

    File apkFile = new File(apkFilePath);
    if (apkFile.exists()) {
      Log.w(TAG, "downloadFile: apkFilePath=[" + apkFilePath + "] already exists. No downloading.");
      return;
    } else {
      Log.v(TAG, "downloadFile: apkFilePath=[" + apkFilePath + "] does not exist. Start downloading ...");
    }

    try {
      if (url != null && !url.isEmpty()) {
        Uri uri = Uri.parse(url);
        context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setMimeType(getMimeType(uri.toString()));
        request.setTitle(fileName);
        request.setDescription("Downloading APK as an attachment...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // The destination may need change:
        request.setDestinationUri(Uri.fromFile(apkFile));
        m_downloadManager.enqueue(request);
      }
    } catch (IllegalStateException e) {
      e.printStackTrace();
    }

    Log.v(TAG, "downloadFile: end");
  }

  public boolean isPackageInstalled(String targetPackage) {
    Log.d(TAG, "isPackageInstalled");

    PackageManager pm = getPackageManager();
    List<ApplicationInfo> packages = pm.getInstalledApplications(0);

    for (ApplicationInfo packageInfo : packages) {
      if (packageInfo.packageName.equals(targetPackage)) {
        return true;
      }
    }

    return false;
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

    Log.v(TAG, "openDownloadedAttachment: end");
  }

  private void openDownloadedAttachment2(final Context context, Uri attachmentUri, final String attachmentMimeType) {
    Log.d(TAG, "openDownloadedAttachment2");

    if (attachmentUri != null) {
      if (ContentResolver.SCHEME_FILE.equals(attachmentUri.getScheme())) {
        File file = new File(attachmentUri.getPath());
        //attachmentUri = FileProvider.getUriForFile(context, getApplicationContext().getPackageName(), file);
        try {
          InputStream inputStream = new FileInputStream(file);
          installPackage(context, inputStream);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        Log.e(TAG, "openDownloadAttachment2: not a file scheme !!!");
      }

      //Intent openAttachmentIntent = new Intent(Intent.ACTION_VIEW);
      //openAttachmentIntent.setDataAndType(attachmentUri, attachmentMimeType);
      //openAttachmentIntent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

      //try {
      //  context.startActivity(openAttachmentIntent);
      //} catch (ActivityNotFoundException e) {
      //  e.printStackTrace();
      //}
    } else {
      Log.e(TAG, "OpenDownloadAttachment2: attachmentUri == null !!!");
    }

    Log.v(TAG, "openDownloadedAttachment2: end");
  }

  public boolean checkStoragePermissionBeforeDownloading() {
    Log.d(TAG, "checkStoragePermissionBeforeDownloading");

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
    Log.d(TAG, "onRequestPermissionsResult");

    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.v(TAG, "onRequestPermissionsResult: Storage permission GRANTED");

      downloadFile(this, m_downloadCompleteReceiver, APK_DOWNLOAD_URL, APK_FILE_NAME);
    } else {
      Log.v(TAG, "onRequestPermissionsResult: Storage permission DENIED");
    }
  }

  /*
    public static DevicePolicyManager getDpm(Context context) {
      return (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static ComponentName getAdmin(Context context) {
      return new ComponentName(context, MyDevicePolicyReceiver.class);
    }

    public static void addMyRestrictions(Context context) {
      getDpm(context).addUserRestriction(getAdmin(context), UserManager.DISALLOW_INSTALL_APPS);
      getDpm(context).addUserRestriction(getAdmin(context), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
    }

    public static void clearMyRestrictions(Context context) {
      getDpm(context).clearUserRestriction(getAdmin(context), UserManager.DISALLOW_INSTALL_APPS);
      getDpm(context).clearUserRestriction(getAdmin(context), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
    }
  */

  public static void installPackage(Context context, InputStream inputStream) throws IOException {
    Log.d(TAG, "installPackage");

    PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
    int sessionId = packageInstaller.createSession(new PackageInstaller
        .SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));

    //openSession checks for user restrictions
    //clearMyRestrictions(context);
    PackageInstaller.Session session = packageInstaller.openSession(sessionId);
    //addMyRestrictions(context);

    long sizeBytes = 0;

    OutputStream out;
    out = session.openWrite("my_app_session", 0, sizeBytes);

    int total = 0;
    byte[] buffer = new byte[65536];
    int c;
    while ((c = inputStream.read(buffer)) != -1) {
      total += c;
      out.write(buffer, 0, c);
    }
    session.fsync(out);
    inputStream.close();
    out.close();

    Log.v(TAG, "installPackage: total=[" + total + "]");

    // fake intent
    //IntentSender statusReceiver = null;
    Intent intent = new Intent(context, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
        1337111117, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    session.commit(pendingIntent.getIntentSender());
    session.close();

    Log.v(TAG, "installPackage: end");
  }
}