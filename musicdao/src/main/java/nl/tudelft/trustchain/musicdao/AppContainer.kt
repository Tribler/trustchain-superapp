package nl.tudelft.trustchain.musicdao

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

object AppContainer {
    lateinit var currentCallback: (List<Uri>) -> Unit
    lateinit var activity: MusicActivity

    @RequiresApi(Build.VERSION_CODES.O)
    fun provide(_activity: MusicActivity) {
        activity = _activity
    }
}
