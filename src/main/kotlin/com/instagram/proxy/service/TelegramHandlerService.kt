package com.instagram.proxy.service

import com.instagram.proxy.domain.model.TelegramMessage
import com.instagram.proxy.integration.dto.DownloadRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.io.File

@Service
class TelegramHandlerService(
    // val instagramContentClient: InstagramContentClient,
    val instagramService: InstagramService,
    @Value("\${minio.bucket-name}") val bucketName: String,
    @Value("\${local.download:true}") val localDownload: Boolean,
) {
    fun handleIncomingMessage(telegramMessage: TelegramMessage): List<File> {
        val watch = StopWatch()
        watch.start()

        var files: List<File> = emptyList()
        files = if (telegramMessage.text.contains("http")) {
            instagramService.downloadContent(DownloadRequest(telegramMessage.text))
        } else {
            instagramService.fetchUserStories(telegramMessage.text)
        }
        return files

        //         val res = instagramContentClient.downloadInstagramContent(DownloadRequest(telegramMessage.text))
        //         watch.stop()
        //
        //         println("Execution time: ${watch.totalTimeMillis} ms")
        //
        //         val result = minioClient.listObjects(
        //             ListObjectsArgs.builder()
        //                 .bucket(bucketName)
        //                 .recursive(false)
        //                 .build()
        //         )
        //
        //         val firstFolder = result.filter { it.get().objectName().endsWith("/") }
        //             .firstOrNull { it.get().objectName() == res.shortcode + "/" }?.get()?.objectName()
        //
        //         if (firstFolder == null) {
        //             println("No folders found in the bucket.")
        //             return emptyList()
        //         }
        //
        //         val folderObjects = minioClient.listObjects(
        //             ListObjectsArgs.builder()
        //                 .bucket(bucketName)
        //                 .prefix(firstFolder)
        //                 .recursive(true)
        //                 .build()
        //         ).toList()
        //
        //         val mp4Files = folderObjects.filter { it.get().objectName().endsWith(".mp4") }.map { it.get() }
        //         val txtFiles = folderObjects.filter { it.get().objectName().endsWith(".txt") }.map { it.get() }
        //         val jpgFiles = folderObjects.filter { it.get().objectName().endsWith(".jpg") }.map { it.get() }
        //             .sortedBy { it.objectName() }
        //
        //         val downloadDir = "downloads"
        //         val downloadedFiles = mutableListOf<File>()
        //
        //         if (mp4Files.isNotEmpty()) {
        //             val mp4File = mp4Files.first()
        //             downloadedFiles.add(downloadFile(mp4File.objectName(), downloadDir))
        //
        //             val txtFile = txtFiles.firstOrNull()
        //             if (txtFile != null) {
        //                 downloadedFiles.add(downloadFile(txtFile.objectName(), downloadDir))
        //             } else {
        //                 println("No .txt file found.")
        //             }
        //         } else {
        //             if (jpgFiles.isNotEmpty()) {
        //                 println("Downloading .jpg files:")
        //                 jpgFiles.forEach { jpg ->
        //                     downloadedFiles.add(downloadFile(jpg.objectName(), downloadDir))
        //                 }
        //             } else {
        //                 println("No .jpg files found.")
        //             }
        //
        //             val txtFile = txtFiles.firstOrNull()
        //             if (txtFile != null) {
        //                 downloadedFiles.add(downloadFile(txtFile.objectName(), downloadDir))
        //             } else {
        //                 println("No .txt file found.")
        //             }
        //         }
        //
        //         return downloadedFiles
        //     }
        // }
        //
        // private fun downloadFile(objectName: String, downloadDir: String): File {
        //     val localFilePath = Paths.get(downloadDir, objectName).toString()
        //     Files.createDirectories(Paths.get(localFilePath).parent)
        //
        //     minioClient.getObject(
        //         GetObjectArgs.builder()
        //             .bucket(bucketName)
        //             .`object`(objectName)
        //             .build()
        //     ).use { inputStream ->
        //         File(localFilePath).outputStream().use { outputStream ->
        //             inputStream.copyTo(outputStream)
        //         }
        //     }
        //
        //     // println("Downloaded: $objectName to $localFilePath")
        //     return File(localFilePath)
        // }
    }
}
