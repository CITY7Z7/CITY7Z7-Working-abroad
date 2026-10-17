package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupManager
import com.example.data.database.CityDatabase
import com.example.data.pack.ContentPackImporter
import com.example.data.repository.CityRepository
import com.example.ui.components.BreadcrumbsHeader
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: CityRepository,
    database: CityDatabase,
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showRestorePasswordDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    // Backup Create SAF
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            showBackupPasswordDialog = true
            selectedRestoreUri = uri
        }
    }

    // Restore Backup SAF
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedRestoreUri = uri
            showRestorePasswordDialog = true
        }
    }

    // Import Pack SAF (.c7pack)
    val importPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    val result = ContentPackImporter.importPack(database, text)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Контент обновлен до версии ${result.getOrNull()}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Ошибка: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Не удалось прочитать пакет: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        BreadcrumbsHeader(
            breadcrumbs = listOf("Настройки", "Параметры и безопасность"),
            onBack = onBack
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
                // Emergency Assistance Card (moved from main menu per user request)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Экстренная помощь (SOS 112)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            text = "Единый номер экстренных служб в Европе (полиция, скорая помощь, спасатели) — 112. В Ирландии также действует 999.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                context.startActivity(dialIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Вызов 112 (Полиция / Скорая)")
                        }
                    }
                }

                // Theme Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Тема оформления", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentTheme == "AUTO",
                                onClick = { onThemeChange("AUTO") },
                                label = { Text("Авто") }
                            )
                            FilterChip(
                                selected = currentTheme == "LIGHT",
                                onClick = { onThemeChange("LIGHT") },
                                label = { Text("Светлая") }
                            )
                            FilterChip(
                                selected = currentTheme == "DARK",
                                onClick = { onThemeChange("DARK") },
                                label = { Text("Тёмная (OKLCH)") }
                            )
                        }
                    }
                }

                // Backup & Restore Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Зашифрованная резервная копия (.c7bak)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Копия включает базу заметок, чек-листы, профиль и все документы. Шифруется алгоритмом AES-256-GCM с PBKDF2 (600 000 итераций). Без пароля открыть невозможно.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { createBackupLauncher.launch("city7z7_backup_${System.currentTimeMillis()}.c7bak") }
                            ) {
                                Text("Создать копию")
                            }
                            OutlinedButton(
                                onClick = { restoreBackupLauncher.launch(arrayOf("*/*")) }
                            ) {
                                Text("Восстановить")
                            }
                        }
                    }
                }

                // Content Pack Updates
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Обновление справочника без интернета", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Загрузите файл пакета (.c7pack) с официальной подписью ECDSA P-256 через флешку или локальный файл.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { importPackLauncher.launch(arrayOf("*/*")) }
                        ) {
                            Text("Импорт пакета (.c7pack)")
                        }
                    }
                }

                // Clear All Data
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Очистить все данные", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text(
                            text = "Удаляет все документы, заметки, профиль и журнал отправок. Требуется подтверждение.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Очистить все данные")
                        }
                    }
                }

                // About & Version Card (v2.4.2)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("app_version_info"),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CITY7Z7 Working Abroad",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = "v2.4.2",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).testTag("app_version_label")
                                )
                            }
                        }

                        Text(
                            text = "100% автономный режим: разрешения сети INTERNET полностью отсутствуют в приложении. " +
                                    "Шифрование документов: AES-256-GCM (Android Keystore). " +
                                    "Локальная база данных: Room с FTS4 и автономным импортом знаний.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Backup Create Dialog
    if (showBackupPasswordDialog && selectedRestoreUri != null) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupPasswordDialog = false },
            title = { Text("Пароль для шифрования копии") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Введите пароль длиной не менее 8 символов:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.length >= 8) {
                            val uri = selectedRestoreUri!!
                            showBackupPasswordDialog = false
                            scope.launch {
                                try {
                                    val stream = context.contentResolver.openOutputStream(uri)
                                    if (stream != null) {
                                        BackupManager.createBackup(context, database, passwordInput, stream)
                                        Toast.makeText(context, "Резервная копия успешно создана", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Ошибка бэкапа: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Минимум 8 символов", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPasswordDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Restore Password Dialog
    if (showRestorePasswordDialog && selectedRestoreUri != null) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestorePasswordDialog = false },
            title = { Text("Восстановление из копии") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Введите пароль от резервной копии. Текущие данные будут обновлены.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Пароль") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedRestoreUri!!
                        showRestorePasswordDialog = false
                        scope.launch {
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                if (stream != null) {
                                    BackupManager.restoreBackup(context, database, passwordInput, stream)
                                    Toast.makeText(context, "Данные успешно восстановлены", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ошибка восстановления: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Восстановить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestorePasswordDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Clear Data Confirmation Dialog (Requires typing УДАЛИТЬ)
    if (showClearDataDialog) {
        var confirmInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Подтверждение очистки") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Для подтверждения полного удаления всех данных введите слово УДАЛИТЬ:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it },
                        label = { Text("УДАЛИТЬ") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (confirmInput == "УДАЛИТЬ") {
                            scope.launch {
                                repository.clearAllUserData()
                                showClearDataDialog = false
                                Toast.makeText(context, "Все данные удалены", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = confirmInput == "УДАЛИТЬ"
                ) {
                    Text("Удалить всё")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
