package id.walt.services.hkvstore

import android.content.Context
import com.sksamuel.hoplite.ConfigLoader
import id.walt.servicematrix.ServiceConfiguration
import io.ipfs.multibase.binary.Base32
import io.ktor.util.*
import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

data class FilesystemStoreConfig(
    val dataRoot: String
) : ServiceConfiguration {
    val dataDirectory by lazy { File(dataRoot).also { it.mkdir() } }
}

class FileSystemHKVStore(context: Context?, configPath: String) : HKVStoreService() {

    override lateinit var configuration: FilesystemStoreConfig

    constructor(config: FilesystemStoreConfig) : this(null,"") {
        configuration = config
    }

    private val logger = KotlinLogging.logger("FileSystemHKVStore")

    init {
        if (context != null && configPath.isNotEmpty()) {
            androidContext = context
//            configuration = fromConfiguration(configPath)
            configuration = FilesystemStoreConfig("") // TODO READ FROM ASSETS
        }

    }

    private val mappingFilePath by lazy {
        File(configuration.dataDirectory, "hash-mappings.properties").apply {
            if (!exists()) Properties().store(this.bufferedWriter(), hashMappingDesc)
        }
        /*configuration.dataDirectory.resolve("hash-mappings.properties").apply {
            if (notExists()) Properties().store(this.bufferedWriter(), hashMappingDesc)
        }*/
    }
    private val mappingProperties = lazy {
        if (!mappingFilePath.exists()) Properties().store(mappingFilePath.bufferedWriter(), hashMappingDesc)

        Properties().apply { load(mappingFilePath.bufferedReader()) }
    }

    private fun storeMappings() = mappingProperties.value.store(mappingFilePath.bufferedWriter(), hashMappingDesc)

    private fun storeHashMapping(keyName: String, hashMapping: String) {
        logger.debug { "Mapping \"$keyName\" to \"$hashMapping\"" }
        mappingProperties.value[hashMapping] = keyName
        storeMappings()
    }

    private fun retrieveHashMapping(hashMapping: String): String = mappingProperties.value[hashMapping] as String

    private fun hashIfNeeded(path: File): File {
        if (path.name.length > MAX_KEY_SIZE) {
            val hashedFileNameBytes = DigestUtils.sha3_512(path.nameWithoutExtension)
            val hashedFileName = Base32().encodeToString(hashedFileNameBytes).replace("=", "").replace("+", "")

            //val ext = extension

            val newName = hashedFileName //+ (ext.ifBlank { "" })

            val newPath = File(path.parent, newName)

            storeHashMapping(path.nameWithoutExtension, newName)

            logger.debug { "File mapping is hashed: Path was \"${path.absolutePath}\", new path is $newPath" }
            return dataDirCombinePath(dataDirRelativePath(newPath))
        }
        return path
    }

    private fun getFinalPath(keyPath: File): File = hashIfNeeded(dataDirCombinePath(keyPath))

    override fun put(key: HKVKey, value: ByteArray) {
        getFinalPath(key.toPath()).apply {
            parentFile?.mkdirs()
            if (parentFile?.exists() == true) writeBytes(value)
        }
    }

    override fun getAsByteArray(key: HKVKey): ByteArray? = getFinalPath(key.toPath()).run {
        return when (exists()) {
            true -> readBytes()
            else -> null
        }
    }

    override fun listChildKeys(parent: HKVKey, recursive: Boolean): Set<HKVKey> =
        getFinalPath(parent.toPath()).listFiles().let { pathFileList ->
            when (recursive) {
                false -> pathFileList?.filter { it.isFile }?.map {
                    var mapping = it
                    if (mapping.name.length > MAX_KEY_SIZE) {
                        mapping = File(mapping.parent, retrieveHashMapping(mapping.name))
                    }

                    HKVKey.fromPath(dataDirRelativePath(mapping))
                }?.toSet()
                true -> pathFileList?.flatMap {
                    var mapping = it
                    if (mapping.name.length > MAX_KEY_SIZE) {
                        mapping = File(mapping.parent, retrieveHashMapping(mapping.name))
                    }

                    HKVKey.fromPath(dataDirRelativePath(mapping)).let { currentPath ->
                        when {
                            it.isFile -> setOf(currentPath)
                            else -> listChildKeys(currentPath, true)
                        }
                    }
                }?.toSet()
            } ?: emptySet()
        }

    override fun delete(key: HKVKey, recursive: Boolean): Boolean =
        getFinalPath(key.toPath()).run { if (recursive) deleteRecursively() else delete() }

    private fun dataDirRelativePath(file: File) = configuration.dataDirectory.relativeTo(file)
    private fun dataDirRelativePath(path: String) = configuration.dataDirectory.relativeTo(File(path))
    private fun dataDirCombinePath(key: File) = configuration.dataDirectory.combineSafe(key.path)

    companion object {
        private const val MAX_KEY_SIZE = 100
        private const val hashMappingDesc = "FileSystemHKVStore hash mappings properties"
    }
}
