package app.hullbeat.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.hullbeat.data.storage.AttachmentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronously decodes and downsamples an image from [uriString] into an [ImageBitmap].
 *
 * Keeps memory bounded by calculating [BitmapFactory.Options.inSampleSize] based on
 * [maxDimension] and respects EXIF orientation.
 */
@Composable
fun rememberSampledBitmap(
    context: Context,
    uriString: String?,
    maxDimension: Int = 320,
): ImageBitmap? {
    var bitmap by remember(uriString, maxDimension) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString, maxDimension) {
        bitmap = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    decodeSampledBitmapFromUri(context, Uri.parse(uriString), maxDimension)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    maxDimension: Int,
): Bitmap? {
    return runCatching {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        AttachmentStorage.openInputStream(context, uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null

        var inSampleSize = 1
        val maxOriginal = maxOf(width, height)
        while (maxOriginal / (inSampleSize * 2) >= maxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val decoded = AttachmentStorage.openInputStream(context, uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val orientation = runCatching {
            AttachmentStorage.openInputStream(context, uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (degrees != 0f) {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) {
                decoded.recycle()
            }
            rotated
        } else {
            decoded
        }
    }.getOrNull()
}
