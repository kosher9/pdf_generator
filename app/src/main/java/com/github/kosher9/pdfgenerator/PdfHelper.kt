package com.github.kosher9.pdfgenerator

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

class PdfHelper {


    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex: Int = 0


    fun openPdf(file: File) {
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor)
    }


    fun renderPage(pageIndex: Int): Bitmap? {
        currentPage?.close()
        currentPage = pdfRenderer?.openPage(pageIndex)
        currentPageIndex = pageIndex
        val bitmap = Bitmap.createBitmap(
            currentPage?.width ?: 0,
            currentPage?.height ?: 0,
            Bitmap.Config.ARGB_8888
        )
        currentPage?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }


    fun getCurrentPageIndex(): Int {
        return currentPageIndex
    }


    fun getCurrentPage(): PdfRenderer.Page? {
        return currentPage
    }


    fun closePdf() {
        currentPage?.close()
        pdfRenderer?.close()
    }
}