package com.yuunus90.linecomment;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Objects;

public class LineComment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String filePath;
    private int lineNumber;
    private String comment;
    private transient boolean isDirty = false;
    private long creationTimestamp;

    public LineComment(String filePath, int lineNumber, String comment) {
        this(filePath, lineNumber, comment, System.currentTimeMillis());
    }

    public LineComment(String filePath, int lineNumber, String comment, long creationTimestamp) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.comment = comment;
        this.creationTimestamp = creationTimestamp;
        this.isDirty = true;
    }

    // Getters
    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getComment() {
        return comment;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    // Setters
    public void setFilePath(String filePath) {
        if (!Objects.equals(this.filePath, filePath)) {
            this.filePath = filePath;
            this.isDirty = true;
        }
    }

    public void setLineNumber(int lineNumber) {
        if (this.lineNumber != lineNumber) {
            this.lineNumber = lineNumber;
            this.isDirty = true;
        }
    }

    public void setComment(String comment) {
        if (!Objects.equals(this.comment, comment)) {
            this.comment = comment;
            this.isDirty = true;
        }
    }

    @Transient
    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
} 