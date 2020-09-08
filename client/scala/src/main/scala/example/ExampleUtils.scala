package example

import scala.util.{Try, Success, Failure}

import java.io.File
import sys.process._
import java.net.URL

object ExampleUtils {
    def deleteDirectory(dir: File): Unit = {
      Option(dir.listFiles()).map(_.foreach(deleteDirectory))
      dir.delete()
    }

    // https://stackoverflow.com/questions/24162478/how-to-download-and-save-a-file-from-the-internet-using-scala
    def fileDownloader(url: String, filename: String) = {
        val destination = new File(filename)
        destination.getParentFile().mkdirs()
        destination.createNewFile()

        new URL(url) #> destination !!
    }
}
