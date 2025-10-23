package com.example.testapplication

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * Extension function on ContentResolver to copy a file from its URI to a local cache file.
 * This is necessary for Retrofit to read the file content.
 */
fun android.content.ContentResolver.getFile(context: Context, uri: Uri): File {
    // 1. Get the filename
    val fileName = this.getFileName(uri)
    // 2. Create a temporary file in the cache directory
    val file = File(context.cacheDir, fileName)

    // 3. Copy the data from the URI stream to the file
    this.openInputStream(uri).use { inputStream ->
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
    }
    return file
}

/**
 * Extension function on ContentResolver to retrieve the display name of a file URI.
 */
fun android.content.ContentResolver.getFileName(uri: Uri): String {
    var name = ""
    val cursor = this.query(uri, null, null, null, null)
    cursor?.use {
        it.moveToFirst()
        // Get the display name index
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
            name = it.getString(nameIndex)
        }
    }
    return name
}
