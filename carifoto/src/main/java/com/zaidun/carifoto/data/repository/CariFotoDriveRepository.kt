
package com.zaidun.carifoto.data.repository

import com.zaidun.carifoto.data.model.DrivePhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.zaidun.carifoto.BuildConfig

class CariFotoDriveRepository {

    private val apiKey = BuildConfig.GOOGLE_DRIVE_API_KEY

    private val baseUrl =
        "https://www.googleapis.com/drive/v3/files"

    suspend fun getPhotos(folderUrl: String): List<DrivePhotoItem> =
        withContext(Dispatchers.IO) {

            val folderId = extractFolderId(folderUrl)
                ?: throw IllegalArgumentException(
                    "Link folder Google Drive tidak valid"
                )

            val result = mutableListOf<DrivePhotoItem>()
            val visitedFolders = mutableSetOf<String>()

            getPhotosRecursive(
                folderId = folderId,
                result = result,
                visitedFolders = visitedFolders
            )

            result
        }

    private fun getPhotosRecursive(
        folderId: String,
        result: MutableList<DrivePhotoItem>,
        visitedFolders: MutableSet<String>
    ) {
        if (!visitedFolders.add(folderId)) return

        var pageToken: String? = null

        do {
            val query = "'$folderId' in parents and trashed = false"

            val params = mutableListOf(
                "q" to query,
                "key" to apiKey,
                "pageSize" to "1000",
                "supportsAllDrives" to "true",
                "includeItemsFromAllDrives" to "true",
                "fields" to "nextPageToken,files(id,name,mimeType,thumbnailLink,webViewLink)"
            )

            if (pageToken != null) {
                params.add("pageToken" to pageToken)
            }

            val queryString = params.joinToString("&") {
                "${encode(it.first)}=${encode(it.second)}"
            }

            val connection = URL("$baseUrl?$queryString")
                .openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                val responseCode = connection.responseCode

                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val responseText = stream.bufferedReader().use {
                    it.readText()
                }

                if (responseCode !in 200..299) {
                    throw Exception(
                        "Google Drive API error ($responseCode): $responseText"
                    )
                }

                val json = JSONObject(responseText)
                val files = json.optJSONArray("files")

                if (files != null) {
                    for (i in 0 until files.length()) {
                        val file = files.getJSONObject(i)

                        val id = file.optString("id")
                        val name = file.optString("name")
                        val mimeType = file.optString("mimeType")

                        if (mimeType == "application/vnd.google-apps.folder") {
                            getPhotosRecursive(
                                folderId = id,
                                result = result,
                                visitedFolders = visitedFolders
                            )
                        } else if (mimeType.startsWith("image/")) {
                            result.add(
                                DrivePhotoItem(
                                    id = id,
                                    name = name,
                                    mimeType = mimeType,
                                    thumbnailLink = file.optString(
                                        "thumbnailLink"
                                    ).takeIf { it.isNotBlank() },
                                    webViewLink = file.optString(
                                        "webViewLink"
                                    ).takeIf { it.isNotBlank() },
                                    imageUrl =
                                        "https://drive.google.com/thumbnail?id=$id&sz=w1000"
                                )
                            )
                        }
                    }
                }

                pageToken = json.optString("nextPageToken")
                    .takeIf { it.isNotBlank() }

            } finally {
                connection.disconnect()
            }

        } while (pageToken != null)
    }

    private fun extractFolderId(value: String): String? {
        val regex = Regex("""/folders/([a-zA-Z0-9_-]+)""")

        return regex.find(value)?.groupValues?.get(1)
            ?: value.takeIf {
                !it.contains("/") && it.isNotBlank()
            }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
    }
}
