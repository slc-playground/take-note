package com.example.linecomment;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ArchivedComment implements Serializable {
    private final String filePath;
    private final int originalLineNumber;
    private final String comment;
    private final String codeLine;
    private final LocalDateTime archivedDate;

    public ArchivedComment(String filePath, int originalLineNumber, String comment, String codeLine) {
        this.filePath = filePath;
        this.originalLineNumber = originalLineNumber;
        this.comment = comment;
        this.codeLine = codeLine;
        this.archivedDate = LocalDateTime.now();
    }

    public String getFilePath() {
        return filePath;
    }

    public int getOriginalLineNumber() {
        return originalLineNumber;
    }

    public String getComment() {
        return comment;
    }

    public String getCodeLine() {
        return codeLine;
    }

    public LocalDateTime getArchivedDate() {
        return archivedDate;
    }
} 