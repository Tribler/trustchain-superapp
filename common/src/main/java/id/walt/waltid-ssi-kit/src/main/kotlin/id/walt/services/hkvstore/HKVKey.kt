package id.walt.services.hkvstore

import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
//import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path

class HKVKey(
    private val rootKey: String,
    vararg moreKeys: String
) {

    private val subKeys = LinkedList<String>()

    init {
        if (moreKeys.isNotEmpty())
            subKeys.addAll(moreKeys)
    }

    private fun forFS(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    fun toPath(): File {
        return File("${forFS(rootKey)}/${subKeys.map { forFS(it) }.toTypedArray().joinToString("/")}")
//        return Path.of(forFS(rootKey), *subKeys.map { forFS(it) }.toTypedArray())
    }

    override fun toString(): String = "/${rootKey}/${subKeys.joinToString("/")}"

    override fun equals(other: Any?): Boolean = if (other == null) false else other.toString() == toString()

    override fun hashCode(): Int = toString().hashCode()

    fun startsWith(key: HKVKey): Boolean {
        return toString().startsWith(key.toString())
    }

    val name: String
        get() = if (subKeys.isEmpty()) rootKey else subKeys.last

    val parent: HKVKey?
        get() = if (subKeys.isEmpty()) null else HKVKey(rootKey, *subKeys.subList(0, subKeys.size - 1).toTypedArray())

    companion object {
        private fun fromFS(value: String): String = URLDecoder.decode(
            value, StandardCharsets.UTF_8.name()
        )

        fun fromPath(path: File): HKVKey {
            return HKVKey(
                fromFS(path.path.split("/")[0]),
                *path.path.split("/").map { fromFS(it) }.toTypedArray()
//                *path.subpath(1, path.nameCount).map { fromFS(it.toString()) }.toTypedArray()
            )
        }

        fun fromString(path: String): HKVKey {
            val parts = path.split("/")
            val rootKey = parts[0]
            return HKVKey(rootKey, *parts.subList(1, parts.size).toTypedArray())
        }

        fun combine(root: HKVKey, vararg subKey: String) = HKVKey(root.rootKey, *root.subKeys.plus(subKey).toTypedArray())
    }
}
