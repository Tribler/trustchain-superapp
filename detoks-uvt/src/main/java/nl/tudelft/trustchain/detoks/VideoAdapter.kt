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
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
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
            torrentManager)
    }


    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        Log.i("DeToks", "onBindViewHolder: $position")
        holder.setVideoData(mVideoItems[position], position, onPlaybackError)
    }

    override fun getItemCount(): Int {
        return mVideoItems.size
    }

    class VideoViewHolder(itemView: View,
                          private val videoScaling: Boolean = false,
                          private val torrentManager: TorrentManager) :
        RecyclerView.ViewHolder(itemView) {
        var mVideoView: VideoView
        var txtTitle: TextView
        var txtDesc: TextView
        var mProgressBar: ProgressBar
        var proposalBlockHash: TextView
        var videoID: TextView
        var videoPostedOn: TextView
        init {
            mVideoView = itemView.findViewById(R.id.videoView)
            txtTitle = itemView.findViewById(R.id.txtTitle)
            txtDesc = itemView.findViewById(R.id.txtDesc)
            mProgressBar = itemView.findViewById(R.id.progressBar)
            proposalBlockHash = itemView.findViewById(R.id.proposalBlockHash)
            videoID = itemView.findViewById(R.id.videoID)
            videoPostedOn = itemView.findViewById(R.id.videoPostedOn)
            setLikeListener()
            setPostVideoListener()
        }

        fun setVideoData(item: VideoItem, position: Int, onPlaybackError: (() -> Unit)? = null) {
            CoroutineScope(Dispatchers.Main).launch {
                val content = item.content(position, 10000)
                txtTitle.text = content.fileName
                txtDesc.text = content.torrentName
                mVideoView.setVideoPath(content.fileURI)
                proposalBlockHash.text = content.proposalBlockHash
                videoPostedOn.text = content.videoPostedOn
                videoID.text = content.videoID
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
         * TODO: change this function so that:
         * - it gets the hash or proposal block of the video that is currently displayed on the
         * scree
         * - checks if this peer/user already liked or already created a proposal block for the
         * proposal block of this video -> if already liked once => show message to user / cannot like again
         * - if not then create an agreement block for this video
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

        /**
         * This function is to be used when a peer posts video with video ID X:
         * the idea is that a proposal block is created everytime a peer posts a video
         * This proposal block is signed by the peer/user that uploaded the video.
         * This proposal block can be paired with an agreement block created and signed by
         * anyone who gives the video X a like/upvote.
         * (except the peer that initiated the proposal block)
         */
        private fun createProposalToken(): TrustChainBlock? {
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
                ANY_COUNTERPARTY_PK
            )
           //TODO: attach the created proposal block OR the hash of this created proposal block
           //       to the video item that is showed on the screen
           //       when a peer/user double clicks on the screen, create an agreement block for this
           //       proposal block
            return proposalBlock
        }

        /**
         * Sets a listener to create a proposal token after a long press has been detected.
         * We do not have a post video functionality yet
         * A long press of 2 seconds represents/simulates a peer having posted a video
         */
        private fun setPostVideoListener() {
            itemView.setOnTouchListener(
                object : LongHoldListener() {
                    override fun onLongHold() {
                        val proposalBlock = createProposalToken()
                        val hash = proposalBlock?.calculateHash()!!
                        val mypeer = IPv8Android.getInstance().myPeer
                        val message = "By long pressing for 2 seconds you with public key: " +
                            "${mypeer.publicKey.keyToBin().toHex()} and member id:\n" +
                            "$mypeer.mid has created a proposalblock on this timestamp: ${proposalBlock.timestamp} \n" +
                            "The hash of this block is ${hash.toHex()}, corresponding hashCode is: ${hash.hashCode()} \n" +
                            "the block Id of this proposal block is: ${proposalBlock.blockId} \n" +
                            "the linked block id is: ${proposalBlock.linkedBlockId}\n"
                        torrentManager.addNewVideo(hash.toHex(), proposalBlock.timestamp.toString(), proposalBlock.blockId)
                        Log.i("DeToks", message)
                        Toast.makeText(
                            itemView.context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

class VideoItem(val content: suspend (Int, Long) -> TorrentMediaInfo)
