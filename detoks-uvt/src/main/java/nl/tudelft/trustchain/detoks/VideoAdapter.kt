package nl.tudelft.trustchain.detoks

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.helpers.DoubleClickListener
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import java.text.SimpleDateFormat
import java.util.*

class VideosAdapter(
    private val torrentManager: TorrentManager,
    private val onPlaybackError: (() -> Unit)? = null,
    private val videoScaling: Boolean = false,
) :
    RecyclerView.Adapter<VideosAdapter.VideoViewHolder?>() {
    private val mVideoItems: List<VideoItem> =
        List(100) { VideoItem(torrentManager::provideContent) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video, parent, false),
            videoScaling,
        )
    }


    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        Log.i("DeToks", "onBindViewHolder: $position")
        holder.setVideoData(mVideoItems[position], position, onPlaybackError)
    }

    override fun getItemCount(): Int {
        return mVideoItems.size
    }

    class VideoViewHolder(itemView: View, private val videoScaling: Boolean = false) :
        RecyclerView.ViewHolder(itemView) {
        var mVideoView: VideoView
        var txtTitle: TextView
        var txtDesc: TextView
        var mProgressBar: ProgressBar

        init {
            mVideoView = itemView.findViewById(R.id.videoView)
            txtTitle = itemView.findViewById(R.id.txtTitle)
            txtDesc = itemView.findViewById(R.id.txtDesc)
            mProgressBar = itemView.findViewById(R.id.progressBar)
            setLikeListener()
        }

        fun setVideoData(item: VideoItem, position: Int, onPlaybackError: (() -> Unit)? = null) {
            CoroutineScope(Dispatchers.Main).launch {
                val content = item.content(position, 10000)
                txtTitle.text = content.fileName
                txtDesc.text = content.torrentName
                mVideoView.setVideoPath(content.fileURI)
                Log.i("DeToks", "Received content: ${content.fileURI}")
                mVideoView.setOnPreparedListener { mp ->
                    mProgressBar.visibility = View.GONE
                    mp.start()
                    if (videoScaling) {
                        val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
                        val screenRatio = mVideoView.width / mVideoView.height.toFloat()
                        val scale = videoRatio / screenRatio
                        if (scale >= 1f) {
                            mVideoView.scaleX = scale
                        } else {
                            mVideoView.scaleY = 1f / scale

                        }
                    }
                }
                mVideoView.setOnCompletionListener { mp -> mp.start() }
                mVideoView.setOnErrorListener { p1, what, extra ->
                    Log.i("DeToks", "onError: $p1, $what, $extra")
                    if (onPlaybackError != null) {
                        onPlaybackError()
                        true
                    } else {
                        true
                    }
                }
            }
        }

        /**
         * Sends a HeartToken to a random user and displays the result in a toast message
         */
        private fun sendHeartToken() {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val upvoteToken = UpvoteToken(1, "1679006615", "12345678910", 1)
            val toastMessage = upvoteCommunity?.sendHeartToken(upvoteToken.tokenID.toString(), localToGMT(upvoteToken.date.toLong()).toString(), upvoteToken.publicKeyMinter, upvoteToken.videoID.toString())

            val lastUpvoteToken = SentTokenManager(itemView.context).getLastToken()

            if (lastUpvoteToken.tokenID > -1 && lastUpvoteToken.tokenID < 10) {
                //Since we have a string as date in our database (for now), we first need to
                //parse it into a date, which then can give us the time in millis.
                //We then can turn the time in millis into the GMT timezone and check if it's
                //before or after the current time.
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val upvoteTokenDate = formatter.parse(lastUpvoteToken.date)
                val dateInMillis = upvoteTokenDate?.time
                if (dateInMillis != null) {
                    if (localToGMT(dateInMillis) <= Date().time) {
                        Toast.makeText(
                            itemView.context,
                            toastMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    itemView.context,
                    "Minted tokenID is INVALID!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        private fun localToGMT(time: Long): Long {
            try {
                val dateFormat =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val date = Date(time)
                dateFormat.timeZone = TimeZone.getTimeZone("GMT")
                val strDate = dateFormat.format(date)
                //            System.out.println("Local Millis * " + date.getTime() + "  ---UTC time  " + strDate);//correct
                val dateFormatLocal =
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val utcDate = dateFormatLocal.parse(strDate)
                //            System.out.println("UTC Millis * " + utcDate.getTime() + " ------  " + dateFormatLocal.format(utcDate));

                return utcDate?.time!!
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return time
        }

        /**
         * Sets a listener to like a video by double tapping the screen
         */
        private fun setLikeListener() {

            val adapter = this

            itemView.setOnClickListener(
                object : DoubleClickListener() {
                    override fun onDoubleClick(view: View?) {
                        adapter.sendHeartToken()
                    }
                }
            )
        }
    }
}

class VideoItem(val content: suspend (Int, Long) -> TorrentMediaInfo)
