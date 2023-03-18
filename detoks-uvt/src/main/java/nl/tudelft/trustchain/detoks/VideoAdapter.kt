package nl.tudelft.trustchain.detoks

import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.*
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustchainConstants
import nl.tudelft.trustchain.detoks.helpers.DoubleClickListener

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
        var proposalSendButton: Button
        var tokensSent: TextView
        var tokensReceived: TextView
        var tokensBalance: TextView

        init {
            mVideoView = itemView.findViewById(R.id.videoView)
            txtTitle = itemView.findViewById(R.id.txtTitle)
            txtDesc = itemView.findViewById(R.id.txtDesc)
            mProgressBar = itemView.findViewById(R.id.progressBar)
            proposalBlockHash = itemView.findViewById(R.id.proposalBlockHash)
            videoID = itemView.findViewById(R.id.videoID)
            videoPostedOn = itemView.findViewById(R.id.videoPostedOn)
            proposalSendButton = itemView.findViewById(R.id.proposalMockButton)
            tokensSent = itemView.findViewById(R.id.tokensSentValue)
            tokensReceived = itemView.findViewById(R.id.tokensReceivedValue)
            tokensBalance = itemView.findViewById(R.id.tokensBalanceValue)

            setLikeListener()
            setPostVideoListener()
            checkTokenBalance()
            dailyBalanceCheckpoint()
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

        private fun dailyBalanceCheckpoint() {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()!!
            val myPublicKey = IPv8Android.getInstance().myPeer.publicKey.keyToBin()
            val latestBalanceCheckpoint = upvoteCommunity.database.getLatest(myPublicKey, UpvoteTrustchainConstants.BALANCE_CHECKPOINT)
            if (latestBalanceCheckpoint != null) {
                //if date is today then don't do anything because there is already a block for today
                if (DateUtils.isToday(latestBalanceCheckpoint.timestamp.time)) return
                val blocksToProcess = upvoteCommunity.database.crawl(myPublicKey, latestBalanceCheckpoint.sequenceNumber.toLong(), upvoteCommunity.database.getBlockCount(myPublicKey))
                var sent = latestBalanceCheckpoint.transaction.get("sent")!!.toString().toInt();
                var received = latestBalanceCheckpoint.transaction.get("received")!!.toString().toInt();
                for (block: TrustChainBlock in blocksToProcess){
                    if (block.type.equals(UpvoteTrustchainConstants.GIVE_HEART_TOKEN)) {
                        if (block.isAgreement && block.linkPublicKey.toHex().equals(myPublicKey.toHex())) {
                            received++
                        }
                        else if (block.isAgreement && block.publicKey.toHex().equals(myPublicKey.toHex())) {
                            sent++
                        }
                    }
                }
                val transaction = mapOf(
                    "sent" to sent,
                    "received" to received,
                    "balance" to received - sent
                )
                // Todo during the Wednesday groupmeeting on 15th of March 2023 we mentioned that it may be better to send the proposalblock to another peer instead?
                upvoteCommunity.createProposalBlock(UpvoteTrustchainConstants.BALANCE_CHECKPOINT, transaction, myPublicKey)
            } else {
                // get all balances
                val (sent, received, balance) = checkTokenBalance()
                val transaction = mapOf(
                    "sent" to sent,
                    "received" to received,
                    "balance" to balance
                )
                // Todo during the Wednesday groupmeeting on 15th of March 2023 we mentioned that it may be better to send the proposalblock to another peer instead?
                upvoteCommunity.createProposalBlock(UpvoteTrustchainConstants.BALANCE_CHECKPOINT, transaction, myPublicKey)
            }
        }

        private fun checkTokenBalance():Triple<Int, Int, Int> {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()!!
            val allBlocks = upvoteCommunity.database.getBlocksWithType(UpvoteTrustchainConstants.GIVE_HEART_TOKEN)
            var sent = 0;
            var received = 0;
            for (block: TrustChainBlock in allBlocks) {
                if (block.isAgreement && block.linkPublicKey.toHex().equals(IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex())) {
                    received++
                }
                else if (block.isAgreement && block.publicKey.toHex().equals(IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex())) {
                    sent++
                }
                else {
                    Log.i("DeToks", "Other type of block found: ${block.blockId}")
                }
            }
            Log.i("DeToks", "Tokens sent: $sent received: $received")
            tokensSent.text = "$sent"
            tokensReceived.text = "$received"
            tokensBalance.text = "${received - sent}"
            return Triple(sent, received, received-sent)
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
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()!!
            val toastMessage = upvoteCommunity.sendHeartToken("", "TEST")
            Toast.makeText(
                itemView.context,
                toastMessage,
                Toast.LENGTH_SHORT
            ).show()
            // getBlockHash Method below might fail to get the proposal block if it is not in this peer's truststore
            val proposalBlock = upvoteCommunity.database.getBlockWithHash(proposalBlockHash.text.toString().hexToBytes())
            if (proposalBlock != null) {
                upvoteCommunity.createAgreementBlock(proposalBlock, proposalBlock.transaction)
                Log.i("DeToks", "Agreement block created!")
            } else {
                Toast.makeText(
                    itemView.context,
                    "This video does not have a proposal block attached to it and is thus not posted by anyone",
                    Toast.LENGTH_SHORT
                ).show()
            }
            //TODO when a video is posted by peer A on phone Ap by long pressing,
            // how does peer B get the video posted by peer A on its phone Bp?
            // peer B first: needs to have peer A's posted video displayed on phone Bp
            // only when the above condition is satisfied can peer B create an agreement block
            // for peer A's proposal block that was created when peer posted a video (thus liking the video posted by peer A)

            // TODO: Currently, it seems that a peer can only like a video created by itself
            //       when we can distribute a video added by a peer to another peer B, peer B will be able to like that viedo
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
            return proposalBlock
        }

        /**
         * Sets a listener to create a proposal token after a long press has been detected.
         * We do not have a post video functionality yet
         * A long press of 2 seconds represents/simulates a peer having posted a video
         */
        private fun setPostVideoListener() {
//            itemView.setOnTouchListener(
//                object : LongHoldListener() {
//                    override fun onLongHold() {
//                        val proposalBlock = createProposalToken()
//                        val hash = proposalBlock?.calculateHash()!!
//                        val mypeer = IPv8Android.getInstance().myPeer
//                        val message = "By long pressing for 2 seconds you with public key: " +
//                            "${mypeer.publicKey.keyToBin().toHex()} and member id:\n" +
//                            "$mypeer.mid has created a proposalblock on this timestamp: ${proposalBlock.timestamp} \n" +
//                            "The hash of this block is ${hash.toHex()}, corresponding hashCode is: ${hash.hashCode()} \n" +
//                            "the block Id of this proposal block is: ${proposalBlock.blockId} \n" +
//                            "the linked block id is: ${proposalBlock.linkedBlockId}\n"
//                        torrentManager.addNewVideo(hash.toHex(), proposalBlock.timestamp.toString(), proposalBlock.blockId)
//                        Log.i("DeToks", message)
//                        Toast.makeText(
//                            itemView.context,
//                            message,
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                }
//            )

            proposalSendButton.setOnClickListener{
                val proposalBlock = createProposalToken()
                val hash = proposalBlock?.calculateHash()!!
                val myPeer = IPv8Android.getInstance().myPeer
                val message = "Button Clicked! Your public key: " +
                    "${myPeer.publicKey.keyToBin().toHex()} and member id:\n" +
                    "$myPeer.mid has created a proposalblock on this timestamp: ${proposalBlock.timestamp} \n" +
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
                createProposalToken()
            }
        }
    }
}

class VideoItem(val content: suspend (Int, Long) -> TorrentMediaInfo)
