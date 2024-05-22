package sapala.s2sauthservice.buildmanagerservice.build

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipUtil {
    companion object {
        fun extractZip(zip: ZipInputStream, extractFolder: File) {
            var zipEntry: ZipEntry?
            while ((zip.nextEntry.also { zipEntry = it }) != null) {
                val entry = zipEntry!!
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }

                val targetFile = extractFolder.resolve(entry.name)
                targetFile.getParentFile().mkdirs()
                FileOutputStream(targetFile).use { fileOutputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while ((zip.read(buffer).also { length = it }) > 0) {
                        fileOutputStream.write(buffer, 0, length)
                    }
                }
                zip.closeEntry()
            }
        }

        fun zipDirectory(sourceDir: File, destinationZip: File, additionalFiles: List<File> = listOf()): File {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
                addToZip(zos, sourceDir)
                addToZip(zos, additionalFiles)
            }
            return destinationZip
        }

        fun zipFiles(files: List<File>, destinationZip: File): File {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
                addToZip(zos, files)
            }
            return destinationZip
        }

        private fun addToZip(zos: ZipOutputStream, sourceDir: File) {
            sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val zipEntry = ZipEntry(sourceDir.toURI().relativize(file.toURI()).path)
                zos.putNextEntry(zipEntry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        private fun addToZip(zos: ZipOutputStream, files: List<File>) {
            files.forEach { file ->
                val zipEntry = ZipEntry(file.name)
                zos.putNextEntry(zipEntry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
