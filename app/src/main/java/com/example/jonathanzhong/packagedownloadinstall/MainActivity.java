package com.example.jonathanzhong.packagedownloadinstall;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();

  private static final String APK_DOWNLOAD_URL =
      "https://github.com/jonathanzho/resFiles/raw/master/apk/PayJoyPackageMonitor.apk";

  ProgressDialog pd;

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
    isStoragePermissionGranted();

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
