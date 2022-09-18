package nl.tudelft.trustchain.datavault.accesscontrol

import android.util.Log
import org.json.JSONArray

class DirectoryTree(val name: String) {

    val children = mutableMapOf<String, DirectoryTree>()

    fun addPath(path: String): DirectoryTree {
//        Log.e("DirTree", "Adding path: $path")
        val pathParts = path.split("/", limit=2)
//        Log.e("DirTree", "cur: ${pathParts[0]}, next: ${pathParts[1]}")
        val current = pathParts[0]
        val currentDT = children.getOrDefault(current, DirectoryTree(current))
        if (pathParts.size == 2 && pathParts[1].isNotEmpty()) currentDT.addPath(pathParts[1])
        children[current] = currentDT
        return this
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

    fun flatten(): JSONArray {
        val childrenArray = JSONArray()
        children.values.forEach {
            childrenArray.put(it.flatten())
        }

        return JSONArray().put(name).put(childrenArray)
    }

    fun serialize(): String {
        return flatten().toString()
    }

    companion object {

        fun parse(nodeArrayString: String): DirectoryTree {
            val nodeArray = JSONArray(nodeArrayString)
            return parse(nodeArray)
        }

        fun parse(nodeArray: JSONArray): DirectoryTree {
            val currentNode = DirectoryTree(nodeArray.getString(0))
            val childrenArray = nodeArray.getJSONArray(1)
            for (i in 0 until childrenArray.length()) {
                val childArray = childrenArray.getJSONArray(i)
                val childNode = parse(childArray)
                currentNode.children[childNode.name] = childNode
            }
            return currentNode
        }
    }
}
