package com.example.top10downloader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView listApps;
    private String feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private String oldURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private String oldData = "";
    private String oldLimit = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Starting Async Activity");
        listApps = findViewById(R.id.xmlListView);

        if (savedInstanceState != null) {
            feedURL = savedInstanceState.getString(oldURL);
            feedLimit = savedInstanceState.getInt(oldLimit);
        }

        downloadUrl(String.format(feedURL, feedLimit));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState: old URL is  " + oldURL);
        outState.putString(oldURL, feedURL);
        outState.putInt(oldLimit, feedLimit);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);
        if (feedLimit == 10) {
            menu.findItem(R.id.mnu10).setChecked(true);
        } else
            menu.findItem(R.id.mnu25).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.mnuFree:
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaid:
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongs:
                feedURL = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10:
            case R.id.mnu25:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                    Log.d(TAG, "onOptionsItemSelected: The url called is " + feedURL + "and the limit is " + feedLimit);
                } else {
                    Log.d(TAG, "onOptionsItemSelected: The url called is  " + feedURL + " No change in the feed limit");
                }
                break;
            case R.id.mnuRefresh:
                oldURL = "";
                oldData = "";
                oldLimit = "";
                break;
            default:
                return super.onOptionsItemSelected(item);

        }
        downloadUrl(String.format(feedURL, feedLimit));
        return true;
    }

    private void downloadUrl(String feedURL) {
        DownloadData downloadData = new DownloadData();
        downloadData.execute(feedURL);
    }


    public class DownloadData extends AsyncTask<String, Void, String> {
        private static final String TAG = "DownloadData";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute: parameter is " + s);
            if ("error".equals(s)) {
                s = oldData;
            } else {
                oldData = s;
            }
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);
//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.list_item, parseApplications.getApplications());
            //           listApps.setAdapter(arrayAdapter);
            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record, parseApplications.getApplications());
            listApps.setAdapter(feedAdapter);
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: starts with " + strings[0]);
            String rssFeed = downloadXML(strings[0]);
            if (rssFeed == null)
                Log.e(TAG, "doInBackground: Error downloading");
            return rssFeed;
        }

        private String downloadXML(String urlPath) {
            StringBuilder xmlResult = new StringBuilder();
            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                long currentTime = System.currentTimeMillis();
//                long expires = connection.getHeaderFieldDate("Expires", currentTime);
//                long lastModified = connection.getHeaderFieldDate("Last-Modified", currentTime);
//                Log.d(TAG, "downloadXML: lastModified is "  + lastModified);
                Log.d(TAG, "downloadXML: old url is " + oldURL);
                Log.d(TAG, "downloadXML: new url is " + urlPath);
                if (!(oldURL.equals(urlPath))) {
//                    lastUpdateTime = lastModified;
                    int response = connection.getResponseCode();
                    Log.d(TAG, "downloadXML: The response was" + response);
                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);

                    int charsRead;
                    char[] inputBuffer = new char[500];
                    while (true) {
                        charsRead = reader.read(inputBuffer);
                        if (charsRead < 0) {
                            break;
                        }
                        if (charsRead > 0) {
                            xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                        }
                    }
                    reader.close();
                    if (urlPath != null) {
                        oldURL = urlPath;
                        oldLimit = Integer.toString(feedLimit);
                    } else {
                        oldURL = "";
                        oldLimit = "";
                    }
                    return xmlResult.toString();
                } else {
                    Log.d(TAG, "downloadXML: Skip Max");
                    return "error";
                }
            } catch (MalformedURLException e) //MalformedURLException is a subclass of IOException
            {
                Log.e(TAG, "downloadXML: invalid URL" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "downloadXML: IOException occurs" + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "downloadXML: Security exception, permission missing" + e.getMessage());
            }
            return null;
        }
    }
}
