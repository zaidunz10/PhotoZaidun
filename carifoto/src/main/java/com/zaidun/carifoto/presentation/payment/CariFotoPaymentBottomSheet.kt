package com.zaidun.carifoto.presentation.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zaidun.carifoto.data.remote.CariFotoPhotoDto
import com.zaidun.carifoto.data.remote.CariFotoSessionDto
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CariFotoPaymentBottomSheet(
    photo: CariFotoPhotoDto,
    session: CariFotoSessionDto?,
    onDismiss: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isProcessing by remember { mutableStateOf(false) }

    val photoPrice = session?.priceSingle ?: 10000
    val adminFee = 2000
    val totalPrice = photoPrice + adminFee

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Detail Foto & Pembayaran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Photo Preview Box (Anti-Crop Aspect Ratio)
            val driveUrl = "https://lh3.googleusercontent.com/d/${photo.driveFileId}"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = driveUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Watermark overlay simulation
                    Text(
                        "PREVIEW WATERMARK",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // File Info
            Text(
                photo.fileName.ifBlank { "IMG_PHOTO.JPG" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            val locationStr = session?.location ?: "Lokasi Hunting"
            val timeStr = photo.exifTime ?: "06:12 WIB"
            Text(
                "$locationStr  •  $timeStr",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(Modifier.height(16.dp))

            // QRIS Barcode Box Simulation
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF7B4FE0), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Bayar Instan via QRIS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mendukung GoPay, OVO, ShopeePay, DANA, LinkAja & BCA Mobile",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Price Breakdown Card
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Harga Foto (Fotografer)", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(currencyFormat.format(photoPrice), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Biaya Layanan Aplikasi", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(currencyFormat.format(adminFee), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFCBD5E1))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Pembayaran", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(
                            currencyFormat.format(totalPrice),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B4FE0)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Pay Button
            Button(
                onClick = {
                    isProcessing = true
                    onPaymentSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B4FE0)),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Bayar Sekarang (${currencyFormat.format(totalPrice)})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
