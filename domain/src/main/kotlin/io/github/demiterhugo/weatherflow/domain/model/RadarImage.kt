package io.github.demiterhugo.weatherflow.domain.model

/** Encoded PNG content. Decoding and validation belong to the data implementation. */
class RadarImage(bytes: ByteArray) {
    private val content = bytes.copyOf()

    init {
        require(content.isNotEmpty()) { "Radar image must not be empty." }
    }

    val byteCount: Int get() = content.size

    /** Returns a copy so consumers cannot mutate a cached sequence. */
    fun copyBytes(): ByteArray = content.copyOf()

    override fun equals(other: Any?): Boolean = other is RadarImage && content.contentEquals(other.content)
    override fun hashCode(): Int = content.contentHashCode()
}
