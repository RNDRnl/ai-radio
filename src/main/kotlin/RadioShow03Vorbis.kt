import audio.VorbisTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.openrndr.animatable.Animatable
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.events.Event
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
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

        program {
            val settings = object {
                @DoubleParameter("min preset length", 60.0, 3600.0)
                var presetLengthMin = 600.0

                @DoubleParameter("max preset length", 60.0, 3600.0)
                var presetLengthMax = 900.0


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

                @DoubleParameter("speaker 5 (Constant)", 0.0, 1.0)
                var speaker5: Double = 1.0

                @DoubleParameter("speaker 6 (Mamma)", 0.0, 1.0)
                var speaker6: Double = 1.0

                @DoubleParameter("speaker 7 (Freud)", 0.0, 1.0)
                var speaker7: Double = 1.0

                @DoubleParameter("speaker 8 (Einstein)", 0.0, 1.0)
                var speaker8: Double = 1.0

                @DoubleParameter("speaker 9 (C. Manson)", 0.0, 1.0)
                var speaker9: Double = 1.0

                @IntParameter("min speech segments", 1, 3)
                var minSpeechSegments = 1

                @IntParameter("max speech segments", 1, 7)
                var maxSpeechSegments = 3

                fun speakers() = listOf(
                    speaker1, speaker2, speaker3, speaker4, speaker5, speaker6, speaker7, speaker8, speaker9
                )


            }

            val gui = GUI()

            gui.add(settings, "Settings")
            extend(gui)



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
                        it.extension == "ogg"
                    }.shuffled()
                    for (music in musics) {
                        yield(VorbisTrack(music.absolutePath, "music"))
                    }
                }
            }.iterator()

            val speakerSources =
                listOf(
                    File("offline-data/voices/virne"),
                    File("offline-data/voices/truck"),
                    File("offline-data/voices/emma"),
                    File("offline-data/voices/angie"),
                    File("offline-data/voices/constant"),
                    File("offline-data/voices/mamma"),
                    File("offline-data/voices/freud"),
                    File("offline-data/voices/einstein"),
                    File("offline-data/voices/cmanson"),
                )
            val speakerSequences = (speakerSources.indices).map { speakerId ->
                sequence {
                    while (true) {
                        val musics = speakerSources[speakerId].listFiles().filter {
                            it.extension == "wav"
                        }.shuffled()
                        require(musics.isNotEmpty())
                        for (music in musics) {
                            println("yield: ${music.absolutePath}")
                            yield(VorbisTrack(music.absolutePath, "speech"))
                        }
                    }
                }.iterator()
            }

            val activeStreams = mutableSetOf<AudioStream>()

            fun sampleSpeakers(count: Int): List<Int> {
                val sliders = settings.speakers()
                val totalWeight = sliders.sum()
                val weights = sliders.mapIndexed { index, it -> Pair(index, it / totalWeight) }
                    .sortedBy { it.second }

                val result = mutableListOf<Int>()
                for (i in 0 until count) {
                    val r = Double.uniform(0.0, 1.0)
                    var sum = 0.0
                    for (w in weights) {
                        sum += w.second
                        if (sum > r) {
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
                activeStreams.add(this)
                this.finished.listen {
                    activeStreams.remove(this)
                }
            }

            fun VorbisTrack.fadeIn() {
                launch {
                    val vr = VolumeRamp(this@fadeIn.gain)
                    vr.fadeIn()
                    vr.updateAnimation()
                    while (vr.hasAnimations()) {
                        vr.updateAnimation()
                        this@fadeIn.gain = vr.volume
                        yield()
                    }
                }
            }


            fun VorbisTrack.fadeOut() {
                launch {
                    println("fading out!")
                    val vr = VolumeRamp(1.0)
                    vr.fadeOut()
                    vr.updateAnimation()
                    while (vr.hasAnimations()) {
                        vr.updateAnimation()
                        this@fadeOut.gain = vr.volume
                        yield()
                    }
                    this@fadeOut.stop()
                    this@fadeOut.finished.trigger(this@fadeOut)
                    this@fadeOut.destroy()
                }
            }

            var lastPresetChange = 0.0
            var presetLength = Double.uniform(settings.presetLengthMin, settings.presetLengthMax)
            var presetName = "startup"

            val presetSequence = sequence {
                while (true) {
                    val presetDir = File("data/presets")
                    val presetFiles = presetDir.listFiles().filter { it.extension == "json" }.shuffled()
                    for (p in presetFiles) {
                        if (p.exists()) {
                            yield(p)
                        }
                    }
                }
            }
            val presetIterator = presetSequence.iterator()

            fun nextPreset() {
                lastPresetChange = seconds
                val nextPreset = presetIterator.next()
                presetName = nextPreset.nameWithoutExtension
                gui.loadParameters(nextPreset)
                presetLength = Double.uniform(settings.presetLengthMin, settings.presetLengthMax)
            }

            keyboard.character.listen {
                if (!it.propagationCancelled) {
                    if (it.character == 'n') {
                        nextPreset()
                    }
                }
            }

            segmentFinished.listen {
                if (seconds - lastPresetChange > presetLength) {
                    nextPreset()
                }

                fun talkWithBgMusic() {
                    val music = musicSequence.next()
                    music.finished.listen { segmentFinished.trigger(Unit) }
                    println("playing music")
                    music.play(0.15)


                    println("constructing speech")


                    val segments = Int.uniform(
                        settings.minSpeechSegments.coerceAtMost(settings.maxSpeechSegments),
                        settings.maxSpeechSegments.coerceAtLeast(settings.minSpeechSegments) + 1
                    )
                    val speakers = sampleSpeakers(segments)

                    val speech = speakers.map { speakerSequences[it].next() }

                    for ((s, n) in speech.zipWithNext()) {
                        s.finished.listen {
                            launch {
                                delay(Int.uniform(150, 250).toLong())
                                n.play(1.0)
                                s.destroy()
                            }
                        }
                    }
                    speech.last().cue(-1.0).listen {
                        music.fadeIn()
                        launch {
                            val delay = Double.uniform(settings.songLengthMin, settings.songLengthMax) * 1000.0
                            delay(delay.toLong())
                            music.fadeOut()
                        }
                    }
                    speech.first().play(1.0)
                }

                    talkWithBgMusic()

            }
            segmentFinished.trigger(Unit)

            extend {
                drawer.translate(250.0, 0.0)
                val presetProgress = (seconds - lastPresetChange) / presetLength
                drawer.translate(0.0, 20.0)
                drawer.text("active program: ${presetName}", 0.0, 0.0)
                drawer.rectangle(0.0, 10.0, (256.0 * presetProgress), 4.0)


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