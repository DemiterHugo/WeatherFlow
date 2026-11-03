package io.github.demiterhugo.weatherflow.data.radar.local

import io.github.demiterhugo.weatherflow.data.radar.InvalidRadarImageException
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.time.format.DateTimeParseException
import java.util.zip.ZipException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

internal class RadarDiskCache(
    private val directory: File,
    private val codec: RadarSnapshotCodec,
    private val maxBytes: Long = 8L * 1024 * 1024,
    private val maxEntries: Int = 4,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : RadarCache {
    private val mutex = Mutex()
    private val revision = MutableStateFlow(0L)
    private var initialized = false

    init {
        require(maxBytes > 0 && maxEntries > 0)
    }

    override fun observe(coordinates: Coordinates): Flow<RadarSequence?> =
        revision.map { read(coordinates) }.distinctUntilChanged().flowOn(io)

    override suspend fun read(coordinates: Coordinates): RadarSequence? = withContext(io) {
        mutex.withLock {
            initialize()
            readLocked(coordinates)
        }
    }

    override suspend fun save(sequence: RadarSequence) = withContext(io) {
        mutex.withLock {
            initialize()
            val previous = readLocked(sequence.coordinates)
            if (previous != null && previous.manifestGeneratedAt > sequence.manifestGeneratedAt) return@withLock
            val destination = file(sequence.coordinates)
            val temporary = File.createTempFile("radar-", ".tmp", directory)
            try {
                codec.write(sequence, temporary)
                if (temporary.length() > MAX_SNAPSHOT_BYTES || temporary.length() > maxBytes) {
                    throw IOException("Radar snapshot exceeds cache budget")
                }
                currentCoroutineContext().ensureActive()
                makeRoom(temporary.length(), destination)
                currentCoroutineContext().ensureActive()
                // Both files are on the same filesystem; readers see the old or complete new snapshot.
                Files.move(temporary.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
                revision.value++
            } finally {
                temporary.delete()
            }
        }
    }

    private fun initialize() {
        if (initialized) return
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot open radar cache")
        entries().filter { it.name.endsWith(".tmp") }.forEach {
            if (!it.delete()) throw IOException("Cannot clean interrupted radar download")
        }
        makeRoom(0, null)
        initialized = true
    }

    private fun readLocked(coordinates: Coordinates): RadarSequence? {
        val source = file(coordinates)
        if (!source.exists()) return null
        try {
            if (source.length() > MAX_SNAPSHOT_BYTES) throw InvalidRadarSnapshotException()
            return codec.read(source).also { if (it.coordinates != coordinates) throw InvalidRadarSnapshotException() }
        } catch (e: Exception) {
            if (e !is InvalidRadarSnapshotException && e !is InvalidRadarImageException && e !is ZipException &&
                e !is EOFException && e !is SerializationException && e !is IllegalArgumentException && e !is DateTimeParseException
            ) throw e
            if (!source.delete()) throw IOException("Cannot remove corrupt radar snapshot", e)
            revision.value++
            return null
        }
    }

    private fun makeRoom(incomingBytes: Long, destination: File?) {
        val snapshots = entries().filter { it.extension == "radar" && it != destination }.sortedBy { it.lastModified() }.toMutableList()
        var bytes = snapshots.sumOf { it.length() } + incomingBytes
        val incomingEntries = if (destination == null) 0 else 1
        while (bytes > maxBytes || snapshots.size + incomingEntries > maxEntries) {
            val oldest = snapshots.removeFirstOrNull() ?: throw IOException("Radar cache budget exhausted")
            val length = oldest.length()
            if (!oldest.delete()) throw IOException("Cannot evict radar snapshot")
            bytes -= length
            revision.value++
        }
    }

    private fun entries(): List<File> = directory.listFiles()?.toList() ?: throw IOException("Cannot list radar cache")

    private fun file(coordinates: Coordinates): File {
        val key = "${coordinates.latitude.toBits()}:${coordinates.longitude.toBits()}".encodeToByteArray()
        val name = MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
        return File(directory, "$name.radar")
    }

    private companion object {
        const val MAX_SNAPSHOT_BYTES = 4L * 1024 * 1024
    }
}
