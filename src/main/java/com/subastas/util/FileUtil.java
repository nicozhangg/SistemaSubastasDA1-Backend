package com.subastas.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.UUID;

/** Utilidades de manejo de archivos subidos. */
public final class FileUtil {

    private FileUtil() {}

    public static String uuidFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT)
                : ".bin";
        return UUID.randomUUID() + ext;
    }
}
