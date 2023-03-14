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
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustchainConstants
import nl.tudelft.trustchain.detoks.helpers.DoubleClickListener
import nl.tudelft.trustchain.detoks.helpers.LongHoldListener

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
            setCreateProposalTokenListener()
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
         * Sends a HearthToken to a random user and displays the result in a toast message
         */
        private fun sendHeartToken() {

            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val toastMessage = upvoteCommunity?.sendHeartToken("", "TEST")
            Toast.makeText(
                itemView.context,
                toastMessage,
                Toast.LENGTH_SHORT
            ).show()
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

        private fun createProposalToken() {
            //TODO: create and sign proposal token with own private key
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val myPeer = IPv8Android.getInstance().myPeer

            val transaction = mapOf(
                "videoID" to "TODO: REPLACE THIS WITH ACTUAL VIDEO ID",
                "heartTokenGivenBy" to ANY_COUNTERPARTY_PK.toHex(),
                "heartTokenGivenTo" to myPeer.publicKey.keyToBin().toHex()
            )
            val proposalBlock = upvoteCommunity?.createProposalBlock(
                UpvoteTrustchainConstants.GIVE_HEART_TOKEN,
                transaction,
                myPeer.publicKey.keyToBin()
            )
            proposalBlock?.sign(myPeer.key as PrivateKey)
        }

        /**
        Sets a listener to create a proposal token after a long press has been detected.
         */
        private fun setCreateProposalTokenListener() {
            itemView.setOnTouchListener(
                object : LongHoldListener() {
                    override fun onLongHold() {
                        val message = "Long HOLD detected"
                        Log.i("DeToks", message)
                        Toast.makeText(
                            itemView.context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        createProposalToken()
                    }
                }
            )
        }
    }
}

class VideoItem(val content: suspend (Int, Long) -> TorrentMediaInfo)
