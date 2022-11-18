import bass.initBass
import kotlinx.coroutines.yield
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.launch
import org.openrndr.shape.LineSegment
import java.io.File

fun main() {
    application {
        initBass()

        program {
            val settings = object {
                @DoubleParameter("min song length", 60.0, 600.0)
                var songLengthMin = 60.0

                @DoubleParameter("max song length", 60.0, 600.0)
                var songLengthMax = 120.0

                @DoubleParameter("mode", 0.0, 1.0)
                var mode = 1.0
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
                listOf(File("offline-data/speaker-02/loudnorm"), File("offline-data/speaker-03/loudnorm"))
            val speakerSequences = (0 until 2).map { speakerId ->
                sequence {
                    while (true) {
                        val musics = speakerSources[speakerId].listFiles().filter {
                            it.extension == "mp3"
                        }.shuffled().sortedBy { it.length() }.drop(50).take(130).shuffled()
                        for (music in musics) {
                            println("yield: ${music.absolutePath}")
                            yield(AudioStream(music.absolutePath, "speech"))
                        }
                    }
                }.iterator()
            }


            val activeStreams = mutableSetOf<AudioStream>()

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
                }
            }

            segmentFinished.listen {
                fun talkWithBgMusic() {
                    val music = musicSequence.next()
                    music.finished.listen { segmentFinished.trigger(Unit) }
                    println("playing music")
                    music.play(0.15, 0, Double.uniform(settings.songLengthMin, settings.songLengthMax))
                    music.cueOut.listen {
                        if (it.early) {
                            music.fadeOut()
                        }
                    }

                    println("constructing speech")
                    val speech = listOf(speakerSequences[0].next(), speakerSequences[1].next())
                    println(speech)

                    println(speech.size)
                    for ((s, n) in speech.zipWithNext()) {
                        println("cueing stuff with $s $n")
                        s.finished.listen {
                            println("$s finished, playing $n")
                            n.play(1.0, 250)
                        }
                    }
                    speech.last().cueOut.listen {
                        music.fadeIn()
                    }
                    speech.first().play(1.0, 0)
                }


                fun talkThenMusic() {
                    val music = musicSequence.next()
                    music.finished.listen { segmentFinished.trigger(Unit) }

                    println("constructing speech")
                    val speech = listOf(speakerSequences[0].next(), speakerSequences[1].next())
                    println(speech)

                    println(speech.size)
                    for ((s, n) in speech.zipWithNext()) {
                        println("cueing stuff with $s $n")
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
                    drawer.lineSegments(fft.mapIndexed { index, it -> LineSegment(index*2.0, 100.0, index*2.0, 100.0-it*100.0) })

                    drawer.translate(0.0, 150.0)
                }

            }
        }
    }
}