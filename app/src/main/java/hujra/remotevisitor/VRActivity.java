package hujra.remotevisitor;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

public class VRActivity extends CardboardActivity implements CardboardView.StereoRenderer, View.OnTouchListener {
    private static final String TAG = "StreamActivity";
    private static final String BEGIN_MSG = "Starting the VR activity";
    private float[] mEulerAngles = new float[3];
    private float[] mInitEulerAngles = new float[3];
    private final double PRECISION = Math.PI / 450.0;
    private final double RANGE = Math.PI / 2.0;
    private Vibrator mVibrator;
    private CardboardOverlayView mOverlayView;
    private MjpegPlayer mp;
    private int i = 0;
    private String baseUrl = "http://";
    private WaitingRequestQueue mQueue;
    private boolean tracking = false;

    public VRActivity() {
    }

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     * //@param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(R.layout.activity_vr);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);
        cardboardView.setOnTouchListener(this);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Intent i = getIntent();
        baseUrl += i.getExtras().get("IPtext");
        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        mOverlayView.show3DToast(BEGIN_MSG);
        startPlayer();

        mQueue = new WaitingRequestQueue(this, baseUrl + ":8080/move");
        mQueue.addRequest(0f, 0f);
    }

    private void startPlayer(){
        String URL = baseUrl + ":8080/stream/video.mjpeg";
        mp = new MjpegPlayer(mOverlayView);
        (new DoRead()).execute(URL);
    }

    @Override
    public void onRendererShutdown(){Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");

    }

    /**
     * Creates the buffers we use to store information about the 3D world. OpenGL doesn't use Java
     * arrays, but rather needs data in a format it can understand. Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getEulerAngles(mEulerAngles, 0);
        if (i % 100 == 0) {
            Log.i(TAG, mEulerAngles[0] + " " + mEulerAngles[1] + " " + mEulerAngles[2]);
        }
        i++;
        if (tracking) {
            shift();
            mQueue.addRequest(mEulerAngles[0], mEulerAngles[1]);
        }
    }

    private void shift() {
        for (int i = 0; i < mEulerAngles.length; i++) {
            mEulerAngles[i] -= mInitEulerAngles[i];
        }
    }

    /**
     * Draws a frame for an eye. The transformation for that eye (from the camera) is passed in as
     * a parameter.
     *
     * @param transform The transformations to apply to render this eye.
     */
    @Override
    public void onDrawEye(EyeTransform transform) {
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (!tracking) {
                Log.i(TAG, "starting tracking");
                mInitEulerAngles = mEulerAngles.clone();
                tracking = true;
                mOverlayView.fade3DToast();
                mQueue.start();
            } else {
                Log.i(TAG, "stopping tracking");
                tracking = false;
                mOverlayView.show3DToast(BEGIN_MSG);
                mQueue.stopAndRecenter();
            }
        }
        return true;
    }

    class DoRead extends AsyncTask<String, Void, MjpegInputStream> {

        @Override
        protected MjpegInputStream doInBackground(String... params) {
            return MjpegInputStream.read(params[0]);
        }

        protected void onPostExecute(MjpegInputStream result) {
            if (result == null){
             //   throw new RuntimeException("stream is null!!!");
                Log.e(TAG, "stream is null!!!");
            }
            else {
                mp.setSource(result);
                Log.i(TAG, "running mjpeg input stream");
            }
        }
    }
}