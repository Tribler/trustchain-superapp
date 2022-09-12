package nl.tudelft.trustchain.literaturedao.controllers

import nl.tudelft.trustchain.literaturedao.*
import java.io.*

/**
 * Controller for file storage.
 * Save new files, get existing files, remove files.
 */
class FileStorageController : LiteratureDaoActivity() {

    val directory: String = "/directory/to/internal/storage/"

    /**
     * This method saves a file in the internal android storage and returns path to the file (uri).
     *
     * Throws an exception if file with the same name already exists.
     */
    // TODO: Implement this method.
    fun saveFile(@Suppress("UNUSED_PARAMETER") file: File): String {
        return "" // replace with path to file
    }

    /**
     * This method retrieves a file by uri.
     *
     * Throws exception if no file found.
     */
    // TODO: Implement this method.
    fun getFile(@Suppress("UNUSED_PARAMETER") uri: String): File {
        return File.createTempFile("", "") // replace with retrieved file
    }

    /**
     * This method removes a file by uri.
     *
     * Throws exception if no file found at uri.
     */
    // TODO: Implement this method.
    fun removeFile(@Suppress("UNUSED_PARAMETER") file: File) {
        // remove the file
    }

}
