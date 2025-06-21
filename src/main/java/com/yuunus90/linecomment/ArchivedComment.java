package com.yuunus90.linecomment;

import java.io.Serializable;

public class ArchivedComment implements Serializable {
    private final String filePath;
    private final int originalLineNumber;
    private final String comment;
    private final String deletedCodeLine;

    public ArchivedComment(String filePath, int originalLineNumber, String comment, String deletedCodeLine) {
        this.filePath = filePath;
        this.originalLineNumber = originalLineNumber;
        this.comment = comment;
        this.deletedCodeLine = deletedCodeLine;
    }

    // Getters
    public String getFilePath() { return filePath; }
    public int getOriginalLineNumber() { return originalLineNumber; }
    public String getComment() { return comment; }
    public String getDeletedCodeLine() { return deletedCodeLine; }
} 