package audio

import org.lwjgl.openal.EXTThreadLocalContext
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.stb.STBVorbis
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.openrndr.events.Event
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class VorbisTrack internal constructor(filePath: String) {
    private var encodedAudio: ByteBuffer? = null
    private var handle: Long = 0
    var channels = 0
    var sampleRate = 0
    val samplesLength: Int
    val samplesSec: Float
    val sampleIndex: AtomicInteger

    private var audioRenderer: AudioRenderer? = null
    private var audioThread: Thread? = null

    private val playing = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    var gain = 1.0
        set(value) {
            field = value
            EXTThreadLocalContext.alcSetThreadContext(AudioSystem.context)
            audioRenderer?.gain(value)
        }
    var pitch = 1.0
        set(value) {
            field = value
            EXTThreadLocalContext.alcSetThreadContext(AudioSystem.context)
            audioRenderer?.pitch(value)
        }


    val finished = Event<VorbisTrack>("finished")

    fun play(loop: Boolean = false) {
        if (!playing.get()) {
            stopRequested.set(false)
            if (audioThread == null) {
                audioThread = thread(isDaemon = true) {
                    audioRenderer = AudioRenderer(this)
                    val progressUpdater = audioRenderer!!.progressUpdater
                    if (!audioRenderer!!.play()) {
                        error("krak")
                    }
                    while (!stopRequested.get()) {
                        audioRenderer!!.update(loop)
                        Thread.sleep(5)
                        progressUpdater.updateProgress()
                    }
                    playing.set(false)
                    audioRenderer?.destroy()
                    finished.trigger(this)
                }
            }
        }
    }

    fun stop() {
        if (playing.get()) {

            stopRequested.set(true)
        }
    }


    fun ioResourceToByteBuffer(filePath: String, blockSize: Int): ByteBuffer {
        val path = Paths.get(filePath)
        if (Files.isReadable(path)) {
            Files.newByteChannel(path).use { fc ->
                val buffer = ByteBuffer.allocateDirect(fc.size().toInt() + 1)
                buffer.order(ByteOrder.nativeOrder())
                while (fc.read(buffer) != -1) {
                }
                buffer.flip()
                return buffer
            }

        }
        error("could not load $filePath")
    }

    init {
        encodedAudio = ioResourceToByteBuffer(filePath, 256 * 1024)
        MemoryStack.stackPush().use { stack ->
            val error: IntBuffer = stack.mallocInt(1)
            handle = STBVorbis.stb_vorbis_open_memory(encodedAudio, error, null)
            if (handle == org.lwjgl.system.MemoryUtil.NULL) {
                throw RuntimeException("Failed to open Ogg Vorbis file. Error: " + error[0])
            }
            val info: STBVorbisInfo = STBVorbisInfo.malloc(stack)
            print(info)
            channels = info.channels()
            sampleRate = info.sample_rate()
        }
        samplesLength = STBVorbis.stb_vorbis_stream_length_in_samples(handle)
        samplesSec = STBVorbis.stb_vorbis_stream_length_in_seconds(handle)
        this.sampleIndex = AtomicInteger(0)
        sampleIndex.set(0)
    }

    fun destroy() {
        STBVorbis.stb_vorbis_close(handle)
    }

    fun progressBy(samples: Int) {
        sampleIndex.set(sampleIndex.get() + samples)
    }

    fun setSampleIndex(sampleIndex: Int) {
        this.sampleIndex.set(sampleIndex)
    }

    fun rewind() {
        seek(0)
    }

    fun skip(direction: Int) {
        seek(
            Math.min(
                Math.max(0, STBVorbis.stb_vorbis_get_sample_offset(handle) + direction * sampleRate),
                samplesLength
            )
        )
    }

    fun skipTo(offset0to1: Float) {
        seek(Math.round(samplesLength * offset0to1))
    }

    fun relativePosition(): Double {
        return sampleIndex.get().toDouble() / samplesLength
    }

    // called from audio thread
    @Synchronized
    fun getSamples(pcm: ShortBuffer?): Int {
        return STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, pcm)
    }

    // called from UI thread
    @Synchronized
    private fun seek(sampleIndex: Int) {
        STBVorbis.stb_vorbis_seek(handle, sampleIndex)
        setSampleIndex(sampleIndex)
    }

    private fun print(info: STBVorbisInfo) {
        println("stream length, samples: " + STBVorbis.stb_vorbis_stream_length_in_samples(handle))
        println("stream length, seconds: " + STBVorbis.stb_vorbis_stream_length_in_seconds(handle))
        println()
        STBVorbis.stb_vorbis_get_info(handle, info)
        println("channels = " + info.channels())
        println("sampleRate = " + info.sample_rate())
        println("maxFrameSize = " + info.max_frame_size())
        println("setupMemoryRequired = " + info.setup_memory_required())
        println("setupTempMemoryRequired() = " + info.setup_temp_memory_required())
        println("tempMemoryRequired = " + info.temp_memory_required())
    }


}