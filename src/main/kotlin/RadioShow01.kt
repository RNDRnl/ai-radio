import bass.initBass
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.draw.tint
import org.openrndr.extra.noise.uniform
import org.openrndr.launch
import org.openrndr.shape.LineSegment
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
        fun sequence() = sequence {
            while (true) {
                val musics = File("offline-data/music").listFiles().filter {
                    it.extension == "mp3"
                }.shuffled()

                val speeches0 = File("offline-data/speaker-02/loudnorm").listFiles().filter {
                    it.extension == "mp3"
                }.sortedBy { it.length() }.take(230).shuffled()

                val speeches1 = File("offline-data/speaker-03/loudnorm").listFiles().filter {
                    it.extension == "mp3"
                }.sortedBy { it.length() }.take(230).shuffled()

                val sp0i = sequence {
                    while (true) {
                        for (i in 0 until speeches0.size) yield(speeches0[i])
                    }
                }.iterator()
                val sp1i = sequence {
                    while (true) {
                        for (i in 0 until speeches1.size) yield(speeches1[i])
                    }
                }.iterator()

                for ((index, music) in musics.withIndex()) {
                    val length = Int.uniform(1, 3)

                    if (Math.random() < 0.5) {
                        for (i in 0 until length) {
                            yield(Pair(sp0i.next().absolutePath, "speech1"))
                            yield(Pair(sp1i.next().absolutePath, "speech2"))
                        }
                    } else {
                        for (i in 0 until length) {
                            yield(Pair(sp1i.next().absolutePath, "speech2"))
                            yield(Pair(sp0i.next().absolutePath, "speech1"))
                        }
                    }
                    yield(Pair(music.absolutePath, "music"))
                }
            }
        }

        var sequenceIter = sequence().iterator()

        var activeStreams = mutableSetOf<AudioStream>()
        fun queueNext(startVolume: Double = 1.0): AudioStream {
            val (path, type) = sequenceIter.next()
            val activeStream = AudioStream(path, type)

            val delay = if ("speech" in type) 500 else 0

            activeStream.play(1.0, delay, this@application.program.dispatcher)
            if (type == "speech1") {
                activeStream.channel?.setPan(-0.5)
            } else if (type == "speech2") {
                activeStream.channel?.setPan(0.5)
            }
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
            drawer.translate(100.0, 100.0)
            activeStreams.map { it }.forEach {
                it.update()
                drawer.rectangle(0.0, 104.0, (256.0 * it.position() / it.duration).coerceAtLeast(0.0), 4.0)
                drawer.text(it.tag, 0.0, 125.0)

                val fft = it.fft()
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegments(fft.mapIndexed { index, it -> LineSegment(index*2.0, 100.0, index*2.0, 100.0-it*100.0) })
            }
        }
    }
}
