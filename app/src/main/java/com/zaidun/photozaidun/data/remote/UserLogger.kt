package com.zaidun.photozaidun.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UserLogger {

    private const val COLLECTION_NAME = "user_logs"

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    /**
     * Mencatat data pengguna saat Login ke Firestore:
     * - Email
     * - Nama
     * - Jam Login Terakhir
     * - Status: LOGGED_IN
     */
    fun logLogin(email: String, name: String) {
        if (email.isBlank()) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            val userData = mapOf(
                "email" to email,
                "name" to name,
                "last_login" to getCurrentTimestamp(),
                "status" to "LOGGED_IN"
            )

            firestore.collection(COLLECTION_NAME)
                .document(email)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    Timber.tag("UserLogger").d("Berhasil mencatat login di Firestore: $email")
                }
                .addOnFailureListener { e ->
                    Timber.tag("UserLogger").e(e, "Gagal mencatat login ke Firestore")
                }
        } catch (e: Exception) {
            Timber.tag("UserLogger").e(e, "Error Firestore logLogin: ${e.message}")
        }
    }

    /**
     * Mencatat data pengguna saat Logout ke Firestore:
     * - Jam Logout Terakhir
     * - Status: LOGGED_OUT
     */
    fun logLogout(email: String) {
        if (email.isBlank()) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            val logoutData = mapOf(
                "last_logout" to getCurrentTimestamp(),
                "status" to "LOGGED_OUT"
            )

            firestore.collection(COLLECTION_NAME)
                .document(email)
                .set(logoutData, SetOptions.merge())
                .addOnSuccessListener {
                    Timber.tag("UserLogger").d("Berhasil mencatat logout di Firestore: $email")
                }
                .addOnFailureListener { e ->
                    Timber.tag("UserLogger").e(e, "Gagal mencatat logout ke Firestore")
                }
        } catch (e: Exception) {
            Timber.tag("UserLogger").e(e, "Error Firestore logLogout: ${e.message}")
        }
    }

    /**
     * Mencatat riwayat export foto ke koleksi terpisah "export_logs":
     * - Email
     * - Nama
     * - Waktu Export
     * - Jumlah Foto yang Diproses
     */
    fun logExport(email: String, name: String, totalPhotos: Int) {
        if (email.isBlank()) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            val exportData = mapOf(
                "email" to email,
                "name" to name,
                "export_time" to getCurrentTimestamp(),
                "total_photos" to totalPhotos
            )

            firestore.collection("export_logs")
                .add(exportData)
                .addOnSuccessListener {
                    Timber.tag("UserLogger").d("Berhasil mencatat log export ke Firestore: $totalPhotos foto untuk $email")
                }
                .addOnFailureListener { e ->
                    Timber.tag("UserLogger").e(e, "Gagal mencatat log export ke Firestore")
                }
        } catch (e: Exception) {
            Timber.tag("UserLogger").e(e, "Error Firestore logExport: ${e.message}")
        }
    }
}
