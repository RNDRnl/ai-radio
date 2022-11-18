import bass.initBass
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.launch
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {

    initBass()
    configure {
        width = 768
        height = 576
    }

    program {
        val image = loadImage("data/images/pm5544.png")
        val font = loadFont("data/fonts/default.otf", 64.0)

        fun sequence() = sequence {
            while(true) {
                val musics = File("offline-data/music").listFiles().filter{
                    it.extension == "mp3"
                }.shuffled()

                val speeches = File("offline-data/speaker-02/loudnorm").listFiles().filter {
                    it.extension == "wav"

                }.shuffled()

                for ((index, music) in musics.withIndex()) {
                    yield(Pair(speeches[index].absolutePath,"speech"))
                    yield(Pair(music.absolutePath, "music"))
                }

            }
        }


        var sequenceIter = sequence().iterator()

        var activeStreams = mutableSetOf<AudioStream>()
        fun queueNext(startVolume: Double=1.0): AudioStream {
            val (path, type) = sequenceIter.next()
            val activeStream = AudioStream(path, type)

            activeStream.play(1.0, 0, context = this@program.dispatcher)
            activeStreams.add(activeStream)
//            activeStream.cueOut.listen {
//                it.fadeOut()
//                queueNext()
//            }

            activeStream.finished.listen {
                activeStreams.remove(activeStream)
                queueNext()
                activeStream.destroy()
            }
            return activeStream
        }

        keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR) {
                activeStreams.forEach {
                    it.skip()
                }
            }
        }



        queueNext()

        extend {
            activeStreams.map { it }.forEach {
                it.update()
            }



        }
    }
}
