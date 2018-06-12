package com.example.jonathanzhong.packagedownloadinstall;

import android.Manifest;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  private static final String APK_DOWNLOAD_URL =
      "https://github.com/jonathanzho/resFiles/raw/master/apk/PayJoyPackageMonitor.apk";
  private static final String APK_FILE_NAME = "PayJoyPackageMonitor.apk";
  private static final String APK_PACKAGE_NAME = "com.example.jonathan.payjoypackagemonitor";
  private static final String ACTION_INSTALL_COMPLETE = "com.example.jonathanzhong.packagedownloadinstall.INSTALL_COMPLETE";

  ProgressDialog pd;

  private long m_downloadId = 0;
  private DownloadManager m_downloadManager;
  private String m_downloadedApkFilePath;
  private Uri m_downloadedApkUri;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate: start");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });

    // Customization starts from here.

    // Get runtime storage permission for API 23 and up:
    //isStoragePermissionGranted();

    m_downloadedApkFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + APK_FILE_NAME;
    m_downloadedApkUri = Uri.parse("file://" + m_downloadedApkFilePath);

    BroadcastReceiver receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
          long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
          DownloadManager.Query query = new DownloadManager.Query();
          query.setFilterById(MainActivity.this.m_downloadId);
          Cursor c = m_downloadManager.query(query);

          if (c.moveToFirst()) {
            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

            if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
              Log.v(TAG, "download SUCCESSFUL");

              Log.v(TAG, "displaying APK...");

              Intent displayIntent = new Intent();
              displayIntent.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
              startActivity(displayIntent);

              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              File downloadedApkFile = new File(m_downloadedApkFilePath);
              downloadedApkFile.setReadable(true, false);

              Log.v(TAG, "installing [" + m_downloadedApkFilePath + "]...");

              InputStream downloadedApkInputStream = null;
              try {
                downloadedApkInputStream = new FileInputStream(downloadedApkFile);
              } catch (IOException e) {
                e.printStackTrace();
              }

              try {
                installPackage(getApplicationContext(), downloadedApkInputStream, APK_PACKAGE_NAME);
              } catch (IOException e) {
                e.printStackTrace();
              }

              unregisterReceiver(this);
              finish();
            } else {
              Log.e(TAG, "download UNSUCCESSFUL");
            }
          }
        }
      }
    };

    registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Clean-up before download
    File file = new File(m_downloadedApkFilePath);
    if (file.exists()) {
      Log.v(TAG,"onCreate: deleting old APK from the download folder...");
      file.delete();  // Does not work ???
    } else {
      Log.v(TAG,"onCreate: no old APK found");
    }

    // Request download
    m_downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(APK_DOWNLOAD_URL));
    m_downloadId = m_downloadManager.enqueue(request);

    Log.d(TAG, "onCreate: end");
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

  public static boolean installPackage(Context context, InputStream inputStream, String packageName) throws IOException {
    Log.d(TAG, "installPackage: start");

    PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
    PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    params.setAppPackageName(packageName);
    int sessionId = packageInstaller.createSession(params);
    PackageInstaller.Session session = packageInstaller.openSession(sessionId);
    OutputStream outputStream = session.openWrite("COSU", 0, -1);
    byte[] buffer = new byte[65536];
    int count;
    while ((count = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, count);
    }
    session.fsync(outputStream);
    inputStream.close();
    outputStream.close();

    session.commit(createIntentSender(context, sessionId));

    Log.d(TAG, "installPackage: end");

    return true;
  }

  private static IntentSender createIntentSender(Context context, int sessionId) {
    PendingIntent pendingIntent = PendingIntent.getBroadcast(
        context,
        sessionId,
        new Intent(ACTION_INSTALL_COMPLETE),
        0);

    return pendingIntent.getIntentSender();
  }


  public boolean isStoragePermissionGranted() {
    if (Build.VERSION.SDK_INT >= 23) {
      if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
          == PackageManager.PERMISSION_GRANTED) {
        Log.v(TAG, "isStoragePermissionGranted: API 23 and up: YES");
        return true;
      } else {
        Log.v(TAG, "isStoragePermissionGranted: API 23 and up: NO. requesting...");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        return false;
      }
    } else {
      Log.v(TAG, "isStoragePermissionGranted: API 22 and down: YES");
      return true;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int grantResults[]) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      Log.v(TAG, "onRequestPermissionsResult: Storage permission GRANTED");

      // Start downloading and installing:
      DownloadInstallPackageTask dipTask = new DownloadInstallPackageTask();
      dipTask.execute(APK_DOWNLOAD_URL);
    } else {
      Log.v(TAG, "onRequestPermissionsResult: Storage permission DENIED");
    }
  }

  private class DownloadInstallPackageTask extends AsyncTask<String, String, String> {
    private final String TAG2 = DownloadInstallPackageTask.class.getSimpleName();

    int styatus = 0;

    protected void onPreExecute() {
      Log.d(TAG2, "onPreExecute");

      super.onPreExecute();

      pd = new ProgressDialog(MainActivity.this);
      pd.setMessage("Please wait!");
      pd.setCancelable(false);
      pd.show();
    }

    @Override
    protected String doInBackground(String... params) {
      Log.d(TAG2, "doInBackground");

      HttpURLConnection connection;

      try {
        URL url = new URL(params[0]);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.connect();

        File folder = new File(Environment.getExternalStorageDirectory() + "/temp");
        boolean success = true;
        if (!folder.exists()) {
          success = folder.mkdir();
        }
        if (!success) {
          Log.e(TAG2, "doInBackground: failed making folder temp on external storage");
          return null;
        }

        File outputFile = new File(folder, "temp.apk");
        if (outputFile.exists()) {
          outputFile.delete();
          Log.v(TAG2, "doInBackground: deleted existing temp.apk");
        }

        FileOutputStream fos = new FileOutputStream(outputFile);

        InputStream is = connection.getInputStream();

        byte[] buffer = new byte[1024];
        int len1 = 0;

        while ((len1 = is.read(buffer)) != -1) {
          fos.write(buffer, 0, len1);
        }

        fos.flush();
        fos.close();
        is.close();

        Log.v(TAG2, "doInBackground: downloaded temp.apk");

        // Install
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(outputFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
      } catch (Exception e) {
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      Log.d(TAG2, "onPostExecute: result=[" + result + "]");

      super.onPostExecute(result);

      // Dismiss progress dialog
      if (pd.isShowing()) {
        pd.dismiss();
      }
    }

  }
}
