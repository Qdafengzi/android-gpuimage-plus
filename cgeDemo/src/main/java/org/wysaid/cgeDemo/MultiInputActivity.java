package org.wysaid.cgeDemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.wysaid.camera.CameraInstance;
import org.wysaid.cgeDemo.demoUtils.MultiInputDemo;
import org.wysaid.common.Common;
import org.wysaid.myUtils.ImageUtil;
import org.wysaid.view.CameraRecordGLSurfaceView;

import java.util.logging.Logger;

public class MultiInputActivity extends AppCompatActivity {

    private MultiInputDemo mCameraView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_input);

        mCameraView = (MultiInputDemo) findViewById(R.id.myGLSurfaceView);
        mCameraView.presetCameraForward(false);



        //Recording video size
        mCameraView.presetRecordingSize(2160, 2160);
        mCameraView.setFocusable(true);
        mCameraView.presetCameraForward(true);

        mCameraView.setZOrderOnTop(false);
        mCameraView.setZOrderMediaOverlay(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        CameraInstance.getInstance().stopCamera();
        Log.i(Common.LOG_TAG, "activity onPause...");
        mCameraView.release(null);
        mCameraView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        mCameraView.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void demoClicked(View view) {
        mCameraView.triggerEffect();
    }

    public void takePhoto(View view) {
        mCameraView.takePicture(new CameraRecordGLSurfaceView.TakePictureCallback() {
            @Override
            public void takePictureOK(Bitmap bmp) {
                if (bmp != null) {
                    String s = ImageUtil.saveBitmap(bmp);
                    Log.d("图片", "大小:" + bmp.getWidth() + "*" + bmp.getHeight());

                    bmp.recycle();
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + s)));
                } else {

                }
            }
        }, null, null, 1.0f, true);
    }
}
