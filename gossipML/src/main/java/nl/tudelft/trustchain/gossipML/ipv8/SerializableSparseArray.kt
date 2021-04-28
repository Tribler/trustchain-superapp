package nl.tudelft.trustchain.gossipML.ipv8

import android.util.SparseArray
import kotlinx.serialization.*
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * @author Asaf Pinhassi www.mobiledev.co.il https://stackoverflow.com/a/21574953
 * @param <E>
 */
@Serializable
class SerializableSparseArray<E> : SparseArray<E> {
    constructor(capacity: Int) : super(capacity) {}
    constructor() : super() {}

    /**
     * This method is private but it is called using reflection by java
     * serialization mechanism. It overwrites the default object serialization.
     *
     * **IMPORTANT**
     * The access modifier for this method MUST be set to **private** otherwise [java.io.StreamCorruptedException]
     * will be thrown.
     *
     * @param oos
     * the stream the data is stored into
     * @throws IOException
     * an exception that might occur during data storing
     */
    @Throws(IOException::class)
    private fun writeObject(oos: ObjectOutputStream) {
        val data = arrayOfNulls<Any>(size())
        for (i in data.indices.reversed()) {
            val pair = arrayOf(keyAt(i), valueAt(i))
            data[i] = pair
        }
        oos.writeObject(data)
    }

    /**
     * This method is private but it is called using reflection by java
     * serialization mechanism. It overwrites the default object serialization.
     *
     * <br></br><br></br>**IMPORTANT**
     * The access modifier for this method MUST be set to **private** otherwise [java.io.StreamCorruptedException]
     * will be thrown.
     *
     * @param oos
     * the stream the data is read from
     * @throws IOException
     * an exception that might occur during data reading
     * @throws ClassNotFoundException
     * this exception will be raised when a class is read that is
     * not known to the current ClassLoader
     * @throws ClassCastException
     * thrown when cast to E does not succeed
     */
    @Throws(IOException::class, ClassNotFoundException::class, ClassCastException::class)
    private fun readObject(ois: ObjectInputStream) {
        val data = ois.readObject() as Array<*>
        for (i in data.indices.reversed()) {
            val pair = data[i] as Array<*>
            val key = pair[0] as Int
            @Suppress("UNCHECKED_CAST") val value = pair[1] as E
            this.append(key, value)
        }
        return
    }

    companion object {
        private const val serialVersionUID = 824056059663678000L
    }
}
