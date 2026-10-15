package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.entity.DocumentCategory
import com.example.data.database.entity.DocumentEntity
import com.example.data.database.entity.DocumentType
import com.example.data.database.entity.PersonProfileEntity
import com.example.data.database.entity.ShareLogEntity
import com.example.data.repository.CityRepository
import com.example.domain.vcard.VCardBuilder
import com.example.domain.watermark.WatermarkHelper
import com.example.domain.xlsx.CsvReader
import com.example.domain.xlsx.SpreadsheetData
import com.example.domain.xlsx.XlsxReader
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate

enum class QrCardMode {
    VCARD,
    RESUME,
    DOCUMENTS_STATUS
}

class DocumentHubViewModel(
    private val context: Context,
    private val repository: CityRepository
) : ViewModel() {

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allActiveDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedDocuments: StateFlow<List<DocumentEntity>> = repository.pinnedDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<DocumentEntity>> = repository.recentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringDocuments: StateFlow<List<DocumentEntity>> = repository.getExpiringDocuments(90)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<PersonProfileEntity?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val shareLogs: StateFlow<List<ShareLogEntity>> = repository.shareLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Document Viewing ---
    private val _activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocument: StateFlow<DocumentEntity?> = _activeDocument.asStateFlow()

    private val _activeDecryptedBytes = MutableStateFlow<ByteArray?>(null)
    val activeDecryptedBytes: StateFlow<ByteArray?> = _activeDecryptedBytes.asStateFlow()

    private val _activeSpreadsheetData = MutableStateFlow<SpreadsheetData?>(null)
    val activeSpreadsheetData: StateFlow<SpreadsheetData?> = _activeSpreadsheetData.asStateFlow()

    private val _activePdfPageCount = MutableStateFlow(0)
    val activePdfPageCount: StateFlow<Int> = _activePdfPageCount.asStateFlow()

    private var activePdfRenderer: PdfRenderer? = null
    private var activePdfPfd: ParcelFileDescriptor? = null
    private var tempPdfFile: File? = null

    fun selectDocument(documentId: String) {
        viewModelScope.launch {
            closeActiveDocument()
            val doc = repository.getDocumentById(documentId) ?: return@launch
            _activeDocument.value = doc

            val bytes = try {
                repository.getDecryptedDocumentBytes(doc)
            } catch (e: Exception) {
                null
            }
            _activeDecryptedBytes.value = bytes

            if (bytes != null) {
                when {
                    doc.mime.contains("csv") || doc.title.endsWith(".csv", ignoreCase = true) -> {
                        _activeSpreadsheetData.value = CsvReader.read(ByteArrayInputStream(bytes))
                    }
                    doc.mime.contains("spreadsheet") || doc.title.endsWith(".xlsx", ignoreCase = true) -> {
                        try {
                            _activeSpreadsheetData.value = XlsxReader.read(ByteArrayInputStream(bytes))
                        } catch (_: Exception) {
                            _activeSpreadsheetData.value = null
                        }
                    }
                    doc.mime.contains("pdf") || doc.title.endsWith(".pdf", ignoreCase = true) -> {
                        initPdfRenderer(bytes)
                    }
                }
            }
        }
    }

    private fun initPdfRenderer(pdfBytes: ByteArray) {
        try {
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val tempFile = File.createTempFile("pdf_view_", ".pdf", shareDir)
            FileOutputStream(tempFile).use { it.write(pdfBytes) }
            tempPdfFile = tempFile

            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            activePdfPfd = pfd
            val renderer = PdfRenderer(pfd)
            activePdfRenderer = renderer
            _activePdfPageCount.value = renderer.pageCount
        } catch (_: Exception) {
            _activePdfPageCount.value = 0
        }
    }

    fun renderPdfPage(pageIndex: Int): Bitmap? {
        val renderer = activePdfRenderer ?: return null
        if (pageIndex !in 0 until renderer.pageCount) return null
        return try {
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun closeActiveDocument() {
        try {
            activePdfRenderer?.close()
            activePdfRenderer = null
            activePdfPfd?.close()
            activePdfPfd = null
            tempPdfFile?.delete()
            tempPdfFile = null
        } catch (_: Exception) {
        }
        _activeDocument.value = null
        _activeDecryptedBytes.value = null
        _activeSpreadsheetData.value = null
        _activePdfPageCount.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        closeActiveDocument()
    }

    // --- Document Actions ---
    fun importDocument(
        title: String,
        category: DocumentCategory,
        type: DocumentType,
        mime: String,
        expiresAt: Long?,
        inputStream: InputStream,
        onSuccess: (DocumentEntity) -> Unit
    ) {
        viewModelScope.launch {
            val doc = repository.importDocumentFile(title, category, type, mime, expiresAt, inputStream)
            onSuccess(doc)
        }
    }

    fun togglePinned(documentId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.updateDocumentPinned(documentId, !currentPinned)
        }
    }

    fun deleteDocument(documentId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (_activeDocument.value?.id == documentId) {
                closeActiveDocument()
            }
            repository.deleteDocument(documentId)
            onDeleted()
        }
    }

    // --- Sharing & Watermarking ---
    suspend fun prepareShareUri(
        document: DocumentEntity,
        recipient: String? = null,
        watermarkEnabled: Boolean = true
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val decryptedBytes = repository.getDecryptedDocumentBytes(document)
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val ext = when {
                document.mime.contains("pdf") -> ".pdf"
                document.mime.contains("png") -> ".png"
                document.mime.contains("webp") -> ".webp"
                document.mime.contains("csv") -> ".csv"
                document.mime.contains("spreadsheet") -> ".xlsx"
                else -> ".jpg"
            }
            val targetFile = File(shareDir, "Share_${document.title.replace(" ", "_")}$ext")

            if (watermarkEnabled && (document.mime.contains("image") || ext == ".jpg" || ext == ".png" || ext == ".webp")) {
                val originalBitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                if (originalBitmap != null) {
                    val profileText = profile.value?.watermarkCustomText
                    val watermarkText = WatermarkHelper.buildWatermarkText(profileText, recipient)
                    val watermarkedBitmap = WatermarkHelper.applyWatermarkToBitmap(originalBitmap, watermarkText)
                    WatermarkHelper.saveWatermarkedBitmap(watermarkedBitmap, targetFile)
                } else {
                    targetFile.writeBytes(decryptedBytes)
                }
            } else {
                targetFile.writeBytes(decryptedBytes)
            }

            // Log the sharing event
            repository.logShare(recipient, listOf(document.title), watermarkEnabled)

            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, targetFile)
        } catch (_: Exception) {
            null
        }
    }

    // --- QR-Code Generation (ZXing Core) ---
    fun generateQrMatrix(
        content: String,
        size: Int = 0
    ): Array<BooleanArray>? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val writer = QRCodeWriter()
            // Using size 0 gives the minimal module dimensions (e.g. 29x29 or 33x33), drastically reducing loop size
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val result = Array(height) { BooleanArray(width) }
            for (y in 0 until height) {
                for (x in 0 until width) {
                    result[y][x] = bitMatrix.get(x, y)
                }
            }
            result
        } catch (_: Exception) {
            null
        }
    }

    fun generateQrBitmap(
        content: String,
        size: Int = 512,
        moduleColor: Int = android.graphics.Color.BLACK,
        bgColor: Int = android.graphics.Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) moduleColor else bgColor
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun buildVCardContent(): String {
        val p = profile.value ?: return ""
        val contactData = VCardBuilder.ContactData(
            fullNameLatin = p.fullNameLatin.ifBlank { p.fullNameRu },
            profession = p.profession,
            phone = p.phone,
            email = p.email,
            country = p.countryResidenceId ?: "",
            note = p.about
        )
        return VCardBuilder.buildVCard3(contactData)
    }

    class Factory(
        private val context: Context,
        private val repository: CityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DocumentHubViewModel(context, repository) as T
        }
    }
}
