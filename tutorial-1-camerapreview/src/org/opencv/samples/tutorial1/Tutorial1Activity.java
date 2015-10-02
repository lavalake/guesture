package org.opencv.samples.tutorial1;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.highgui.VideoCapture;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import static org.opencv.core.Core.*;
import static org.opencv.highgui.Highgui.*;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.video.Video.*;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class Tutorial1Activity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            System.out.println("opencv init failed");
        }else{
            System.out.print("opencv loaded");
        }
        //System.loadLibrary("opencv_java");
    }
    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    int barWidth = 5;
    int lastBar = -1;
    int videoHeight=0, videoWidth=0;
    int last = 0;
    // number of cyclic frame buffer used for motion detection
    // (should, probably, depend on FPS)
    final int N = 2;
    final double MHI_DURATION = 1;
    final double MAX_TIME_DELTA = 0.5;
    final double MIN_TIME_DELTA = 0.05;
    Mat image, motion, mhi, orient, mask, segmask;
    Mat[] buf;
    VideoCapture capture;
    Size size;
    double magnitude, startTime = 0;
    Timer timer;
    final Handler h = new Handler(new Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            long millis = System.currentTimeMillis();
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds     = seconds % 60;
            capture.read(image);
            if (image.empty()) {

            } else {
                //update_mhi(image, motion, 30);

            }

            return false;
        }
    });
    class firstTask extends TimerTask {

        @Override
        public void run() {
            h.sendEmptyMessage(0);
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    image = new Mat();
                    capture = new VideoCapture(0);
                    if (capture.open(0)) {
                        videoWidth = (int) capture.get(CV_CAP_PROP_FRAME_WIDTH);
                        videoHeight = (int) capture.get(CV_CAP_PROP_FRAME_HEIGHT);
                    }
                    //size(videoWidth, videoHeight);
                    
                    size = new Size(videoWidth, videoHeight);

                    buf = new Mat[N];
                    
                    for (int i = 0; i < N; i++) {
                        buf[i] = Mat.zeros(size, CvType.CV_8UC1);
                    }
                    //image = Mat.zeros(size, CvType.CV_8UC1);
                    motion = Mat.zeros(size, CvType.CV_8UC3);
                    //mhi = Mat.zeros(size, CvType.CV_32FC1);
                    orient = Mat.zeros(size, CvType.CV_32FC1);
                    segmask = Mat.zeros(size, CvType.CV_32FC1);
                    mask = Mat.zeros(size, CvType.CV_8UC1);
                    
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private int count;

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        /*image = new Mat();
        capture = new VideoCapture(0);
        if (capture.open(0)) {
            videoWidth = (int) capture.get(CV_CAP_PROP_FRAME_WIDTH);
            videoHeight = (int) capture.get(CV_CAP_PROP_FRAME_HEIGHT);
        }
        //size(videoWidth, videoHeight);
        size = new Size(videoWidth, videoHeight);

        buf = new Mat[N];
        for (int i = 0; i < N; i++) {
            buf[i] = Mat.zeros(size, CvType.CV_8UC1);
        }
        motion = Mat.zeros(size, CvType.CV_8UC3);
        mhi = Mat.zeros(size, CvType.CV_32FC1);
        orient = Mat.zeros(size, CvType.CV_32FC1);
        segmask = Mat.zeros(size, CvType.CV_32FC1);
        mask = Mat.zeros(size, CvType.CV_8UC1);
        */
        count = 0;
        //background(0);
        startTime = System.nanoTime();
        
        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
                toastMesage = "Java Camera";
            } else {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
                toastMesage = "Native Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableView();
            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        
        count++;
        if(count%10 == 0){
            //frame.convertTo(image, rtype);
            System.out.println("frame type "+frame.type() + "image type "+image.type());
            frame.convertTo(image, CvType.CV_8UC1);
            System.out.println("frame type "+frame.type() + "image type "+image.type());
            update_mhi(image, motion, 30);
        }
        return frame;
    }
    private void update_mhi(Mat img, Mat dst, int diff_threshold) {
        double timestamp = (System.nanoTime() - startTime) / 1e9;
        int idx1 = last, idx2;
        Mat silh;
        System.out.println("call update mhi");
        
        cvtColor(img, buf[last], COLOR_BGR2GRAY);
        double angle, count;
        
        idx2 = (last + 1) % N; // index of (last - (N-1))th frame
        last = idx2;
        if(last==1) return;
        silh = buf[idx2];
        absdiff(buf[idx1], buf[idx2], silh);
        threshold(silh, silh, diff_threshold, 1, THRESH_BINARY);
        
        mhi = Mat.zeros(silh.size(), CvType.CV_32FC1);
        updateMotionHistory(silh, mhi, timestamp, MHI_DURATION);
        mhi.convertTo(mask, mask.type(), 255.0 / MHI_DURATION,
                (MHI_DURATION - timestamp) * 255.0 / MHI_DURATION);
        dst.setTo(new Scalar(0));
        List<Mat> list = new ArrayList<Mat>(3);
        list.add(mask);
        list.add(Mat.zeros(mask.size(), mask.type()));
        list.add(Mat.zeros(mask.size(), mask.type()));
        merge(list, dst);
        calcMotionGradient(mhi, mask, orient, MAX_TIME_DELTA, MIN_TIME_DELTA, 3);
        MatOfRect roi = new MatOfRect();
        segmentMotion(mhi, segmask, roi, timestamp, MAX_TIME_DELTA);
        int total = roi.toArray().length;
        Rect[] rois = roi.toArray();
        Rect comp_rect;
        Scalar color;
        
        /*
        for (int i = -1; i < total; i++) {
            if (i < 0) {
                comp_rect = new Rect(0, 0, videoWidth, videoHeight);
                color = new Scalar(255, 255, 255);
                magnitude = 100;
            } else {
                comp_rect = rois[i];
                if (comp_rect.width + comp_rect.height < 100) // reject very small components
                    continue;
                color = new Scalar(0, 0, 255);
                magnitude = 30;
            }

            Mat silhROI = silh.submat(comp_rect);
            Mat mhiROI = mhi.submat(comp_rect);
            Mat orientROI = orient.submat(comp_rect);
            Mat maskROI = mask.submat(comp_rect);

            angle = calcGlobalOrientation(orientROI, maskROI, mhiROI, timestamp, MHI_DURATION);
            angle = 360.0 - angle;
            count = Core.norm(silhROI, NORM_L1);

            silhROI.release();
            mhiROI.release();
            orientROI.release();
            maskROI.release();
            if (count < comp_rect.height * comp_rect.width * 0.05) {
                continue;
            }
            
            if(total > 0)
            System.out.println("movement detect");
            Point center = new Point((comp_rect.x + comp_rect.width / 2),
                    (comp_rect.y + comp_rect.height / 2));
            circle(dst, center, (int) Math.round(magnitude * 1.2), color, 3, LINE_AA, 0);
            Core.line(dst, center, new Point(
                    Math.round(center.x + magnitude * Math.cos(angle * Math.PI / 180)),
                    Math.round(center.y - magnitude * Math.sin(angle * Math.PI / 180))), color, 3, LINE_AA, 0);
        
        }*/
        if(total > 0)
           System.out.println("movement detect");


    }
        
}
