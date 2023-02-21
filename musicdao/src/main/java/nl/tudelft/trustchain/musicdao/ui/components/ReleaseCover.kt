package nl.tudelft.trustchain.musicdao.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import nl.tudelft.trustchain.musicdao.R
import java.io.File

@Composable
fun ReleaseCover(file: File? = null, modifier: Modifier = Modifier) {
    if (file != null) {
        BitmapCover(file = file, modifier = modifier)
    } else {
        DefaultCover(modifier = modifier.background(Color.DarkGray))
    }
}

@Composable
fun DefaultCover(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_music),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun BitmapCover(file: File, modifier: Modifier = Modifier) {
    val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier
    )
}
