package io.github.demiterhugo.weatherflow.data.radar.local

import io.github.demiterhugo.weatherflow.data.radar.RadarImageDecoder
import io.github.demiterhugo.weatherflow.data.radar.remote.MAX_RADAR_IMAGE_BYTES
import io.github.demiterhugo.weatherflow.data.radar.remote.RADAR_IMAGE_SIZE
import io.github.demiterhugo.weatherflow.data.radar.remote.RADAR_ZOOM
import io.github.demiterhugo.weatherflow.domain.model.Coordinates
import io.github.demiterhugo.weatherflow.domain.model.RadarFrame
import io.github.demiterhugo.weatherflow.domain.model.RadarSequence
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class InvalidRadarSnapshotException : IOException("Invalid radar snapshot")

/** A versioned ZIP keeps metadata and PNGs in one atomically replaceable file, with CRC checks. */
internal class RadarSnapshotCodec(private val decoder: RadarImageDecoder) {
    private val json = Json { ignoreUnknownKeys = true }

    fun write(sequence: RadarSequence, file: File) {
        val metadata = Metadata(
            version = 1,
            latitude = sequence.coordinates.latitude,
            longitude = sequence.coordinates.longitude,
            zoom = sequence.zoom,
            imageSize = sequence.imageSize,
            generatedAt = sequence.manifestGeneratedAt.toString(),
            fetchedAt = sequence.fetchedAt.toString(),
            coverageCheckedAt = sequence.coverage.checkedAt.toString(),
            frames = sequence.frames.map { FrameMetadata(it.sourceId, it.generatedAt.toString()) },
        )
        FileOutputStream(file).use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                zip.entry("metadata.json", json.encodeToString(metadata).encodeToByteArray())
                sequence.frames.forEachIndexed { index, frame -> zip.entry("frame-$index.png", frame.image.copyBytes()) }
                zip.entry("coverage.png", sequence.coverage.mask.copyBytes())
                zip.finish()
                zip.flush()
                output.fd.sync()
            }
        }
    }

    fun read(file: File): RadarSequence = ZipInputStream(file.inputStream().buffered()).use { zip ->
        val metadata = json.decodeFromString<Metadata>(zip.entry("metadata.json", 16 * 1024).decodeToString())
        if (metadata.version != 1 || metadata.zoom != RADAR_ZOOM || metadata.imageSize != RADAR_IMAGE_SIZE ||
            metadata.frames.size !in 1..6
        ) throw InvalidRadarSnapshotException()
        val frames = metadata.frames.mapIndexed { index, frame ->
            RadarFrame(
                frame.sourceId,
                Instant.parse(frame.generatedAt),
                decoder.image(zip.entry("frame-$index.png", MAX_RADAR_IMAGE_BYTES)),
            )
        }
        val coverage = decoder.coverage(
            zip.entry("coverage.png", MAX_RADAR_IMAGE_BYTES),
            Instant.parse(metadata.coverageCheckedAt),
        )
        if (zip.nextEntry != null) throw InvalidRadarSnapshotException()
        RadarSequence(
            coordinates = Coordinates(metadata.latitude, metadata.longitude),
            zoom = metadata.zoom,
            imageSize = metadata.imageSize,
            manifestGeneratedAt = Instant.parse(metadata.generatedAt),
            fetchedAt = Instant.parse(metadata.fetchedAt),
            frames = frames,
            coverage = coverage,
        )
    }

    private fun ZipOutputStream.entry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun ZipInputStream.entry(name: String, maxBytes: Int): ByteArray {
        if (nextEntry?.name != name) throw InvalidRadarSnapshotException()
        return readBounded(maxBytes)
    }

    private fun InputStream.readBounded(limit: Int): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) return result.toByteArray()
            if (result.size() + count > limit) throw InvalidRadarSnapshotException()
            result.write(buffer, 0, count)
        }
    }

    @Serializable
    private data class Metadata(
        val version: Int,
        val latitude: Double,
        val longitude: Double,
        val zoom: Int,
        val imageSize: Int,
        val generatedAt: String,
        val fetchedAt: String,
        val coverageCheckedAt: String,
        val frames: List<FrameMetadata>,
    )

    @Serializable
    private data class FrameMetadata(val sourceId: String, val generatedAt: String)
}
