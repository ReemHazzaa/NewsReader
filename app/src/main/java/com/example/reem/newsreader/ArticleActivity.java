package com.example.reem.newsreader;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        WebView webView =findViewById(R.id.webView);

        // Make sure Javascript is enabled
        webView.getSettings().setJavaScriptEnabled(true);

        //Don't male the phone launch it's own browser
        webView.setWebViewClient(new WebViewClient());

        // Get the intent and display info
        Intent intent = getIntent();
        webView.loadData(intent.getStringExtra("content"), "text/html","UTF-8");
    }
}
