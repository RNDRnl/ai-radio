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

    configure {
        width = 768
        height = 576
    }

    program {

        File("/Users/edwin/bla.log").writeText(File(".").absolutePath)
        extend {
            drawer.clear(ColorRGBa.PINK)
        }
    }
}
