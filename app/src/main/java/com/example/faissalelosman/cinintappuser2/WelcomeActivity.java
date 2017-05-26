package com.example.faissalelosman.cinintappuser2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
/**
 * Created by faissalelosman on 05/02/2017.
 */


public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener,ZXingScannerView.ResultHandler {
    static String sSyncNodeIpAddress ;
    Button bScan;
    Intent iMain;
    ZXingScannerView zScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        bScan       = (Button)findViewById(R.id.bScan);
        iMain    = new Intent(this, MainActivity.class);

        bScan.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Uri.Builder builder;
        URL url= null;
        switch (view.getId()) {
            case R.id.bScan:
                zScannerView = new ZXingScannerView(this);
                setContentView(zScannerView);
                zScannerView.setResultHandler(this);
                zScannerView.startCamera();
                break;
        }
    }

    @Override
    public void handleResult(Result result) {
        sSyncNodeIpAddress = result.toString();
        zScannerView.stopCamera();
        this.finish();
        startActivity(iMain);
    }


}
