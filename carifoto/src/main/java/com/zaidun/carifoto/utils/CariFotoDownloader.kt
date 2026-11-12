package com.zaidun.carifoto.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import timber.log.Timber

object CariFotoDownloader {

    fun downloadPhoto(context: Context, fileId: String, fileName: String) {
        val cleanName = if (fileName.isNotBlank()) fileName else "IMG_${System.currentTimeMillis()}.jpg"
        val directDownloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"

        try {
            val request = DownloadManager.Request(Uri.parse(directDownloadUrl))
                .setTitle("CariFoto - $cleanName")
                .setDescription("Mengunduh foto $cleanName ke Galeri...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CariFoto/$cleanName")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Mengunduh $cleanName ke folder Downloads...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.e(e, "Gagal mengunduh file via DownloadManager, mengalihkan ke browser...")
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directDownloadUrl))
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Gagal mengunduh foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
