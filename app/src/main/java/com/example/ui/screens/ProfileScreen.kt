package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.repository.CityRepository
import com.example.domain.watermark.WatermarkHelper
import com.example.ui.components.BreadcrumbsHeader
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    repository: CityRepository,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by repository.profile.collectAsState(initial = null)

    var fullNameRu by remember { mutableStateOf("") }
    var fullNameLatin by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var watermarkText by remember { mutableStateOf(WatermarkHelper.DEFAULT_LEGAL_NOTICE) }

    LaunchedEffect(profile) {
        profile?.let { p ->
            fullNameRu = p.fullNameRu
            fullNameLatin = p.fullNameLatin
            phone = p.phone
            email = p.email
            profession = p.profession
            about = p.about
            watermarkText = p.watermarkCustomText.ifBlank { WatermarkHelper.DEFAULT_LEGAL_NOTICE }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Профиль", "Личные данные и визитка"),
            onBack = onBack,
            actions = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.saveProfile(
                                PersonProfileEntity(
                                    id = 1,
                                    fullNameRu = fullNameRu.trim(),
                                    fullNameLatin = fullNameLatin.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    profession = profession.trim(),
                                    about = about.trim(),
                                    watermarkCustomText = watermarkText.trim()
                                )
                            )
                            Toast.makeText(context, "Профиль сохранён локально", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Сохранить")
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "🔒 Данные профиля хранятся исключительно на этом устройстве в зашифрованной базе данных и используются для заполнения шаблонов и QR-визитки.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = fullNameRu,
                    onValueChange = { fullNameRu = it },
                    label = { Text("ФИО на русском языке") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fullNameLatin,
                    onValueChange = { fullNameLatin = it },
                    label = { Text("ФИО латиницей (как в загранпаспорте)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    label = { Text("Профессия / Специальность") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = about,
                    onValueChange = { if (it.length <= 300) about = it },
                    label = { Text("Кратко о себе (до 300 символов)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { if (it.length <= 120) watermarkText = it },
                    label = { Text("Текст водяного знака для документов (до 120 симв.)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        }
    }
}
