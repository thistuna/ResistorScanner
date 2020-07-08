package com.thistuna.resistorscanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
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
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.COLOR_HSV2BGR;
import static org.opencv.imgproc.Imgproc.cvtColor;
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

    // dstにsrcを貼り付ける
    private void paste(Mat dst, Mat src, int x, int y, int width, int height) {
        Mat resized_img = new Mat();
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
        paste(dst, src, x, y, src.cols(), src.rows());
    }

    int ColorHSV[][] = {
            {0,0,42}, // 黒
            {14,173,134}, // 茶
            {248,227,237}, // 赤
            {11,219,241}, // 橙
            {43,219,240}, // 黄
            {85,255,192}, // 緑
            {147,255,192}, // 青
            {45,125,161}, // 紫
            {153,8,166}, // 灰
            {0,0,239} // 白
    };

    private int decodeColorCode(int H, int S, int V){
        double distance = 1145141919;
        int minnum = 0;
        for(int i=0; i<10; i++){
            int Hd = H-ColorHSV[i][0];
            if(Hd < 0) Hd = -Hd;
            if(Hd >= 128) Hd = 256-Hd;
            int Sd = S-ColorHSV[i][1];
            if(Sd < 0) Sd = -Sd;
            int Vd = V-ColorHSV[i][2];
            if(Vd < 0) Vd = -Vd;
            double localDistance = Hd*Hd*50 + Sd/50 + Vd/50;
            if(localDistance < distance){
                minnum = i;
                distance = localDistance;
            }
        }

        return minnum;
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        // ここで何らかの画像処理を行う
        // 試しに、ネガポジ反転してみる
//        Core.bitwise_not(inputFrame, inputFrame);
        Mat rot = new Mat();

        Core.rotate(inputFrame, rot, Core.ROTATE_90_CLOCKWISE);

        Rect roi = new Rect(new Point(220, 358), new Size(40, 5));
        final Mat cuttedPic = new Mat();
        rot.submat(roi).copyTo(cuttedPic);

//        double a[] = cuttedPic.get(0,0);
//
//        for(int i=0; i<100; ++i){
//            rot.put(100,100+i,a);
//        }

        int MAX_CLUSTERS = 5;

        final Mat cuttedHSV = new Mat();
        final Mat ReCutPic = new Mat();

        Mat points = new Mat();
        Imgproc.cvtColor(cuttedPic, cuttedHSV, Imgproc.COLOR_RGBA2BGR,3);
        Imgproc.cvtColor(cuttedHSV, cuttedHSV, Imgproc.COLOR_BGR2HSV, 3);

        cuttedHSV.convertTo(points, CvType.CV_32FC3);
        int size = points.rows()*points.cols();
        points = points.reshape(3,size);

        final Mat clusters = new Mat();
        Mat centers = new Mat();
        Core.kmeans(points, MAX_CLUSTERS, clusters,
                new TermCriteria(TermCriteria.EPS+TermCriteria.MAX_ITER, 10, 1.0), 1, Core.KMEANS_PP_CENTERS, centers);

        //Mat color = Mat.zeros(MAX_CLUSTERS, 1, CvType.CV_32FC3);
        //Mat count = Mat.zeros(MAX_CLUSTERS, 1, CvType.CV_32SC1);

        Mat reshapeClusters = clusters.reshape(1, cuttedHSV.rows());

        int len = -1;
        int min = 100;
        int max = -1;
        int[] count = new int[MAX_CLUSTERS];
        double[] CenterOfGravityX = new double[MAX_CLUSTERS];
        double[] CenterOfGravityY = new double[MAX_CLUSTERS];
        double[] meanH = new double[MAX_CLUSTERS];
        double[] meanS = new double[MAX_CLUSTERS];
        double[] meanV = new double[MAX_CLUSTERS];
        Mat plot = Mat.zeros(cuttedHSV.rows(), cuttedHSV.cols(), CvType.CV_8UC3);
        int[][] meanAddH = new int[MAX_CLUSTERS][size];
        int[][] meanAddS = new int[MAX_CLUSTERS][size];
        int[][] meanAddV = new int[MAX_CLUSTERS][size];
        for(int i=0; i<plot.rows(); ++i){
            for(int j=0; j<plot.cols(); ++j){
                int[] temp = new int[1];
                byte[] puttemp = new byte[3];
                len = reshapeClusters.get(i,j).length;
                reshapeClusters.get(i,j,temp);
                if(temp[0] > max) max = temp[0];
                if(temp[0] < min) min = temp[0];
/*                if(temp[0] == 0) {
                    puttemp[0] = 0;
                    puttemp[1] = 0;
                    puttemp[2] = 0;
                }
                if(temp[0] == 1) {
                    puttemp[0] = (byte)255;
                    puttemp[1] = 0;
                    puttemp[2] = 0;
                }
                if(temp[0] == 2) {
                    puttemp[0] = 0;
                    puttemp[1] = (byte)255;
                    puttemp[2] = 0;
                }
                if(temp[0] == 3) {
                    puttemp[0] = 0;
                    puttemp[1] = 0;
                    puttemp[2] = (byte)255;
                }
                if(temp[0] == 4) {
                    puttemp[0] = (byte)255;
                    puttemp[1] = (byte)255;
                    puttemp[2] = (byte)255;
                }*/
                for(int k = 0; k<MAX_CLUSTERS; ++k){
                    if(temp[0] == k){
                        count[k]++;
                        CenterOfGravityX[k] += j;
                        CenterOfGravityY[k] += i;

                        byte[] getTempHSV = new byte[3];
                        cuttedHSV.get(i,j,getTempHSV);

                        meanAddH[k][0]++;
                        meanAddS[k][0]++;
                        meanAddV[k][0]++;

                        meanAddH[k][meanAddH[k][0]] = getTempHSV[0];
                        meanAddS[k][meanAddS[k][0]] = getTempHSV[1];
                        meanAddV[k][meanAddV[k][0]] = getTempHSV[2];


                    }
                }
                //plot.put(i,j,puttemp);
            }
        }

        for(int i=0; i<5; i++){
            double meanSubH[] = new double[meanAddH[i][0]];
            double meanSubS[] = new double[meanAddS[i][0]];
            double meanSubV[] = new double[meanAddV[i][0]];

            for(int j=0; j<meanAddH[i][0]; ++j){
                meanSubH[j] = meanAddH[i][j+1];
                meanSubS[j] = meanAddS[i][j+1];
                meanSubV[j] = meanAddV[i][j+1];
            }

            Arrays.sort(meanSubH);
            Arrays.sort(meanSubS);
            Arrays.sort(meanSubV);

            meanH[i] = meanSubH[meanSubH.length/2];
            meanS[i] = meanSubS[meanSubS.length/2];
            meanV[i] = meanSubV[meanSubV.length/2];
        }

        int bg=0;
        for(int i=0; i<5; ++i){
            CenterOfGravityX[i] /= count[i];
            CenterOfGravityY[i] /= count[i];
//            meanH[i] /= count[i];
//            meanS[i] /= count[i];
//            meanV[i] /= count[i];
            if(count[bg] < count[i]) bg = i;
        }

        double[] unti = CenterOfGravityX.clone();
        unti[bg] = 114514;
        int[] ColorLine = new int[5];
        for(int i=0; i<5; ++i){
            int localmin = 0;
            for(int j=0; j<5; ++j){
                if(unti[localmin] > unti[j]){
                    localmin = j;
                }
            }
            ColorLine[i] = localmin;
            unti[localmin] = 114154+1+i;
        }

        int[] colors = new int[4];
        colors[0] = decodeColorCode((int)meanH[ColorLine[0]], (int)meanS[ColorLine[0]], (int)meanV[ColorLine[0]]);
        colors[1] = decodeColorCode((int)meanH[ColorLine[1]], (int)meanS[ColorLine[1]], (int)meanV[ColorLine[1]]);
        colors[2] = decodeColorCode((int)meanH[ColorLine[2]], (int)meanS[ColorLine[2]], (int)meanV[ColorLine[2]]);
        colors[3] = decodeColorCode((int)meanH[ColorLine[3]], (int)meanS[ColorLine[3]], (int)meanV[ColorLine[3]]);

        String resultColor = Codecolor[colors[0]]+Codecolor[colors[1]]+Codecolor[colors[2]]+Codecolor[colors[3]];
        int resultReg = colors[0]*10 + colors[1];
        for(int i=0; i<colors[2]; ++i){
            resultReg *= 10;
        }

        for(int i=0; i<plot.rows(); ++i) {
            for (int j = 0; j < plot.cols(); ++j) {
                byte[] puttemp = new byte[3];
                int[] temp = new int[1];
                reshapeClusters.get(i,j,temp);

                for(int k = 0; k<5; ++k){
                    if(temp[0] == k){
                        puttemp[0] = (byte)meanH[k];
                        puttemp[1] = (byte)meanS[k];
                        puttemp[2] = (byte)meanV[k];
                        plot.put(i,j,puttemp);
                    }
                }
            }
        }
                //new Mat(cuttedHSV.rows(), cuttedHSV.cols(), CvType.CV_8UC3);
        //clusters.convertTo(ReCutPic, CvType.CV_8SC3);

        //Imgproc.cvtColor(ReCutPic, ReCutPic, Imgproc.COLOR_HSV2BGR,3);
        //Imgproc.cvtColor(ReCutPic, ReCutPic, Imgproc.COLOR_BGR2RGBA,4);

        rectangle(rot, new Point(220,350), new Point(260, 370), new Scalar(255,0,0), 1, 1);

        Imgproc.cvtColor(plot, plot, Imgproc.COLOR_HSV2BGR,3);
        Imgproc.cvtColor(plot, plot, Imgproc.COLOR_BGR2RGBA,4);
        paste(rot, plot,100,400);

        final Mat matouttemp = cuttedHSV;
        final int flen = len;
        final int fmin = min;
        final int fmax = max;
        //final String res = resultColor + ColorLine[0] + ColorLine[1] + ColorLine[2] + ColorLine[3];

        final String resultString = resultColor + "\n" + resultReg + "Ω";

        textView.post(new Runnable() {
            @SuppressLint("SetTextI18n")
            public void run() {
                //textView.setText(res + "max:"+fmax+"min:"+fmin+"len:"+flen + ", " + matouttemp.toString() + "r:" + matouttemp.rows() + " c:" + matouttemp.cols());
                textView.setText(resultString);
            }
        });

        return rot;
    }
}
