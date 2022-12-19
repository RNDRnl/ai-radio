import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.system.JNI.*
import org.lwjgl.system.Pointer
import org.lwjgl.system.macosx.CoreFoundation.*

import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*

import org.lwjgl.system.macosx.ObjCRuntime.*
import org.openrndr.application
import org.openrndr.draw.loadImage
import java.io.File
import java.nio.ByteBuffer


// https://developer.apple.com/library/archive/documentation/CoreFoundation/Conceptual/CFBundles/AccessingaBundlesContents/AccessingaBundlesContents.html


fun CFString(text:String): Long {
        val textBA = text.encodeToByteArray()
        val textBB = ByteBuffer.allocateDirect(textBA.size+1)
        textBB.rewind()
        textBB.put(textBA)
        textBB.put(0)
        textBB.rewind()

        val string = CFStringCreateWithCString(0L, textBB, kCFStringEncodingUTF8)
        return string
}



fun main() = application {
        program {
                val image = loadImage("data/images/cheeta.jpg")
                extend {
                drawer.image(image)
        }
}

//        val dirBuffer = memASCII("/")
//        val dirLoc = memAddress(dirBuffer)
//
//
//
//        val chdir = getLibrary().getFunctionAddress("chdir")
//        println(chdir)
//
//        val getcwd = getLibrary().getFunctionAddress("getcwd")
//        println(getcwd)
//
//
//        //invokePP(dirLoc, chdir)
//        println("file path: ${File(".").absolutePath}")
//
//        val p = memAddress(MemoryUtil.memAlloc(256))
//        invokePPP(p,256, getcwd)
//        println(memASCII(p))
//
//
//        File("/Users/edwin/path.txt").writeText(File(".").absolutePath)
//
//        val objc_msgSend = getLibrary().getFunctionAddress("objc_msgSend")
//        println(objc_msgSend)
//        File("/Users/edwin/objc-msgsend.txt").writeText("$objc_msgSend")
//
//
//        val bundleClass = objc_getClass("NSBundle")
//        println(bundleClass)
//        File("/Users/edwin/bundle-class.txt").writeText("$bundleClass")
//
//
////        val mainBundleMethod = class_getClassMethod(bundleClass, CFString("mainBundle"))
//
//        val mainBundleMethod = sel_getUid("mainBundle")
//        println(mainBundleMethod)
//
//
//        File("/Users/edwin/bundle-mainBundleMethod.txt").writeText("$mainBundleMethod")
//        val mainBundle = invokePPP(bundleClass, sel_getUid("mainBundle"), objc_msgSend)
//
//
//
//        File("/Users/edwin/bundle-mainBundle.txt").writeText("$mainBundle")
//
//        val mainBundlePath = invokePPP(mainBundle, sel_getUid("bundlePath"), objc_msgSend)
//        println("mainBundlePath: ${mainBundlePath}")
//
//
//        val mainBundlePathUtf8 = invokePPP(mainBundlePath, sel_getUid("UTF8String"), objc_msgSend)
//        println("mainBundlePathUtf8: ${mainBundlePathUtf8}")
//        val finalPath = memUTF8(mainBundlePathUtf8)
//        println(memUTF8(mainBundlePathUtf8))
//
//        File("/Users/edwin/bundle-mainBundle-path.txt").writeText("$finalPath")
//

//        val text = "TemplateProgramKt"
//        val textBA = text.encodeToByteArray()
//        val textBB = ByteBuffer.allocateDirect(textBA.size+1)
//        textBB.rewind()
//        textBB.put(textBA)
//        textBB.put(0)
//        textBB.rewind()
//
//        val string = CFStringCreateWithCString(0L, textBB, kCFStringEncodingUTF8)
//        println(string)
//        val bundle = CFBundleGetBundleWithIdentifier(string)
//
//
//        val class_ = object_getClass(bundle)
//        File("/Users/edwin/bundle-class.txt").writeText("$class_")
//
//        if (bundle != 0L) {
//                val text = "bundlePath"
//                val textBA = text.encodeToByteArray()
//                val textBB = ByteBuffer.allocateDirect(textBA.size+1)
//                textBB.rewind()
//                textBB.put(textBA)
//                textBB.put(0)
//                textBB.rewind()
//                val string = CFStringCreateWithCString(0L, textBB, kCFStringEncodingUTF8)
//
//                val bundlePathMethod = class_getInstanceMethod(class_, string)
//
//
//
//
//                File("/Users/edwin/bundle-method.txt").writeText("$bundlePathMethod")
//                val attr = property_getAttributes(bundlePathMethod)
//
//
//                File("/Users/edwin/bundle-resourcePath-attributes.txt").writeText("$attr")
//        }
//
//
//
//        File("/Users/edwin/bundle.txt").writeText("$bundle")
//
//
//        File("/Users/edwin/bla.log").writeText(File(".").absolutePath)
//        File("/Users/edwin/whereami.txt").writeText(
//        File(".").listFiles().map {
//                it.absolutePath
//        }.joinToString("\n")
//        )
//
//        File("/Users/edwin/envvars.txt").writeText(System.getenv().toList().map { "${it.first}: ${it.second}" }.joinToString("\n"))

}
