package org.wysaid.cgeDemo.camera

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.fragment.app.Fragment
import org.wysaid.camera.CameraInstance
import org.wysaid.cgeDemo.databinding.FragmentCameraBinding
import org.wysaid.library.BuildConfig
import org.wysaid.myUtils.FileUtil
import org.wysaid.myUtils.ImageUtil
import org.wysaid.myUtils.MsgUtil
import org.wysaid.myUtils.PermissionUtil
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.view.CameraRecordGLSurfaceView.EndRecordingCallback

class CameraFragment : Fragment() {

    var lastVideoPathFileName: String = FileUtil.getPath() + "/lastVideoPath.txt"
    private val LOG_TAG = "CameraFragment"

    private fun showText(s: String) {
        mBinding.myGLSurfaceView.post { MsgUtil.toastMsg(requireContext(), s) }
    }

    lateinit var mBinding: FragmentCameraBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return mBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PermissionUtil.verifyStoragePermissions(requireActivity())
        initView()
        initListener()
    }

    private fun initView() {
        mBinding.myGLSurfaceView.presetCameraForward(true)
        mBinding.myGLSurfaceView.presetRecordingSize(3840, 2160)
        mBinding.myGLSurfaceView.setPictureSize(3840, 2160, true) // > 4MP
        mBinding.myGLSurfaceView.setZOrderOnTop(false)
        mBinding.myGLSurfaceView.setZOrderMediaOverlay(true)
        mBinding.myGLSurfaceView.setMaxPreviewSize(3840, 2160)
        mBinding.myGLSurfaceView.setFitFullView(false)
        mBinding.myGLSurfaceView.filterTouchesWhenObscured = true
        val layoutParams = mBinding.myGLSurfaceView.layoutParams

        layoutParams.width = 1440
        layoutParams.height = (1440 * 2160 / 3840f).toInt()
        mBinding.myGLSurfaceView.layoutParams = layoutParams
        mBinding.myGLSurfaceView.requestLayout()


        mBinding.compose.setContent {
            Column {
                val map =
                    mapOf(Pair(1, 1), Pair(5, 4), Pair(4, 3), Pair(16, 9), Pair(9, 16), Pair(2, 1))
                LazyRow() {
                    map.forEach { t, u ->
                        item {
                            androidx.compose.material3.Button(onClick = {
                               setAspectRatio(t,u)
                            }) {
                                androidx.compose.material3.Text(text = "$t:$u")
                            }
                        }
                    }
                }
            }
        }

    }

    private fun setAspectRatio(width: Int, height: Int) {

        val baseWidth = 2160

//        mBinding.myGLSurfaceView.pre
        val layoutParams = mBinding.myGLSurfaceView.layoutParams
        if (width > height) {
            layoutParams.width = 1440
            layoutParams.height = (1440 * height / width.toFloat()).toInt()

            mBinding.myGLSurfaceView.setMaxPreviewSize(baseWidth,(baseWidth * height / width.toFloat()).toInt())
            mBinding.myGLSurfaceView.setPictureSize(baseWidth,(baseWidth * height / width.toFloat()).toInt(), true)
        } else {
            layoutParams.height = 1440
            layoutParams.width = (1440 * width / height.toFloat()).toInt()
            mBinding.myGLSurfaceView.setMaxPreviewSize((baseWidth * width / height.toFloat()).toInt(), baseWidth)
            mBinding.myGLSurfaceView.setPictureSize((baseWidth * width / height.toFloat()).toInt(), baseWidth, true)
        }
        mBinding.myGLSurfaceView.setFitFullView(false)
        mBinding.myGLSurfaceView.layoutParams = layoutParams
        mBinding.myGLSurfaceView.requestLayout()

        mBinding.myGLSurfaceView.onPause()
        mBinding.myGLSurfaceView.onResume()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        mBinding.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mBinding.myGLSurfaceView.setFilterWithConfig("@cvlomo saturation 0.1 @adjust exposure 0 @adjust whitebalance " + progress / 100f + " 1")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })


        mBinding.takePicBtn.setOnClickListener {
            showText("Taking Picture...")
            mBinding.myGLSurfaceView.takePicture({ bmp ->
                if (bmp != null) {
                    val s = ImageUtil.saveBitmap(bmp)
                    bmp.recycle()
                    showText("Take picture success!")
                    requireContext().sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(
                                "file://$s"
                            )
                        )
                    )
                } else showText("Take picture failed!")
            }, null, null, 1.0f, true)
        }

        mBinding.takeShotBtn.setOnClickListener {
            showText("Taking Shot...")
            mBinding.myGLSurfaceView.takeShot { bmp ->
                if (bmp != null) {
                    val s = ImageUtil.saveBitmap(bmp)
                    bmp.recycle()
                    showText("Take Shot success!")
                    requireContext().sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(
                                "file://$s"
                            )
                        )
                    )
                } else showText("Take Shot failed!")
            }
        }

        mBinding.recordBtn.setOnClickListener(RecordListener)
        mBinding.myGLSurfaceView.setOnCreateCallback { Log.i(LOG_TAG, "view onCreate") }
        mBinding.pauseBtn.setOnClickListener { mBinding.myGLSurfaceView.stopPreview() }
        mBinding.resumeBtn.setOnClickListener { mBinding.myGLSurfaceView.resumePreview() }
        mBinding.fitViewBtn.setOnClickListener(object : View.OnClickListener {
            var shouldFit: Boolean = false

            override fun onClick(v: View) {
                shouldFit = !shouldFit
                mBinding.myGLSurfaceView.setFitFullView(shouldFit)
            }
        })

        mBinding.myGLSurfaceView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.i(
                        LOG_TAG,
                        String.format("Tap to focus: %g, %g", event.x, event.y)
                    )
                    val focusX = event.x / mBinding.myGLSurfaceView.width
                    val focusY = event.y / mBinding.myGLSurfaceView.height

                    mBinding.myGLSurfaceView.focusAtPoint(
                        focusX, focusY
                    ) { success, camera ->
                        if (success) {
                            Log.e(
                                LOG_TAG,
                                String.format("Focus OK, pos: %g, %g", focusX, focusY)
                            )
                        } else {
                            Log.e(
                                LOG_TAG,
                                String.format("Focus failed, pos: %g, %g", focusX, focusY)
                            )
                            mBinding.myGLSurfaceView.cameraInstance()
                                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                        }
                    }
                }

                else -> {}
            }
            true
        }
    }


    private val RecordListener = object : View.OnClickListener {
        var isValid: Boolean = true
        var recordFilename: String? = null

        override fun onClick(v: View) {
            if (!BuildConfig.CGE_USE_VIDEO_MODULE) {
                MsgUtil.toastMsg(
                    requireContext(),
                    "gradle.ext.disableVideoModule is defined to true, video module disabled!!"
                )
                return
            }

            val btn = v as Button

            if (!isValid) {
                Log.e(LOG_TAG, "Please wait for the call...")
                return
            }

            isValid = false

            if (!mBinding.myGLSurfaceView.isRecording) {
                btn.text = "Recording"
                Log.i(LOG_TAG, "Start recording...")
                recordFilename = ImageUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4"
                //                recordFilename = ImageUtil.getPath(CameraDemoActivity.this, false) + "/rec_1.mp4";
                mBinding.myGLSurfaceView.startRecording(recordFilename) { success ->
                    if (success) {
                        showText("Start recording OK")
                        FileUtil.saveTextContent(recordFilename, lastVideoPathFileName)
                    } else {
                        showText("Start recording failed")
                    }
                    isValid = true
                }
            } else {
                showText("Recorded as: $recordFilename")
                btn.text = "Recorded"
                Log.i(LOG_TAG, "End recording...")
                mBinding.myGLSurfaceView.endRecording(EndRecordingCallback {
                    Log.i(LOG_TAG, "End recording OK")
                    isValid = true
                })
            }
        }
    }


    var customFilterIndex: Int = 0

    fun customFilterClicked(view: View?) {
        ++customFilterIndex
        customFilterIndex %= CGENativeLibrary.cgeGetCustomFilterNum()
        mBinding.myGLSurfaceView.queueEvent {
            val customFilter =
                CGENativeLibrary.cgeCreateCustomNativeFilter(customFilterIndex, 1.0f, true)
            mBinding.myGLSurfaceView.recorder.setNativeFilter(customFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        CameraInstance.getInstance().stopCamera()
        Log.i(LOG_TAG, "activity onPause...")
        mBinding.myGLSurfaceView.release(null)
        mBinding.myGLSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mBinding.myGLSurfaceView.onResume()
    }

}