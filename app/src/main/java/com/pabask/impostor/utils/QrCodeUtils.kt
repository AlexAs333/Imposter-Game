package com.pabask.impostor.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeUtils {

    private const val COLOR_DATA = 0xFF0F111A.toInt() // Tu negro-azulado
    private const val COLOR_BG = 0xFFFFFFFF.toInt()   // Blanco

    fun generateQrBitmap(content: String, size: Int = 1024): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M // Bajamos a M para que sea menos denso y más fácil de leer
            hints[EncodeHintType.MARGIN] = 1

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val scaleFactor = size / matrixWidth.toFloat()

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(COLOR_BG)

            val paint = Paint().apply {
                color = COLOR_DATA
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            for (x in 0 until matrixWidth) {
                for (y in 0 until matrixHeight) {
                    if (bitMatrix[x, y]) {

                        // CÁLCULO DE POSICIÓN
                        val left = x * scaleFactor
                        val top = y * scaleFactor
                        val right = left + scaleFactor
                        val bottom = top + scaleFactor

                        // ¿ES UNA DE LAS 3 ESQUINAS IMPORTANTES (FINDER PATTERNS)?
                        // Los patrones de búsqueda son cuadrados de 7x7 en las esquinas
                        val isFinderPattern =
                            (x < 7 && y < 7) || // Arriba Izquierda
                                    (x > matrixWidth - 8 && y < 7) || // Arriba Derecha
                                    (x < 7 && y > matrixHeight - 8)   // Abajo Izquierda

                        if (isFinderPattern) {
                            // --- MODO SEGURO: CUADRADO PERFECTO ---
                            // Esto asegura que la cámara lo detecte al instante
                            canvas.drawRect(left, top, right + 0.5f, bottom + 0.5f, paint)
                            // (+0.5f es un truco para evitar líneas blancas finas entre bloques)
                        } else {
                            // --- MODO COOL: REDONDEADO ---
                            // Reducimos el padding al mínimo para que se toquen un poco
                            val padding = scaleFactor * 0.05f
                            val rect = RectF(left + padding, top + padding, right - padding, bottom - padding)

                            // Redondeamos, pero no tanto como antes
                            val cornerRadius = scaleFactor * 0.25f
                            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
                        }
                    }
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}