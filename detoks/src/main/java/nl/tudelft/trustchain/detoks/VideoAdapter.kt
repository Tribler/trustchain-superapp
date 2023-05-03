package nl.tudelft.trustchain.detoks

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.frostwire.jlibtorrent.TorrentHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class VideosAdapter(
    private val torrentManager: TorrentManager,
    private val onPlaybackError: (() -> Unit)? = null,
    private val videoScaling: Boolean = false,
) :
    RecyclerView.Adapter<VideosAdapter.VideoViewHolder?>() {
    private val mVideoItems: List<VideoItem> =
        List(100) { VideoItem(torrentManager::provideContent) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        createLoadingView(torrentManager.getCurrentHandler(), view)

        return VideoViewHolder(view, videoScaling)
    }


    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        Log.i("DeToks", "onBindViewHolder: $position")
        holder.setVideoData(mVideoItems[position], position, onPlaybackError)
    }

    override fun getItemCount(): Int {
        return mVideoItems.size
    }

    private fun createLoadingView(handler: TorrentManager.TorrentHandler, view: View) {
        val torrentNameTV: TextView = view.findViewById(R.id.torrentName)
        torrentNameTV.text = handler.torrentName

        val fileNameTV: TextView = view.findViewById(R.id.fileNameTV)
        fileNameTV.text = handler.fileName

        val fileSeedsTV: TextView = view.findViewById(R.id.fileSeedsTV)
        val filePeersTV: TextView = view.findViewById(R.id.filePeersTV)
        val downloadRateTV: TextView = view.findViewById(R.id.downloadRateTV)
        val fileSizeTV: TextView = view.findViewById(R.id.fileSizeTV)
        val startTimeTV: TextView = view.findViewById(R.id.elapsedTimeTV)
        val progress1MBTV: ProgressBar = view.findViewById(R.id.progress1MBTV)
        val progressTotalTV: ProgressBar = view.findViewById(R.id.progressTotalTV)
        val remainingSizeTV: TextView = view.findViewById(R.id.dhtPeersTV)
        val doneDownloadingTV: TextView = view.findViewById(R.id.doneDownloading)

        val currentTime = System.currentTimeMillis()
        val fileSize = handler.getFileSize()
        val toKb = 1000
        val firstMB = 10000000.0

        val handlerMain = Handler((Looper.getMainLooper()))
        val loadingUpdate: Runnable = object: Runnable {
            @SuppressLint("SetTextI18n")
            override fun run() {
                val status = handler.handle.status(TorrentHandle.QUERY_ACCURATE_DOWNLOAD_COUNTERS)
                fileSeedsTV.text = status.numSeeds().toString()
                filePeersTV.text = status.numPeers().toString()

                downloadRateTV.text = "%.2f".format(status.downloadRate() / toKb.toDouble())
                fileSizeTV.text = "%.2f".format(fileSize / firstMB)
                startTimeTV.text = ((System.currentTimeMillis() - currentTime) / toKb).toString()

                val downloaded = handler.handle.fileProgress()[handler.fileIndex]
                if (downloaded > firstMB) progress1MBTV.progress = 100
                else progress1MBTV.progress = ((downloaded/ firstMB) * 100).toInt()

                progressTotalTV.progress = ((downloaded/fileSize.toDouble()) * 100).toInt()
                remainingSizeTV.text = ((fileSize - downloaded) / firstMB).toString()

                if (handler.isDownloaded()) {
                    doneDownloadingTV.visibility = View.VISIBLE
                    return
                }
                handlerMain.postDelayed(this, 10L)
            }
        }
        handlerMain.post(loadingUpdate)
    }

    class VideoViewHolder(itemView: View, private val videoScaling: Boolean = false) :
        RecyclerView.ViewHolder(itemView) {
        var mVideoView: VideoView
        var txtTitle: TextView
        var txtDesc: TextView
        var progressBarLayout: LinearLayout

        init {
            mVideoView = itemView.findViewById(R.id.videoView)
            txtTitle = itemView.findViewById(R.id.txtTitle)
            txtDesc = itemView.findViewById(R.id.txtDesc)
            progressBarLayout = itemView.findViewById(R.id.progressBarLayout)
        }

        fun setVideoData(item: VideoItem, position: Int, onPlaybackError: (() -> Unit)? = null) {
            CoroutineScope(Dispatchers.Main).launch {
                val content = item.content(position, 10000)
                txtTitle.text = content.fileName
                txtDesc.text = content.torrentName
                mVideoView.setVideoPath(content.fileURI)
                mVideoView.setOnPreparedListener { mp ->
                    progressBarLayout.visibility = View.GONE
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
                val bundle = Bundle()
                bundle.putString("video_name", txtTitle.text.toString())
                txtTitle.setOnClickListener { p0 ->
                    p0!!.findNavController().navigate(R.id.action_toTorrentFragment, bundle)
                }
            }
        }
    }
}

class VideoItem(val content: suspend (Int, Long) -> TorrentMediaInfo)
