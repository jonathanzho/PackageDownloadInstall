package com.example.jonathanzhong.packagedownloadinstall;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

        DownloadInstallPackageTask dipTask = new DownloadInstallPackageTask();
        dipTask.execute(APK_DOWNLOAD_URL);

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
                intent.setDataAndType(Uri.fromFile(outputFile),"application/vnd.android.package-archive");
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

/*
    ProgressDialog pd;
    TextView tvHW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: start");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Customization starts from here

        tvHW = (TextView) findViewById(R.id.tvHW);

        JsonTask jsonTask = new JsonTask();
        jsonTask.execute(TEST_JSON_URL);

        Log.d(TAG, "onCreate: end");
    }

private class JsonTask extends AsyncTask<String, String, String> {
    private final String TAG2 = JsonTask.class.getSimpleName();

    @Override
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

        String result = null;

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line);

                Log.d(TAG2, "line>" + line);
            }

            result = buffer.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG2, "onPostExecute: result=[" + result + "]");

        super.onPostExecute(result);

        // Dismiss progress dialog
        if (pd.isShowing()) {
            pd.dismiss();
        }

        // Set up Gson
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        // Deserialize JSON
        UserProfile[] userProfiles = gson.fromJson(result, UserProfile[].class);

        int numUsers = userProfiles.length;

        Log.d(TAG2, "numUsers=[" + numUsers + "]");

        String user0Name = userProfiles[0].getUserName();

        Log.d(TAG2, "user0Name=[" + user0Name + "]");

        // Display
        tvHW.setText(result);
        tvHW.setText(user0Name);
    }
}
}
*/