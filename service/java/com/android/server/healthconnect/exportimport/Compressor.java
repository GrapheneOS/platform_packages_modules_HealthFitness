/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.healthconnect.exportimport;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Slog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class Compressor {

    private static final String TAG = "HealthConnectCompressor";

    /** Compresses the source file */
    void compress(File source, String entryName, File zip) throws IOException {
        try {
            zip.mkdirs();
            zip.delete();

            try (ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zip))) {
                outputStream.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream inputStream = new FileInputStream(source)) {
                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = inputStream.read(bytes)) >= 0) {
                        outputStream.write(bytes, 0, length);
                    }
                    outputStream.closeEntry();
                }
            }
            Slog.i(TAG, "File zipped: " + zip.getAbsolutePath());
        } catch (Exception e) {
            Slog.e(TAG, "Failed to compress", e);
            zip.delete();
            throw e;
        }
    }

    /** Decompresses the zip file */
    void decompress(Uri zip, String entryName, File destination, Context userContext)
            throws IOException {
        try {
            destination.mkdirs();
            destination.delete();

            ContentResolver contentResolver = userContext.getContentResolver();
            try (ZipInputStream zipInputStream =
                    new ZipInputStream(contentResolver.openInputStream(zip))) {
                try (FileOutputStream outputStream = new FileOutputStream(destination)) {

                    ZipEntry entry;
                    while (true) {
                        entry = zipInputStream.getNextEntry();
                        if (entry == null) {
                            throw new IllegalArgumentException("Entry not found in archive.");
                        }
                        Slog.d(TAG, "Entry found: " + entry.getName());
                        if (Objects.equals(entry.getName(), entryName)) {
                            break;
                        }
                        zipInputStream.closeEntry();
                    }

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = zipInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    zipInputStream.closeEntry();
                }
            }
            Slog.i(TAG, "File unzipped: " + destination.getAbsolutePath());
        } catch (Exception e) {
            Slog.e(TAG, "Failed to decompress", e);
            destination.delete();
            throw e;
        }
    }
}
