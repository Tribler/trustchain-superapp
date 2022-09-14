package nl.tudelft.trustchain.datavault.accesscontrol

import android.util.Log

public class DirectoryTree(val name: String) {

    val children = mutableMapOf<String, DirectoryTree>()

    fun addPath(path: String) {
        Log.e("DirTree", "Adding path: $path")
        val pathParts = path.split("/", limit=2)
//        Log.e("DirTree", "cur: ${pathParts[0]}, next: ${pathParts[1]}")
        val current = pathParts[0]
        val currentDT = children.getOrDefault(current, DirectoryTree(current))
        if (pathParts.size == 2 && pathParts[1].isNotEmpty()) currentDT.addPath(pathParts[1])
        children[current] = currentDT
    }

    fun contains(path: String): Boolean {
        Log.e("DirTree", "Checking path: $path")
        val pathParts = path.split("/", limit=2)
//        Log.e("DirTree", "cur: ${pathParts[0]}, next: ${pathParts[1]}")
        val current = pathParts[0]

        if (children.contains(current)) {
            if (pathParts.size == 1 || pathParts[1].isEmpty()) return true
            return children[current]!!.contains(pathParts[1])
        }

        return false
    }
}
