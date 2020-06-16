package com.thistuna.resistorscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import static org.opencv.imgproc.Imgproc.rectangle;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener {
    private CameraBridgeViewBase m_cameraView;
    private TextView textView;

    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissionCamera(this);
        OpenCVLoader.initDebug();
        m_cameraView = findViewById(R.id.camera_view);
        m_cameraView.setCvCameraViewListener(this);
        m_cameraView.enableView();

        textView = findViewById(R.id.TextView1);

        textView.setText("赤 赤 黒 茶");
    }

    public static boolean getPermissionCamera(Activity activity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(
                    activity,
                    permissions,
                    0);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        // ここで何らかの画像処理を行う
        // 試しに、ネガポジ反転してみる
//        Core.bitwise_not(inputFrame, inputFrame);
        Mat rot = new Mat();

        Core.rotate(inputFrame, rot, Core.ROTATE_90_CLOCKWISE);

        rectangle(rot, new Point(220,350), new Point(260, 370), new Scalar(255,0,0), 1, 1);



        return rot;
    }
}
