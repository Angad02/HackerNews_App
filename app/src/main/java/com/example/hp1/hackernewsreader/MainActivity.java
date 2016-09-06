package com.example.hp1.hackernewsreader;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MainActivity extends Activity {

    Map<Integer,String>articleURLs = new HashMap<Integer,String>();   // Integer for storing the id and String for storing the urls.
    Map<Integer,String>articleTitles = new HashMap<Integer,String>();  //Integer for storing the id and String for storing the titles.
    ArrayList<Integer>articlesIds =  new ArrayList<Integer>();

    SQLiteDatabase articlesDB;

    ArrayList<String>titles = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    ArrayList<String>urls = new ArrayList<String>();
    ArrayList<String>content = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) { // passing content to second activity.

                Intent i = new Intent(getApplicationContext(),ArticleActivity.class);
                i.putExtra("articleUrl",urls.get(position));
                i.putExtra("content",content.get(position));
                startActivity(i);



                Log.i("articleURL",urls.get(position));
            }
        });

        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);


        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER , url VARCHAR, title VARCHAR, content VARCHAR)");


        updateListView();

         DownloadTask task = new DownloadTask();

        try {
          task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");




            Log.i("articleIds",articlesIds.toString());
            Log.i("articleTitles",articleTitles.toString());
            Log.i("articleURLs",articleURLs.toString());

           // Log.i("Result",result);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void updateListView(){


        try {

            Log.i("UI UPDATED","DONE");

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null); // show news in order of id from largest id to smallest(DESC).

            int contentIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");

            c.moveToFirst();

            titles.clear(); // for just to be sure there aren't any titles before we add any.
            urls.clear();

            while (c != null) {

                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
                content.add(c.getString(contentIndex));

               // Log.i("articleId", Integer.toString(c.getInt(articleIdIndex)));
                Log.i("articleURL", c.getString(urlIndex));
                Log.i("articleTitle", c.getString(titleIndex));

                c.moveToNext();

            }

            arrayAdapter.notifyDataSetChanged();



        }catch (Exception e){

            e.printStackTrace();
        }

      }

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override

        protected String doInBackground(String...urls){

            String result ="";
            URL url;
            HttpURLConnection urlConnection = null;

            try{

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection)url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while(data !=-1){

                char current = (char)data;

                        result+= current;

                    data = reader.read();



               }


                JSONArray jsonArray = new JSONArray(result);

                articlesDB.execSQL("DELETE FROM articles"); // deleting previous itms from the database before we add new ones.


                for(int i =0;i<20;i++){

                    // Log.i("ArticleId",jsonArray.getString(i));

                    // we need the title and the url from the API.

                    String articleId = jsonArray.getString(i);
                    //DownloadTask getArticle = new DownloadTask();

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection)url.openConnection();
                     in = urlConnection.getInputStream();
                     reader = new InputStreamReader(in);
                     data = reader.read();

                    String articleInfo = "";

                    while (data!=-1){


                        char current = (char)data;

                        articleInfo+= current;

                        data = reader.read();

                    }


                    JSONObject jsonObject = new JSONObject(articleInfo);

                    Log.i("jsonObject",jsonObject.toString());

                    String articleTitle = jsonObject.getString("title");
                    String articleURL = jsonObject.getString("url");

                   // url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");

                    String articleContent = "";
                    /*
                    url = new URL(articleURL);
                    urlConnection = (HttpURLConnection)url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleContent = "";

                    while (data!=-1){


                        char current = (char)data;

                        articleInfo+= current;

                        data = reader.read();

                    }
                    */


                    articlesIds.add(Integer.valueOf(articleId));// this will add articleId as integer to articleIds.
                    articleTitles.put(Integer.valueOf(articleId),articleTitle);
                    articleURLs.put(Integer.valueOf(articleId),articleURL);

                    // Log.i("articleTitle",articleTitle);
                    //Log.i("articleURL",articleURL);

                    // Inserting the articles into the database.

                    String sql = "INSERT INTO articles (articleId,url,title,content) VALUES (?,?,?,?)";

                    SQLiteStatement statement = articlesDB.compileStatement(sql);// converting a string into a sql statement.

                    statement.bindString(1,articleId); // replace 1st question mark with articleId.
                    statement.bindString(2,articleURL);
                    statement.bindString(3,articleTitle);
                    statement.bindString(4,articleContent);

                    statement.execute();

                }




            }
            catch (Exception e){

                e.printStackTrace();
            }


            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
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
}
