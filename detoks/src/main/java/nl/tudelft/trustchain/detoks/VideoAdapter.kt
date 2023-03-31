package nl.tudelft.trustchain.detoks

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android


class VideosAdapter(
    private val torrentManager: TorrentManager,
    private val onPlaybackError: (() -> Unit)? = null,
    private val videoScaling: Boolean = false
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

    // Taken from https://www.geeksforgeeks.org/double-tap-on-a-button-in-android/
    abstract class DoubleClickListener : View.OnClickListener {
        var lastClickTime: Long = 0

        override fun onClick(v: View?) {
            val clickTime = System.currentTimeMillis()

            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                onDoubleClick(v)
            }

            lastClickTime = clickTime
        }

        abstract fun onDoubleClick(v: View?)

        companion object {
            private const val DOUBLE_CLICK_TIME_DELTA: Long = 300 // milliseconds
        }
    }

    class VideoViewHolder(itemView: View, private val videoScaling: Boolean = false) :
        RecyclerView.ViewHolder(itemView) {
        var mVideoView: VideoView
        var txtTitle: TextView
        var txtDesc: TextView
        var peerCount: TextView
        var mProgressBar: ProgressBar
        var likeButton: ImageButton
        var likeCount: TextView
        var isLiked: Boolean = false

        private fun likeVideo(content: TorrentMediaInfo) {
            if (isLiked) return

            isLiked = true
            likeButton.setImageResource(R.drawable.baseline_favorite_24_red)

            try {
                likeCount.text = ((likeCount.text as String).toInt() + 1).toString()
            } catch (_: NumberFormatException) {
                Log.d("DeToks", "Could not update the like counter.")
            }

            val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
            community.broadcastLike(content.fileName, content.torrentName, content.creator, content.torrentMagnet)
        }

        init {
            mVideoView = itemView.findViewById(R.id.videoView)
            txtTitle = itemView.findViewById(R.id.txtTitle)
            txtDesc = itemView.findViewById(R.id.txtDesc)
            peerCount = itemView.findViewById(R.id.peerCount)
            mProgressBar = itemView.findViewById(R.id.progressBar)
            likeButton = itemView.findViewById(R.id.like_button)
            likeCount = itemView.findViewById(R.id.like_count)

            // Hide the like button until the video loads.
            likeButton.visibility = View.GONE

            // Disable the click sound effects.
            mVideoView.isSoundEffectsEnabled = false
            likeButton.isSoundEffectsEnabled = false
        }

        fun setVideoData(item: VideoItem, position: Int, onPlaybackError: (() -> Unit)? = null) {
            CoroutineScope(Dispatchers.Main).launch {
                val content = item.content(position, 10000)
                val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

                likeCount.text = community.getLikes(content.fileName, content.torrentName).size.toString()

                isLiked = community.userLikedVideo(
                    content.fileName,
                    content.torrentName,
                    community.myPeer.publicKey.toString()
                )

                if (isLiked)
                    likeButton.setImageResource(R.drawable.baseline_favorite_24_red)
                else
                    likeButton.setImageResource(R.drawable.baseline_favorite_24_white)

                // Show the like button.
                likeButton.visibility = View.VISIBLE

                likeButton.setOnClickListener{
                    likeVideo(content)
                }

                mVideoView.setOnClickListener(object: DoubleClickListener() {
                    override fun onDoubleClick(v: View?) {
                        likeVideo(content)
                    }
                })

                txtTitle.text = content.creator
                txtDesc.text = content.torrentName
                peerCount.text = "Peers: " + community.getPeers().size.toString()
                Log.d("DeToks", content.fileURI)
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
                    Log.i("DeToks", "onError: $p1, $what, $extra , ${p1.duration}")

                    if (onPlaybackError != null) {
                        onPlaybackError()
                        true
                    } else {
                        true
                    }
                }
            }
        }
    }
}

class VideoItem(val content: suspend (Int, Long) -> TorrentMediaInfo)
