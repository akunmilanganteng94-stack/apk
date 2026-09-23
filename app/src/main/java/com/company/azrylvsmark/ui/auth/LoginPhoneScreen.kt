package com.company.azrylvsmark.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.azrylvsmark.data.repository.AuthRepository
import com.company.azrylvsmark.ui.theme.BorderNavy
import com.company.azrylvsmark.ui.theme.CardNavy
import com.company.azrylvsmark.ui.theme.ContainerNavy
import com.company.azrylvsmark.ui.theme.DeepNavy
import com.company.azrylvsmark.ui.theme.ElectricBlue
import com.company.azrylvsmark.ui.theme.NeonCyan
import com.company.azrylvsmark.ui.theme.TextMutedDark
import com.company.azrylvsmark.ui.theme.TextPrimaryDark
import com.company.azrylvsmark.ui.theme.TextSecondaryDark
import com.company.azrylvsmark.utils.CryptoUtils

data class CountryCodeOption(val name: String, val code: String, val flag: String)

@Composable
fun LoginPhoneScreen(
    authRepository: AuthRepository,
    onCodeSent: (verificationId: String, phoneNumber: String) -> Unit,
    onAutoVerified: () -> Unit
) {
    val context = LocalContext.current
    val countryList = listOf(
        CountryCodeOption("Indonesia", "+62", "🇮🇩"),
        CountryCodeOption("Malaysia", "+60", "🇲🇾"),
        CountryCodeOption("Singapore", "+65", "🇸🇬"),
        CountryCodeOption("United States", "+1", "🇺🇸"),
        CountryCodeOption("United Kingdom", "+44", "🇬🇧"),
        CountryCodeOption("Saudi Arabia", "+966", "🇸🇦"),
        CountryCodeOption("India", "+91", "🇮🇳"),
        CountryCodeOption("Australia", "+61", "🇦🇺")
    )

    var selectedCountry by remember { mutableStateOf(countryList[0]) }
    var isCountryDropdownOpen by remember { mutableStateOf(false) }

    var rawInputPhone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cleaned phone number without trunk zero or duplicate country code
    val cleanedPhone by remember(rawInputPhone, selectedCountry) {
        derivedStateOf {
            var digits = rawInputPhone.filter { it.isDigit() }
            if (digits.startsWith("0")) {
                digits = digits.substring(1)
            }
            val ccWithoutPlus = selectedCountry.code.removePrefix("+")
            if (digits.startsWith(ccWithoutPlus)) {
                digits = digits.removePrefix(ccWithoutPlus)
                if (digits.startsWith("0")) digits = digits.substring(1)
            }
            digits
        }
    }

    val fullE164Phone by remember(cleanedPhone, selectedCountry) {
        derivedStateOf {
            if (cleanedPhone.isBlank()) ""
            else "${selectedCountry.code}$cleanedPhone"
        }
    }

    val formattedDisplayPhone by remember(fullE164Phone) {
        derivedStateOf {
            if (fullE164Phone.isNotBlank()) CryptoUtils.formatDisplayPhoneNumber(fullE164Phone) else ""
        }
    }

    fun submitPhone() {
        if (cleanedPhone.length < 7) {
            errorMessage = "Silakan masukkan nomor telepon yang valid (minimal 7 digit)."
            return
        }

        errorMessage = null
        isLoading = true
        val fullPhone = fullE164Phone

        val activity = context as? Activity
        if (activity != null) {
            authRepository.sendOtp(
                activity = activity,
                phoneNumber = fullPhone,
                forceResendingToken = null,
                onCodeSent = { verificationId, _ ->
                    isLoading = false
                    Toast.makeText(context, "Kode verifikasi telah dikirim ke $formattedDisplayPhone", Toast.LENGTH_SHORT).show()
                    onCodeSent(verificationId, fullPhone)
                },
                onVerificationFailed = { error ->
                    isLoading = false
                    errorMessage = error
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                },
                onAutoVerified = {
                    isLoading = false
                    Toast.makeText(context, "Verifikasi otomatis berhasil!", Toast.LENGTH_SHORT).show()
                    onAutoVerified()
                }
            )
        } else {
            isLoading = false
            errorMessage = "Gagal mengakses Activity konteks."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .testTag("login_phone_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // App Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(ElectricBlue, ContainerNavy))
                        )
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AZMASSANGE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Heading
            Text(
                text = "Masuk dengan Nomor HP",
                color = TextPrimaryDark,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Masukkan nomor HP Anda. Kode verifikasi akan dikirimkan langsung melalui SMS ke nomor tersebut.",
                color = TextSecondaryDark,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Phone Input Row with Country Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Selector Box
                Box {
                    Row(
                        modifier = Modifier
                            .width(96.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardNavy)
                            .border(1.dp, BorderNavy, RoundedCornerShape(14.dp))
                            .clickable { isCountryDropdownOpen = true }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedCountry.flag} ${selectedCountry.code}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Negara",
                            tint = TextMutedDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isCountryDropdownOpen,
                        onDismissRequest = { isCountryDropdownOpen = false },
                        modifier = Modifier.background(CardNavy)
                    ) {
                        countryList.forEach { country ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${country.flag} ${country.name} (${country.code})",
                                        color = TextPrimaryDark,
                                        fontSize = 13.5.sp
                                    )
                                },
                                onClick = {
                                    selectedCountry = country
                                    isCountryDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Phone Input Field
                OutlinedTextField(
                    value = rawInputPhone,
                    onValueChange = { input ->
                        rawInputPhone = input.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }
                    },
                    placeholder = {
                        Text("812-3456-7890", color = TextMutedDark, fontSize = 15.sp)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitPhone() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardNavy,
                        unfocusedContainerColor = CardNavy,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderNavy,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("phone_number_input")
                )
            }

            // Live preview banner of target SMS number
            AnimatedVisibility(visible = formattedDisplayPhone.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ContainerNavy)
                        .border(1.dp, BorderNavy, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kode SMS akan dikirim ke: ",
                            color = TextSecondaryDark,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = formattedDisplayPhone,
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2D1214))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Main "Kirim Kode SMS" Button
            Button(
                onClick = { submitPhone() },
                enabled = !isLoading && cleanedPhone.length >= 7,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    disabledContainerColor = ContainerNavy
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_phone_btn")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mengirim kode SMS...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kirim Kode ke SMS",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Test helper card (especially helpful in emulators or when testing Firebase phone auth)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardNavy.copy(alpha = 0.6f))
                    .border(1.dp, BorderNavy, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Uji Coba Cepat (Test Number)",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Anda dapat memasukkan nomor telepon asli Indonesia Anda (+62) atau klik di bawah untuk mengisi nomor pengujian otomatis.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                selectedCountry = countryList[0] // +62
                                rawInputPhone = "81234567890"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ContainerNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("quick_fill_btn")
                        ) {
                            Text(
                                text = "Isi +62 812-3456-7890",
                                color = NeonCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Security badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privasi Terlindungi",
                    tint = TextMutedDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encrypted & Private by Design",
                    color = TextMutedDark,
                    fontSize = 12.sp
                )
            }
        }
    }
}
