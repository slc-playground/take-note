package com.example.linecomment;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LineCommentDocumentListener implements DocumentListener {
    private final Project project;
    private final Document document;

    public LineCommentDocumentListener(Project project, Document document) {
        this.project = project;
        this.document = document;
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) return;

        LineCommentService service = project.getService(LineCommentService.class);
        List<LineComment> comments = service.getCommentsForFile(file);
        if (comments.isEmpty()) return;

        int offset = event.getOffset();
        int oldLength = event.getOldLength();
        int newLength = event.getNewLength();
        int lineNumber = document.getLineNumber(offset);

        List<LineComment> updatedComments = new ArrayList<>();
        boolean needsUpdate = false;

        for (LineComment comment : comments) {
            int commentLine = comment.getLineNumber();
            
            if (commentLine == lineNumber && oldLength > 0) {
                // Bu satırdaki yorumu arşivle
                needsUpdate = true;
            } else if (commentLine > lineNumber) {
                // Bu satırdan sonraki yorumların satır numaralarını güncelle
                updatedComments.add(comment.withNewLineNumber(commentLine + (newLength - oldLength)));
                needsUpdate = true;
            } else {
                updatedComments.add(comment);
            }
        }

        if (needsUpdate) {
            // Tüm yorumları güncelle
            service.updateComments(file, updatedComments);
        }
    }
} 