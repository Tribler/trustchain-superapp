package nl.tudelft.trustchain.musicdao

import android.net.Uri

object AppContainer {
    lateinit var currentCallback: (List<Uri>) -> Unit
    lateinit var activity: MusicActivity

    fun provide(_activity: MusicActivity) {
        activity = _activity
    }
}
