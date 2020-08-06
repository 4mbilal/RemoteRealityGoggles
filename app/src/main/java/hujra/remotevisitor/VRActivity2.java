package hujra.remotevisitor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;


public class VRActivity2 extends CardboardActivity {
    private static final String TAG = "VRActivity2";

    private MjpegView mv;
    private boolean running;
    private DataOutputStream out;
    private DataInputStream in;
    public boolean CommLinkRxconnected = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        String vidStreamURL = "http://";
        vidStreamURL += i.getExtras().get("IPtext") + ":8080/stream/video.mjpeg";
        String HeadTrackingURL = i.getExtras().get("IPtext") + "";

/*        SharedPreferences pref = getSharedPreferences("prefs", 0);
        server = pref.getString("server", "192.168.1.4");
        port = pref.getInt("port", 50000);
        camPort = pref.getInt("camPort", 8080);

        //save pref values again
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("server", server).apply();
        editor.putInt("port", port).apply();
        editor.putInt("camPort", camPort).apply();
*/
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mv = new MjpegView(this);
        setContentView(mv);

        running = true;
        new HeadTracking(this).start();
        new CommLinkSetup(HeadTrackingURL, 5000).start();
        new MjpgReader().execute(vidStreamURL);
        //new CommLinkRx().start();
    }

    public void onPause() {
        super.onPause();
        mv.stopPlayback();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            mv.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public class MjpgReader extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.i(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(new URI(url[0])));
                Log.i(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            } catch (URISyntaxException e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed-UriSyntaxException", e);
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
        }
    }

    private class CommLinkSetup extends Thread {
        private String serverName;
        private int port;
        private boolean connected;
        private Socket client;

        public CommLinkSetup(String serverName, int port) {
            this.serverName = serverName;
            this.port = port;
        }

        @Override
        public void run() {
            connected = false;
            while (!connected) {
                try {
                    Log.i(TAG, "Trying to connect to " + serverName + ":" + port + "...");
                    client = new Socket(serverName, port);
                    Log.i(TAG, "Connected to " + client.getRemoteSocketAddress());

                    OutputStream outToServer = client.getOutputStream();
                    out = new DataOutputStream(outToServer);

                    InputStream inFromServer = client.getInputStream();
                    in = new DataInputStream(inFromServer);
                    connected = true;
                    CommLinkRxconnected = true;

                    Log.i(TAG, "Connected ?: " + client.getRemoteSocketAddress());
                } catch (IOException e) {
                    Log.e(TAG, "Connection failed, retrying ...");
                    try {
                        sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    connected = false;
                }
            }
        }

        public boolean isConnected() {
            return connected;
        }

        public Socket getClient() {
            return client;
        }
    }

    public void CommLinkTx(float yaw,float pitch,float roll) {
        byte[] buffer_y = ByteBuffer.allocate(4).putFloat(yaw).array();
        byte[] buffer_p = ByteBuffer.allocate(4).putFloat(pitch).array();
        byte[] buffer_r = ByteBuffer.allocate(4).putFloat(roll).array();
        byte[] buffer = new byte[12];
        System.arraycopy(buffer_y, 0, buffer, 0, 4);
        System.arraycopy(buffer_p, 0, buffer, 4, 4);
        System.arraycopy(buffer_r, 0, buffer, 8, 4);
        try {
            out.write(buffer, 0, 12);
        } catch (IOException e) {
            Log.e(TAG, "Failed to send data! ");
        }
    }

    private class CommLinkRx extends Thread {

        @Override
        public void run() {
/*
            //listen for incoming messages from the car
            String message;
            while(true) {

                try {
                    message = in.readUTF();

                    //listen for pin state changes
                    if (message.contains("fwd fast")) fwdFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("fwd slow")) fwdSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd fast")) bwdFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd slow")) bwdSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("left fast")) leftFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("left slow")) leftSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("right fast")) rightFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("right slow")) rightSlow = Car.PinState.parse(message.split("\\s+")[2]);

                    Log.i(TAG, "Nachricht vom Auto: " + message);

                } catch (Exception ignored) {}
            }*/
        }
    }
}