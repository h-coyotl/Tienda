package com.tienda.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import com.tienda.R
import java.io.ByteArrayOutputStream
import kotlin.math.max

class ImageUtils {
    enum class CompressFormat { PNG, JPEG, WEBP_LOSSY }

    companion object {
        // Cargar un Bitmap desde un Uri
        fun loadBitmapFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
            } catch (e: Exception) {
                null
            }
        }

        // Redimensionar un Bitmap (mantiene proporciones)
        fun resizeBitmap(src: Bitmap, maxDim: Int = 256): Bitmap {
            val w = src.width
            val h = src.height
            val scale = if (w >= h) maxDim.toFloat() / max(1, w) else maxDim.toFloat() / max(1, h)
            if (scale >= 1f) return src
            val nw = (w * scale).toInt().coerceAtLeast(1)
            val nh = (h * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(src, nw, nh, true)
        }

        // Bitmap -> ByteArray
        fun bitmapToBytes(
            bmp: Bitmap,
            format: CompressFormat = CompressFormat.WEBP_LOSSY,
            quality: Int = 70
        ): ByteArray {
            val out = ByteArrayOutputStream()
            when (format) {
                CompressFormat.PNG -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                CompressFormat.JPEG -> bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
                CompressFormat.WEBP_LOSSY -> bmp.compress(Bitmap.CompressFormat.WEBP, quality, out)
            }
            return out.toByteArray()
        }

        // ByteArray -> Base64
        fun encodeToBase64(bytes: ByteArray): String {
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        // Base64 -> Bitmap
        fun decodeBase64ToBitmap(base64: String): Bitmap? {
            return try {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            } catch (e: Exception) {
                null
            }
        }

        fun getDefaultImageBase64(context: Context): String {
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.placeholder)
            val resized = ImageUtils.resizeBitmap(bmp)
            val bytes = ImageUtils.bitmapToBytes(resized)
            return ImageUtils.encodeToBase64(bytes)
        }

    }
}