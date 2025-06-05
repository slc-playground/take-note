package com.example.linecomment;

import java.io.Serializable;

public class LineComment implements Serializable {
    private final String filePath;
    private final String fileName;
    private int lineNumber;
    private final String comment;
    private final String username;
    private final long timestamp;

    public LineComment(String filePath, String fileName, int lineNumber, String comment, String username) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.comment = comment;
        this.username = username;
        this.timestamp = System.currentTimeMillis();
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getComment() {
        return comment;
    }

    public String getUsername() {
        return username;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LineComment withNewLineNumber(int newLineNumber) {
        return new LineComment(this.filePath, this.fileName, newLineNumber, this.comment, this.username);
    }
} 