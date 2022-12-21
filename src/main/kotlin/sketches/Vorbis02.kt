package sketches


import audio.VorbisTrack
import kotlinx.coroutines.yield
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.launch

fun main() =
    application {
        program {
            val track = VorbisTrack("offline-data/ikonz.ogg")
            track.cue(5.0).listen {
                println("omg halfway")
                launch {
                    class VolumeRamp(var volume:Double):Animatable() {

                    }
                    val vr = VolumeRamp(1.0)
                    vr.apply {
                        ::volume.animate(0.0, 1000)
                    }


                    while(vr.volume > 0.0) {
                        vr.updateAnimation()
                        it.gain = vr.volume
                        yield()
                    }
                    it.stop()
                    it.destroy()
                }
            }
            track.play(loop = false)


        }
    }
