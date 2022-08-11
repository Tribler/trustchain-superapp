package nl.tudelft.trustchain.common.ebsi

import android.content.Context
import java.io.File

class WaltIdInterface(
    private val context: Context
) {
    val TAG = "WaltIdInterface"
    private val waltIdDir by lazy { File(context.filesDir, "walt_id").also { it.mkdir() } }

    private val dataDir by lazy { File(waltIdDir, "data").also { it.mkdir() } }
    val didCreatedDir by lazy { File(dataDir, "did/created").also { it.mkdir() } }
    val didResolvedDir by lazy { File(dataDir, "did/resolved").also { it.mkdir() } }
    val vcTemplatesDir by lazy { File(dataDir, "vc/templates").also { it.mkdir() } }
    val vcCreatedDir by lazy { File(dataDir, "vc/created").also { it.mkdir() } }
    val vcPresentedDir by lazy { File(dataDir, "vc/presented").also { it.mkdir() } }

    private val keyDir by lazy { File(dataDir, "key").also { it.mkdir() } }
    private val ebsiDir by lazy { File(dataDir, "ebsi").also { it.mkdir() } }
}
