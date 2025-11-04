package org.example.model;

import java.time.LocalDate;

public record FileRecord(
        int id,
        String filename,
        String path,
        int wordCount,
        LocalDate importDate
) {}
