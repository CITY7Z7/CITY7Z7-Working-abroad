package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.UserContactEntity
import com.example.data.repository.CityRepository
import com.example.ui.components.BreadcrumbsHeader

@Composable
fun SosScreen(
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showStaffAssistant by remember { mutableStateOf(false) }

    val userContacts by repository.getContacts(null).collectAsState(initial = emptyList())
    val profile by repository.profile.collectAsState(initial = null)

    if (showStaffAssistant) {
        FullscreenStaffAssistant(
            profile = profile,
            onClose = { showStaffAssistant = false }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("sos_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Экстренно", "Экстренные службы и SOS"),
            onBack = onBack,
            actions = {
                Button(
                    onClick = { showStaffAssistant = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.testTag("show_staff_button")
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Показать сотруднику", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        BoxContainerCentered {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emergency Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Emergency, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Единая служба спасения Европы",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Звонок на 112 или 999 бесплатен и доступен даже без SIM-карты и при заблокированном экране.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Official Emergency Numbers
                Text(
                    text = "Официальные экстренные линии",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                EmergencyNumberCard(
                    title = "112 / 999 — Скорая, полиция, пожарные",
                    subtitle = "Экстренная помощь при угрозе жизни и здоровью",
                    phone = "112",
                    onDial = { dialNumber(context, "112") }
                )

                EmergencyNumberCard(
                    title = "101 — Полиция (Garda) не экстренная",
                    subtitle = "Для сообщений о происшествиях без прямой угрозы жизни",
                    phone = "101",
                    onDial = { dialNumber(context, "101") }
                )

                EmergencyNumberCard(
                    title = "116 123 — Samaritans (Психологическая помощь)",
                    subtitle = "Круглосуточная бесплатная эмоциональная поддержка в кризисе",
                    phone = "116123",
                    onDial = { dialNumber(context, "116123") }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // User Contacts
                Text(
                    text = "Мои доверенные контакты",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (userContacts.isEmpty()) {
                    Text(
                        text = "Вы пока не добавили личные экстренные контакты (консульство, работодатель, доверенное лицо). Добавить можно в разделе «Мои контакты».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    userContacts.forEach { contact ->
                        EmergencyNumberCard(
                            title = contact.label,
                            subtitle = contact.note.ifBlank { "Личный контакт" },
                            phone = contact.phone,
                            onDial = { dialNumber(context, contact.phone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyNumberCard(
    title: String,
    subtitle: String,
    phone: String,
    onDial: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onDial,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = "Набрать")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Набрать ($phone)")
            }
        }
    }
}

@Composable
private fun FullscreenStaffAssistant(
    profile: PersonProfileEntity?,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
            .testTag("fullscreen_staff_assistant")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EMERGENCY ASSISTANCE CARD",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "• I NEED HELP.",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "• PLEASE CALL AN AMBULANCE OR THE POLICE.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Yellow
                        )
                        Text(
                            text = "• I NEED AN INTERPRETER. I SPEAK RUSSIAN.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "• I DO NOT UNDERSTAND THIS DOCUMENT. I CANNOT SIGN WITHOUT LEGAL ADVICE.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray
                        )
                    }
                }

                if (profile != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MY DETAILS / МОИ ДАННЫЕ:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "Name: ${profile.fullNameLatin.ifBlank { profile.fullNameRu }}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (profile.phone.isNotBlank()) {
                                Text(
                                    text = "Phone: ${profile.phone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Вернуться / Back", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun dialNumber(context: android.content.Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
