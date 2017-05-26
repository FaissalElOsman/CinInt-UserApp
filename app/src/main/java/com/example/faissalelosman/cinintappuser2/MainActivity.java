package com.example.faissalelosman.cinintappuser2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by faissalelosman on 05/02/2017.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    Button      bPlay, bPause,bSynchronizePlay,bReturn,bExit;
    MediaPlayer mpPlayer;
    TextView    tvFilmName,tvDescription,tvDuration,tvTimeStamp,tvMinimalRTT;
    SeekBar     sbPutTimeStamp;
    Intent      iWelcome;
    int         iTimeStamp;
    JSONObject  request;
    int         minimal_delay;
    boolean     isMinimalDelaySet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bSynchronizePlay    = (Button)findViewById(R.id.bSynchronize);
        bPlay               = (Button)findViewById(R.id.bPlay);
        bPause              = (Button)findViewById(R.id.bPause);
        bReturn             = (Button)findViewById(R.id.bReturn);
        bExit               = (Button)findViewById(R.id.bExit);

        tvFilmName          = (TextView)findViewById(R.id.tvFilmName);
        tvDescription       = (TextView)findViewById(R.id.tvDescription);
        tvDuration          = (TextView)findViewById(R.id.tvDuration);
        tvTimeStamp         = (TextView)findViewById(R.id.tvTimeStamp);
        tvMinimalRTT        = (TextView)findViewById(R.id.tvMinimalRTT);

        sbPutTimeStamp      = (SeekBar)findViewById(R.id.sbPutTimeStamp);

        iWelcome            = new Intent(this, WelcomeActivity.class);

        mpPlayer            = MediaPlayer.create(MainActivity.this, Uri.parse(Environment.getExternalStorageDirectory().getPath()+ "/CinInt/right.wav"));

        bPlay.setOnClickListener(this);
        bPause.setOnClickListener(this);
        bSynchronizePlay.setOnClickListener(this);
        bReturn.setOnClickListener(this);
        bExit.setOnClickListener(this);
        sbPutTimeStamp.setOnSeekBarChangeListener(this);

        sbPutTimeStamp.setMax(mpPlayer.getDuration());
        tvTimeStamp.setText("TimeStamp : "+sbPutTimeStamp.getProgress()+" / "+sbPutTimeStamp.getMax());

        Uri.Builder builder;
        URL url= null;
        builder=new Uri.Builder();
        builder.scheme("http")
                .encodedAuthority(WelcomeActivity.sSyncNodeIpAddress+":5000")
                .appendPath("connect");
        try {
            url = new URL(builder.build().toString());
            new MainActivity.ScanHandler().execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        final Handler hHandler = new Handler();
        final int iDelay = 100; //milliseconds

        hHandler.postDelayed(new Runnable(){
            public void run(){
                sbPutTimeStamp.setProgress(mpPlayer.getCurrentPosition());
                tvTimeStamp.setText("TimeStamp : "+mpPlayer.getCurrentPosition()+" / "+sbPutTimeStamp.getMax());
                hHandler.postDelayed(this, iDelay);
            }
        }, iDelay);
    }

    @Override
    public void onClick(View view) {
        Uri.Builder builder;
        URL url= null;
        switch (view.getId()) {
            case R.id.bPlay:
                mpPlayer.start();
                break;
            case R.id.bPause:
                mpPlayer.pause();
                break;
            case R.id.bSynchronize:
                builder=new Uri.Builder();
                builder.scheme("http")
                        .encodedAuthority(WelcomeActivity.sSyncNodeIpAddress+":5000")
                        .appendPath("synchronize");
                try {
                    url = new URL(builder.build().toString());
                    try {
                        request   = new JSONObject();
                        request.put("deviceTS",System.currentTimeMillis());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    new MainActivity.SynchronizeHandler().execute(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bReturn:
                mpPlayer.stop();
                startActivity(iWelcome);
                break;
            case R.id.bExit:
                mpPlayer.stop();
                MainActivity.this.finish();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        iTimeStamp = i;
        tvTimeStamp.setText("TimeStamp : "+iTimeStamp+" / "+sbPutTimeStamp.getMax());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mpPlayer.pause();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mpPlayer.seekTo(iTimeStamp);
        mpPlayer.start();
    }

    public class SynchronizeHandler extends AsyncTask<URL,String,String> {

        @Override
        protected String doInBackground(URL... params) {
            HttpURLConnection connection    =null;
            BufferedReader reader           =null;
            try{
                connection = (HttpURLConnection) params[0].openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(request.toString());
                wr.flush();

                connection.connect();
                InputStream stream = connection.getInputStream();

                reader              = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line="";
                while((line=reader.readLine())!=null)
                    buffer.append(line);
                connection.disconnect();
                //mpPlayer.pause();

                return buffer.toString();
            } catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {

            super.onPostExecute(s);
            try {
                JSONObject object=new JSONObject(s);
                String success = object.getString("success");

                if(success=="true"){
                    String  playerDelay     = object.getString("playerDelay");
                    String  initialDeviceTS = object.getString("deviceTS");
                    long    currentDeviceTS = System.currentTimeMillis();
                    int RTT                 = (int)(currentDeviceTS - Long.parseLong(initialDeviceTS));
                    int    delay            = Integer.parseInt(playerDelay) + (RTT/2);
                    if(!isMinimalDelaySet){
                        minimal_delay = RTT;
                        isMinimalDelaySet= true;
                        tvMinimalRTT.setText("MinimalRTT = "+ minimal_delay);
                        mpPlayer.seekTo(delay);
                        mpPlayer.start();
                    }
                    if(RTT<minimal_delay){
                        minimal_delay = RTT;
                        tvMinimalRTT.setText("MinimalRTT = "+ minimal_delay);
                        mpPlayer.seekTo(delay);
                        mpPlayer.start();
                    }

                }
                else{
                    String data = object.getString("data");
                    Context context = getApplicationContext();
                    Toast.makeText(context, data, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class ScanHandler extends AsyncTask<URL,String,String> {

        @Override
        protected String doInBackground(URL... params) {
            HttpURLConnection connection    =null;
            BufferedReader reader           =null;
            try{
                Log.v("faissal","connection = "+params[0].toString());
                connection = (HttpURLConnection) params[0].openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                InputStream stream = connection.getInputStream();

                reader              = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line="";
                while((line=reader.readLine())!=null)
                    buffer.append(line);
                Log.v("faissal","buffer = "+buffer);

                String saveDir = Environment.getExternalStorageDirectory().getPath()+ "/CinInt";
                String saveFilePath = saveDir + File.separator + "lol.wav";
                Log.v("faissal","saveFilePath = "+saveFilePath);
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);
                int bytesRead = -1;
                byte[] bufferFile = new byte [4096];
                while((bytesRead = stream.read(bufferFile)) != -1){
                    outputStream.write(bufferFile,0,bytesRead);
                }
                outputStream.close();

                connection.disconnect();
                return buffer.toString();
            } catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {

            super.onPostExecute(s);
            try {
                JSONObject object=new JSONObject(s);
                String success = object.getString("success");

                if(success=="true"){
                    String sFilmName = object.getString("filmName");
                    String sFilmDescription = object.getString("filmDescription");
                    tvFilmName.setText("Film name : "+sFilmName);
                    tvDescription.setText("Description : "+sFilmDescription);

                }
                else{
                    String data = object.getString("data");
                    Context context = getApplicationContext();
                    Toast.makeText(context, data, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
