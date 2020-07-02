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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.resize;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener {
    private String[] Codecolor = {"黒","茶","赤","橙","黄","緑","青","紫","灰","白"};
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

    int min(int a1, int a2){
        return a1 > a2 ? a2 : a1;
    }

    int max(int a1, int a2){
        return a1 < a2 ? a2 : a1;
    }

    private void paste(Mat dst, Mat src, int x, int y, int width, int height) {
        Mat resized_img = null;
        resize(src, resized_img, new Size(width, height));

        if (x >= dst.cols() || y >= dst.rows()) return;
        int w = (x >= 0) ? min(dst.cols() - x, resized_img.cols()) : min(max(resized_img.cols() + x, 0), dst.cols());
        int h = (y >= 0) ? min(dst.rows() - y, resized_img.rows()) : min(max(resized_img.rows() + y, 0), dst.rows());
        int u = (x >= 0) ? 0 : min(-x, resized_img.cols() - 1);
        int v = (y >= 0) ? 0 : min(-y, resized_img.rows() - 1);
        int px = max(x, 0);
        int py = max(y, 0);

        Mat roi_dst = dst.submat(new Rect(px, py, w, h));
        Mat roi_resized = resized_img.submat(new Rect(u, v, w, h));
        roi_resized.copyTo(roi_dst);
    }

    // 画像を画像に貼り付ける関数（サイズ指定を省略したバージョン）
    private void paste(Mat dst, Mat src, int x, int y) {
        paste(dst, src, x, y, src.rows(), src.cols());
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        // ここで何らかの画像処理を行う
        // 試しに、ネガポジ反転してみる
//        Core.bitwise_not(inputFrame, inputFrame);
        Mat rot = new Mat();

        Core.rotate(inputFrame, rot, Core.ROTATE_90_CLOCKWISE);

        Rect roi = new Rect(new Point(220, 358), new Size(40, 5));
        final Mat cutted = rot.submat(roi);
        cutted.copyTo(cutted);

        rectangle(rot, new Point(220,350), new Point(260, 370), new Scalar(255,0,0), 1, 1);

        double a[] = cutted.get(0,1);

        for(int i=0; i<100; ++i){
            rot.put(100,100+i,a);
        }


        textView.post(new Runnable() {
            public void run() {
                textView.setText("h:" + cutted.rows() + " r:" + cutted.cols());
            }
        });

        return rot;
    }
}
