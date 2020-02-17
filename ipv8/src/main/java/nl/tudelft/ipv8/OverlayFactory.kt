package nl.tudelft.ipv8

open class OverlayFactory<T : Overlay>(
    val overlayClass: Class<T>
) {
    open fun create(): T {
        return overlayClass.newInstance()
    }
}
