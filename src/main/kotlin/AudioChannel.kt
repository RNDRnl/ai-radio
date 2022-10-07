import bass.BassChannel
import jouvieje.bass.Bass
import jouvieje.bass.defines.BASS_DATA.BASS_DATA_FFT256
import jouvieje.bass.defines.BASS_SAMPLE
import jouvieje.bass.structures.HSTREAM
import kotlinx.coroutines.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.draw.volumeTexture
import org.openrndr.events.Event
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext

class AudioStream(val path: String, val tag:String) : Animatable() {
    var finished = Event<AudioStream>()
    var cueOut = Event<AudioStream>()

    var stream: HSTREAM? = null
    var channel: BassChannel? = null
    var playing = false
    var duration = 0.0
    var outCued = false

    var volume = 1.0

    fun position(): Double {
        if (channel == null) return 0.0 else
            return channel!!.getPosition()
    }

    fun skip() {
        val duration = channel!!.getDuration()
        channel!!.setPosition(duration - 3.0)
    }

    fun fadeOut() {
        ::volume.cancel()
        ::volume.animate(0.0, 2000, Easing.QuadOut)
    }

    fun fadeIn() {
        ::volume.cancel()
        ::volume.animate(1.0, 2000, Easing.QuadIn)
    }

    fun fft(): DoubleArray {
        if (channel == null) {
            return doubleArrayOf()
        } else {
            val bb = ByteBuffer.allocateDirect(128 * 4)
            bb.order(ByteOrder.nativeOrder())
            bb.rewind()
            Bass.BASS_ChannelGetData(channel!!.channel, bb, BASS_DATA_FFT256)
            bb.rewind()

            val result = DoubleArray(128)
            for (i in 0 until 128) {
                result[i] = bb.getFloat().toDouble()
            }
            return result
        }
    }

    fun play(startVolume: Double = 1.0, predelay: Int = 0, context: CoroutineDispatcher) {
        volume = startVolume
        stream = Bass.BASS_StreamCreateFile(false, path, 0, 0, 0)
        channel = BassChannel(stream!!.asInt())
        channel!!.setVolume(volume)

        if (predelay == 0) {
            channel!!.play()
            playing = true
        }
        duration = channel!!.getDuration()

        if (predelay != 0) {
            var ct = System.currentTimeMillis()
            GlobalScope.launch(context) {
                println("waiting")
                while (System.currentTimeMillis() - ct < predelay) {
                    yield()
                }
                println("play")
                channel!!.play()
                playing = true
            }
        }

    }

    fun update() {
        //updateAnimation()
        if (playing) {
            //channel!!.setVolume(volume)
            val pos = channel!!.getPosition()
            if (pos > duration - 2.0 && !outCued) {
                println("almost finished $path")
                outCued = true
                cueOut.trigger(this)
            }

            if (channel?.isPlaying() == false) {
                println("finished $path")
                playing = false
                finished.trigger(this)
            }
        }
    }

    fun destroy() {
        Bass.BASS_StreamFree(stream!!)

    }
}