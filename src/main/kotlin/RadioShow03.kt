import bass.initBass
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.launch
import org.openrndr.shape.LineSegment
import java.io.File

fun main() {
    application {
        configure {
            title = "AI Radio"
        }
        initBass()

        program {
            val settings = object {
                @DoubleParameter("min song length", 5.0, 120.0)
                var songLengthMin = 15.0

                @DoubleParameter("max song length", 5.0, 120.0)
                var songLengthMax = 120.0

                @DoubleParameter("mode", 0.0, 1.0)
                var mode = 1.0

                @DoubleParameter("speaker 1 (Virne)", 0.0, 1.0)
                var speaker1: Double = 1.0

                @DoubleParameter("speaker 2 (Truck)", 0.0, 1.0)
                var speaker2: Double = 1.0

                @DoubleParameter("speaker 3 (Emma)", 0.0, 1.0)
                var speaker3: Double = 1.0

                @DoubleParameter("speaker 4 (Angie)", 0.0, 1.0)
                var speaker4: Double = 1.0


                @IntParameter("min speech segments", 1, 3)
                var minSpeechSegments = 1

                @IntParameter("max speech segments", 1, 7)
                var maxSpeechSegments = 3
            }

            val segmentFinished = Event<Unit>()

            class VolumeRamp(var volume: Double) : Animatable() {
                fun fadeIn(duration: Int = 2000) {
                    animate(::volume, 1.0, duration.toLong())
                }

                fun fadeOut(duration: Int = 2000) {
                    animate(::volume, 0.0, duration.toLong())
                }
            }

            val musicSequence = sequence {
                while (true) {
                    val musics = File("offline-data/music").listFiles().filter {
                        it.extension == "mp3"
                    }.shuffled()
                    for (music in musics) {
                        yield(AudioStream(music.absolutePath, "music"))
                    }
                }
            }.iterator()

            val speakerSources =
                listOf(
                    File("offline-data/voices/virne"),
                    File("offline-data/voices/truck"),
                    File("offline-data/voices/angie"),
                    File("offline-data/voices/emma"),
                    )
            val speakerSequences = (0 until 4).map { speakerId ->
                sequence {
                    while (true) {
                        val musics = speakerSources[speakerId].listFiles().filter {
                            it.extension == "wav"
                        }.shuffled()
                        require(musics.isNotEmpty())
                        for (music in musics) {
                            println("yield: ${music.absolutePath}")
                            yield(AudioStream(music.absolutePath, "speech"))
                        }
                    }
                }.iterator()
            }


            val activeStreams = mutableSetOf<AudioStream>()

            fun sampleSpeakers(count: Int): List<Int> {
                val totalWeight = settings.speaker1 + settings.speaker2 + settings.speaker3 + settings.speaker4

                val weights = mutableListOf(
                    Pair(0, settings.speaker1 / totalWeight),
                    Pair(1, settings.speaker2 / totalWeight),
                    Pair(2, settings.speaker3 / totalWeight),
                    Pair(3, settings.speaker4 / totalWeight)
                ).sortedBy { it.second }


                val result = mutableListOf<Int>()
                for (i in 0 until count) {
                    val r = Double.uniform(0.0, 1.0)
                    var sum = 0.0
                    for (w in weights) {

                        sum += w.second
                        println("${sum} ${r}")
                        if (sum > r) {
                            println("found sample ${w.first}")
                            result.add(w.first)
                            break
                        }
                    }
                }
                return result
            }

            launch {
                while (true) {
                    for (stream in activeStreams.map { it }) {
                        stream.update()
                    }
                    yield()
                }
            }

            fun AudioStream.play(volume: Double, predelay: Int, maximumDuration: Double = 120.0) {
                this.play(volume, predelay, maximumDuration, dispatcher)
                println("active streams: $activeStreams")
                activeStreams.add(this)
                this.finished.listen {
                    println("finished")
                    println("active streams: $activeStreams")
                    activeStreams.remove(this)
                }
            }

            fun AudioStream.fadeIn() {
                launch {
                    println("fading in!")
                    val vr = VolumeRamp(this@fadeIn.volume)
                    vr.fadeIn()
                    vr.updateAnimation()
                    while (vr.hasAnimations()) {
                        vr.updateAnimation()
                        this@fadeIn.channel!!.setVolume(vr.volume)
                        yield()
                    }
                }
            }


            fun AudioStream.fadeOut() {
                launch {
                    println("fading out!")
                    val vr = VolumeRamp(1.0)
                    vr.fadeOut()
                    vr.updateAnimation()
                    while (vr.hasAnimations()) {
                        vr.updateAnimation()
                        this@fadeOut.channel!!.setVolume(vr.volume)
                        yield()
                    }
                    this@fadeOut.finished.trigger(this@fadeOut)
                    this@fadeOut.destroy()
                }
            }

            segmentFinished.listen {
                fun talkWithBgMusic() {
                    val music = musicSequence.next()
                    music.finished.listen { segmentFinished.trigger(Unit) }
                    println("playing music")
                    music.play(0.15, 0, 1000000.0)
                    music.cueOut.listen {
                        if (it.early) {
                            music.fadeOut()
                        }
                    }

                    println("constructing speech")


                    val segments = Int.uniform(
                        settings.minSpeechSegments.coerceAtMost(settings.maxSpeechSegments),
                        settings.maxSpeechSegments.coerceAtLeast(settings.minSpeechSegments) + 1
                    )
                    val speakers = sampleSpeakers(segments)

                    val speech = speakers.map { speakerSequences[it].next() }

                    for ((s, n) in speech.zipWithNext()) {
                        s.finished.listen {
                            println("$s finished, playing $n")
                            n.play(1.0, 50)
                            s.destroy()
                        }
                    }
                    speech.last().cueOut.listen {
                        music.fadeIn()
                        launch {
                            println("fading out in 10s")
                            val delay = Double.uniform(settings.songLengthMin, settings.songLengthMax) * 1000.0
                            delay(delay.toLong())
                            music.fadeOut()
                        }
                    }
                    speech.first().play(1.0, 0)
                }


                fun talkThenMusic() {
                    val music = musicSequence.next()
                    music.finished.listen { segmentFinished.trigger(Unit) }

                    val speech = listOf(speakerSequences[0].next(), speakerSequences[1].next())

                    println(speech.size)
                    for ((s, n) in speech.zipWithNext()) {
                        s.finished.listen {
                            println("$s finished, playing $n")
                            n.play(1.0, 250)
                        }
                    }
                    speech.last().finished.listen {
                        music.play(1.0, 0, Double.uniform(settings.songLengthMin, settings.songLengthMax))
                        music.cueOut.listen {
                            if (it.early) {
                                music.fadeOut()
                            }
                        }

                    }
                    speech.first().play(1.0, 0)
                }

                if (Math.random() <= settings.mode) {
                    talkWithBgMusic()
                } else {
                    talkThenMusic()
                }


            }
            segmentFinished.trigger(Unit)


            val gui = GUI()

            gui.add(settings, "Settings")

            extend(gui)

            extend {
                drawer.translate(250.0, 0.0)
                activeStreams.map { it }.forEach {
                    it.update()
                    drawer.rectangle(0.0, 104.0, (256.0 * it.position() / it.duration).coerceAtLeast(0.0), 4.0)
                    drawer.text(it.tag, 0.0, 125.0)

                    val fft = it.fft()
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.lineSegments(fft.mapIndexed { index, it ->
                        LineSegment(
                            index * 2.0,
                            100.0,
                            index * 2.0,
                            100.0 - it * 100.0
                        )
                    })

                    drawer.translate(0.0, 150.0)
                }

            }
        }
    }
}