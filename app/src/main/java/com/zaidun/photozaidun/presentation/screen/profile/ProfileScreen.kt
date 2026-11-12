package com.zaidun.photozaidun.presentation.screen.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.zaidun.photozaidun.data.remote.SupabaseHelper
import com.zaidun.photozaidun.data.remote.UserDto
import com.zaidun.photozaidun.presentation.component.motion.PressAnimatedButton
import com.zaidun.photozaidun.presentation.screen.settings.SettingsUiState
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

private val ScreenBg = Color(0xFFF7F4FC)
private val CardWhite = Color(0xFFFFFFFF)
private val TitleDark = Color(0xFF2A1A44)
private val TextGray = Color(0xFF64748B)
private val AccentPurple = Color(0xFF7B4FE0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSaveSuccess: (
        whatsapp: String,
        instagram: String,
        bankName: String,
        accountNumber: String,
        accountHolderName: String,
        priceSingle: Int,
        minQtyDiscount: Int,
        priceDiscount: Int?
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var whatsapp by remember { mutableStateOf(uiState.photographerWa) }
    var instagram by remember { mutableStateOf(uiState.photographerIg) }
    var bankName by remember { mutableStateOf(uiState.bankName) }
    var accountNumber by remember { mutableStateOf(uiState.accountNumber) }
    var accountHolder by remember { mutableStateOf(uiState.accountHolderName) }
    var photoPrice by remember { mutableStateOf(uiState.defaultPhotoPrice.toString()) }
    var minQtyText by remember { mutableStateOf(uiState.minQtyDiscount.toString()) }
    var discountPriceText by remember { mutableStateOf(uiState.priceDiscount?.toString() ?: "") }

    val bankOptions = remember {
        listOf("BCA", "Mandiri", "BRI", "BNI", "SeaBank", "Bank Jago", "GoPay", "OVO", "DANA", "ShopeePay", "LinkAja")
    }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var screenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenVisible = true
    }

    LaunchedEffect(
        uiState.photographerWa,
        uiState.photographerIg,
        uiState.bankName,
        uiState.accountNumber,
        uiState.accountHolderName,
        uiState.defaultPhotoPrice,
        uiState.minQtyDiscount,
        uiState.priceDiscount
    ) {
        whatsapp = uiState.photographerWa
        instagram = uiState.photographerIg
        if (uiState.bankName.isNotBlank()) bankName = uiState.bankName
        if (uiState.accountNumber.isNotBlank()) accountNumber = uiState.accountNumber
        if (uiState.accountHolderName.isNotBlank()) accountHolder = uiState.accountHolderName
        if (uiState.defaultPhotoPrice > 0) photoPrice = uiState.defaultPhotoPrice.toString()
        if (uiState.minQtyDiscount > 0) minQtyText = uiState.minQtyDiscount.toString()
        val discount = uiState.priceDiscount
        if (discount != null && discount > 0) {
            discountPriceText = discount.toString()
        }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            TopAppBar(
                title = { Text("Profil & Rekening Fotografer", fontWeight = FontWeight.Bold, color = TitleDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TitleDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBg)
            )
        }
    ) { padding ->
        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { -it / 10 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Foto Profil Avatar
                val photoUrl = uiState.googleUser?.photoUrl
                val userName = uiState.googleUser?.name ?: "GUest"
                val initials = userName.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

                Surface(
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFD6C7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "ZS" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 30.sp,
                                        color = Color(0xFF2E184D)
                                    )
                                }
                            },
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFD6C7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "ZS" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 30.sp,
                                        color = Color(0xFF2E184D)
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFD6C7FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials.ifEmpty { "ZS" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                color = Color(0xFF2E184D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nama & Email Google
                Text(
                    text = uiState.googleUser?.name ?: "Guest",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitleDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.googleUser?.email ?: "Belum Login",
                    fontSize = 12.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                // =========================================================
                // CARD 1: INFORMASI FOTOGRAFER (WA & IG)
                // =========================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "KONTAK & MEDSOS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Form WhatsApp
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("Nomor WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("08123456789", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Form Instagram
                        OutlinedTextField(
                            value = instagram,
                            onValueChange = { instagram = it },
                            label = { Text("Username Instagram", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("@zaidun_photo", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =========================================================
                // CARD 2: TARIF & HARGA FOTO DISKON
                // =========================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "TARIF & DISKON FOTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Harga Eceran per Foto
                        OutlinedTextField(
                            value = photoPrice,
                            onValueChange = { photoPrice = it.filter(Char::isDigit) },
                            label = { Text("Harga Standar per Foto (Rp)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("10000", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Diskon Paket Grosir (Min Qty & Harga Diskon)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = minQtyText,
                                onValueChange = { minQtyText = it.filter(Char::isDigit) },
                                label = { Text("Min. Foto Grosir", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                placeholder = { Text("3", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = discountPriceText,
                                onValueChange = { discountPriceText = it.filter(Char::isDigit) },
                                label = { Text("Harga Paket Diskon (Rp)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                placeholder = { Text("25000", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =========================================================
                // CARD 3: REKENING PENCAIRAN SALDO (DROPDOWN BANK)
                // =========================================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "REKENING PENCAIRAN SALDO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dropdown Bank / e-Wallet
                        ExposedDropdownMenuBox(
                            expanded = bankDropdownExpanded,
                            onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Nama Bank / e-Wallet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = bankDropdownExpanded,
                                onDismissRequest = { bankDropdownExpanded = false }
                            ) {
                                bankOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, fontSize = 13.sp) },
                                        onClick = {
                                            bankName = option
                                            bankDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nomor Rekening / e-Wallet
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it.filter(Char::isDigit) },
                            label = { Text("Nomor Rekening / No. HP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("1234567890", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nama Pemilik Rekening
                        OutlinedTextField(
                            value = accountHolder,
                            onValueChange = { accountHolder = it },
                            label = { Text("Nama Pemilik Rekening", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Sesuai Buku Tabungan / KTP", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Simpan
                PressAnimatedButton(
                    onClick = {
                        if (whatsapp.isBlank() || instagram.isBlank()) {
                            Toast.makeText(context, "Harap lengkapi WhatsApp dan Instagram", Toast.LENGTH_SHORT).show()
                            return@PressAnimatedButton
                        }

                        val user = uiState.googleUser
                        if (user == null) {
                            Toast.makeText(context, "Harap login Google terlebih dahulu", Toast.LENGTH_LONG).show()
                            return@PressAnimatedButton
                        }

                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val userDto = UserDto(
                                    id = user.id,
                                    name = user.name,
                                    email = user.email,
                                    avatarUrl = user.photoUrl
                                )

                                try {
                                    SupabaseHelper.client.postgrest.from("users").upsert(userDto)
                                } catch (e: Exception) {
                                    // Abaikan jika ada perbedaan RLS atau kolom di Supabase users
                                }

                                val priceSingleInt = photoPrice.toIntOrNull() ?: 10000
                                val minQtyInt = minQtyText.toIntOrNull() ?: 3
                                val discountPriceInt = discountPriceText.toIntOrNull()

                                onSaveSuccess(whatsapp, instagram, bankName, accountNumber, accountHolder, priceSingleInt, minQtyInt, discountPriceInt)

                                Toast.makeText(context, "Profil & Rekening berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Simpan Profil & Rekening", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
