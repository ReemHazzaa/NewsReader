package com.example.reem.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    // Create a DataBase to store articles
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure the DB is ready before any download takes place
        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        // Create an articles Table in the DB
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR) ");



        DownloadTask task = new DownloadTask();
        try {
            // Get the top hacker news
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }


        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Go to articleActivity
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);


            }
        });

        updateListView();
    }

    public void updateListView() {
        // Pull from the DB
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        // Move through
        if (c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {

                // Fill in the array lists from the DB
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            } while (c.moveToNext());

            // Notify
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection;

            try {
                // Get the first url in urls[0] and convert it into a string
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1) {
                   char current = (char) data;
                   result += current;
                   data = inputStreamReader.read();
                }

                // We got the data as a string = result
                // Process it as a JSON object
                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if (jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length();
                }

                // Clear the articles table in DB before looping through all items
                articlesDB.execSQL("DELETE FROM articles");

                // loop through all items
                for (int i =0; i<numberOfItems; i++) {
                    // Get the article id
                    String articleId = jsonArray.getString(i);

                    // Get the URL of that particular article
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ articleId +".json?print=pretty");

                    // Download the article info
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();
                    String articleInfo = "";
                    while (data != -1) {
                        char current = (char) data;
                        articleInfo += current;
                        data = inputStreamReader.read();
                    }


                    //Log.i("ArticleInfo", articleInfo);
                    // Get the "title" & "URL" of the article
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        // Download article content
                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();

                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String articleContent = "";
                        while (data != -1) {
                            char current = (char) data;
                            articleContent += current;
                            data = inputStreamReader.read();
                        }

                        // Store in DB
                        String sql = "INSERT INTO articles(articleId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        // Pass the info
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);

                        statement.execute();

                    }
                }



                Log.i("URL content",result);
                return result;

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();

        }
    }
}
