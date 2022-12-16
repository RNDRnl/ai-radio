package audio

import org.lwjgl.openal.*
import org.lwjgl.openal.AL10.AL_GAIN
import org.lwjgl.openal.AL10.AL_PITCH
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer


object AudioSystem {
    val device: Long = ALC10.alcOpenDevice(null as ByteBuffer?)
    val context: Long = ALC10.alcCreateContext(device, null as IntBuffer?)

    fun destroy() {
        ALC10.alcDestroyContext(context)
        ALC10.alcCloseDevice(device)
    }
}

class AudioRenderer internal constructor(private val track: VorbisTrack) {
    private var format: Int = 0

    private val source: Int
    private val buffers: IntBuffer
    private val pcm: ShortBuffer
    var bufferOffset: Long = 0 // offset of last processed buffer
    var offset: Long = 0 // bufferOffset + offset of current buffer
    var lastOffset: Long = 0 // last offset update

    init {
        when (track.channels) {
            1 -> format = AL10.AL_FORMAT_MONO16
            2 -> format = AL10.AL_FORMAT_STEREO16
            else -> throw UnsupportedOperationException("Unsupported number of channels: " + track.channels)
        }
//        device = ALC10.alcOpenDevice(null as ByteBuffer?)
//        if (device == MemoryUtil.NULL) {
//            throw IllegalStateException("Failed to open the default device.")
//        }
//        context = ALC10.alcCreateContext(device, null as IntBuffer?)
//        if (context == MemoryUtil.NULL) {
//            throw IllegalStateException("Failed to create an OpenAL context.")
//        }


        pcm = MemoryUtil.memAllocShort(BUFFER_SIZE)
        EXTThreadLocalContext.alcSetThreadContext(AudioSystem.context)
        val deviceCaps: ALCCapabilities = ALC.createCapabilities(AudioSystem.device)
        AL.createCapabilities(deviceCaps)
        source = AL10.alGenSources()
        AL10.alSourcei(source, SOFTDirectChannels.AL_DIRECT_CHANNELS_SOFT, AL10.AL_TRUE)
        buffers = MemoryUtil.memAllocInt(2)
        AL10.alGenBuffers(buffers)
    }

    fun destroy() {
        AL10.alDeleteBuffers(buffers)
        AL10.alDeleteSources(source)
        MemoryUtil.memFree(buffers)
        MemoryUtil.memFree(pcm)
        EXTThreadLocalContext.alcSetThreadContext(MemoryUtil.NULL)

    }

    private fun stream(buffer: Int): Int {
        var samples: Int = 0
        while (samples < BUFFER_SIZE) {
            pcm.position(samples)
            val samplesPerChannel: Int = track.getSamples(pcm)
            if (samplesPerChannel == 0) {
                break
            }
            samples += samplesPerChannel * track.channels
        }
        if (samples != 0) {
            pcm.position(0)
            pcm.limit(samples)
            AL10.alBufferData(buffer, format, pcm, track.sampleRate)
            pcm.limit(BUFFER_SIZE)
        }
        return samples
    }

    fun play(): Boolean {
        for (i in 0 until buffers.limit()) {
            if (stream(buffers.get(i)) == 0) {
                return false
            }
        }
        AL10.alSourceQueueBuffers(source, buffers)
        AL10.alSourcePlay(source)
        return true
    }

    fun pause() {
        AL10.alSourcePause(source)
    }

    fun stop() {
        AL10.alSourceStop(source)
    }

    fun gain(gain:Double) {
        AL10.alSourcef(source, AL_GAIN, gain.toFloat())
    }

    fun pitch(pitch:Double) {
        AL10.alSourcef(source, AL_PITCH, pitch.toFloat())
    }

    fun update(loop: Boolean): Boolean {
        val processed: Int = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED)
        for (i in 0 until processed) {
            bufferOffset += (BUFFER_SIZE / track.channels).toLong()
            val buffer: Int = AL10.alSourceUnqueueBuffers(source)
            if (stream(buffer) == 0) {
                var shouldExit: Boolean = true
                if (loop) {
                    track.rewind()
                    bufferOffset = 0
                    offset = bufferOffset
                    lastOffset = offset
                    shouldExit = stream(buffer) == 0
                }
                if (shouldExit) {
                    return false
                }
            }
            AL10.alSourceQueueBuffers(source, buffer)
        }
        if (processed == 2) {
            AL10.alSourcePlay(source)
        }
        return true
    }

    val progressUpdater: ProgressUpdater
        get() = object : ProgressUpdater {
            override fun makeCurrent(current: Boolean) {
                EXTThreadLocalContext.alcSetThreadContext(if (current) AudioSystem.context else MemoryUtil.NULL)
            }

            override fun updateProgress() {
                offset = bufferOffset + AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET)
                track.progressBy((offset - lastOffset).toInt())
                lastOffset = offset
            }
        }

    companion object {
        private val BUFFER_SIZE: Int = 1024 * 8
    }
}