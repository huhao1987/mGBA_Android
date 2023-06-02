package hh.game.mgba_android.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import org.libsdl.app.SDLSurface
import java.nio.ByteBuffer

class VideoUtils {
    companion object{
        fun captureScreenshot(videoView: SDLSurface, callback: (Bitmap?) -> Unit){
            usePixelCopy(videoView, callback)
        }
        private fun usePixelCopy(videoView: SDLSurface, callback: (Bitmap?) -> Unit) {
            val bitmap: Bitmap = Bitmap.createBitmap(
                videoView.width,
                videoView.height,
                Bitmap.Config.ARGB_8888
            );
            try {
                // Create a handler thread to offload the processing of the image.
                val handlerThread = HandlerThread("PixelCopier");
                handlerThread.start();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    PixelCopy.request(
                        videoView, bitmap,
                        PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                            if (copyResult == PixelCopy.SUCCESS) {
                                callback(bitmap)
                            }
                            handlerThread.quitSafely();
                        },
                        Handler(handlerThread.looper)
                    )
                }
            } catch (e: IllegalArgumentException) {
                callback(null)
                // PixelCopy may throw IllegalArgumentException, make sure to handle it
                e.printStackTrace()
            }
        }

        fun convertByteArrayToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
            val expectedSize = width * height * 4 // 4 bytes per pixel (RGBA)
            if (data.size != expectedSize) {
                Log.e("Bitmap:", "Invalid pixel data size: ${data.size}, expected: $expectedSize")
                return null
            }
            val buffer = ByteBuffer.wrap(data)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            if (bitmap == null) {
                Log.e("Bitmap:", "Failed to create Bitmap")
                return null
            }
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }
    }
}