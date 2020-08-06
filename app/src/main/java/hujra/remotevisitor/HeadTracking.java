package hujra.remotevisitor;

import android.content.Context;
import android.util.Log;

import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

import java.util.ArrayList;
import java.util.Arrays;

public class HeadTracking extends Thread {
    private static final String TAG = "HeadTracking";
    private HeadTracker tracker;
    private HeadTransform headTransform;
    private float[] mEulerAngles = new float[3];
    private ArrayList<String> trackerData = new ArrayList<>();
    private boolean running;
    VRActivity2 mainActivity;

    public HeadTracking(VRActivity2 mainActivity) {
        this.mainActivity = mainActivity;
        //init head tracking
        tracker = new HeadTracker(mainActivity);
        tracker.startTracking();
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            if (tracker != null) {
                float[] headView = new float[16];
                tracker.getLastHeadView(headView, 0);
                //headTransform.getEulerAngles(mEulerAngles, 0);
                float yaw = (float) (Math.atan2(-headView[8],Math.sqrt(headView[9]*headView[9]+headView[10]*headView[10]))*57.296);
                float roll = (float) (Math.atan2(headView[4],headView[0])*57.296);
                float pitch = (float) (Math.atan2(headView[9],headView[10])*57.296);
                Log.d(TAG, ":" + yaw + "," + pitch + "," + roll);
                if(mainActivity.CommLinkRxconnected) {
                    mainActivity.CommLinkTx(yaw,pitch,roll);
                }
            } else Log.e(TAG, "tracker == null");
            try {
                Thread.sleep(100, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
