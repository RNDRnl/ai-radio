package sketches


import audio.VorbisTrack
import org.openrndr.application
import org.openrndr.color.ColorRGBa

fun main() =
    application {
        program {
            val track = VorbisTrack("offline-data/ikonz.ogg")
            track.play(loop = true)

            val track2 = VorbisTrack("offline-data/ikonz.ogg")
            track2.play(loop = true)
            track2.pitch = 2.0

            mouse.dragged.listen {
                track.skipTo((mouse.position.x / width).toFloat())
            }

            extend {
                track2.pitch = 2.0
                drawer.stroke = ColorRGBa.RED
                drawer.lineSegment(
                    track.relativePosition() * width,
                    0.0,
                    track.relativePosition() * width,
                    height * 1.0
                )
            }
        }
    }
