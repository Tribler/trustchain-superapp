package nl.tudelft.trustchain.literaturedao.controllers

import nl.tudelft.trustchain.literaturedao.*
import java.io.*

/**
 * Controller for file storage.
 * Save new files, get existing files, remove files.
 */
class FileStorageController : LiteratureDaoActivity() {

    val directory : String = "/direcotry/to/internal/storage/"

    /**
     * This method saves a file in the internal android storage and returns path to the file (uri).
     *
     * Throws an exception if file with the same name already exists.
     */
    fun saveFile(file: File) : String {
        return "" // replace with path to file
    }

    /**
     * This method retrieves a file by uri.
     *
     * Throws exception if no file found.
     */
    fun getFile(uri: String) : File {
        return File.createTempFile("", "") // replace with retrieved file
    }

    /**
     * This method removes a file by uri.
     *
     * Throws exception if no file found at uri.
     */
    fun removeFile(file: File) {
        // remove the file
    }

}
