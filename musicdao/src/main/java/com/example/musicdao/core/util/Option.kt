sealed class Option<out A> {
    object None : Option<Nothing>()
    data class Value<out A>(val value: A) : Option<A>()

    companion object {
        fun <A>from(value: A?): Option<out A> {
            return value?.let { Value(value) } ?: None
        }
    }
}
