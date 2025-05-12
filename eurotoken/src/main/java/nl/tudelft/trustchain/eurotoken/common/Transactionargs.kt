package nl.tudelft.trustchain.eurotoken.common

import android.os.Parcel
import android.os.Parcelable

enum class Mode { SEND, RECEIVE }
enum class Channel { QR, NFC }

// parcelize didnt work? custom..

data class TransactionArgs(
    val mode: Mode,
    val channel: Channel,
    val amount: Long = 0L,
    val publicKey: String? = null,
    val name: String? = null,
    val qrData: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        Mode.valueOf(parcel.readString()!!),
        Channel.valueOf(parcel.readString()!!),
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mode.name)
        dest.writeString(channel.name)
        dest.writeLong(amount)
        dest.writeString(publicKey)
        dest.writeString(name)
        dest.writeString(qrData)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TransactionArgs> {
        override fun createFromParcel(parcel: Parcel): TransactionArgs = TransactionArgs(parcel)
        override fun newArray(size: Int): Array<TransactionArgs?> = arrayOfNulls(size)
    }
}
