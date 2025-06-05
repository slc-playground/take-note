package com.example.linecomment;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class LineCommentEditorListener implements EditorFactoryListener, EditorMouseListener, DocumentListener {
    private static final Logger LOG = Logger.getInstance(LineCommentEditorListener.class);
    private final Project project;

    public LineCommentEditorListener(Project project) {
        this.project = project;
        LOG.info("LineCommentEditorListener initialized for project: " + project.getName());
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        editor.addEditorMouseListener(this);
        editor.getDocument().addDocumentListener(this);
        
        VirtualFile file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                .getFile(editor.getDocument());
        if (file == null) {
            LOG.warn("File is null for editor created");
            return;
        }

        LOG.info("Editor created for file: " + file.getPath());
        
        // Önce tüm highlighters'ları temizle
        MarkupModel markupModel = editor.getMarkupModel();
        markupModel.removeAllHighlighters();
        
        // Yorumları yükle ve göster
        List<LineComment> comments = LineCommentService.getInstance(project).getCommentsForFile(file);
        LOG.info("Found " + comments.size() + " comments for file");
        
        for (LineComment comment : comments) {
            int lineNumber = comment.getLineNumber();
            if (lineNumber >= 0 && lineNumber < editor.getDocument().getLineCount()) {
                RangeHighlighter highlighter = markupModel.addLineHighlighter(
                    lineNumber,
                    HighlighterLayer.SELECTION - 1,
                    new TextAttributes(null, null, null, null, Font.PLAIN)
                );
                highlighter.setGutterIconRenderer(new LineCommentGutterIconRenderer(
                    lineNumber,
                    comment.getComment(),
                    comment.getUsername(),
                    comment.getTimestamp(),
                    file,
                    project
                ));
                LOG.info("Added comment icon at line: " + (lineNumber + 1));
            } else {
                LOG.warn("Comment line number out of bounds: " + lineNumber);
            }
        }
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        editor.removeEditorMouseListener(this);
        editor.getDocument().removeDocumentListener(this);
        LOG.info("Editor released: " + editor.getDocument().getText());
    }

    @Override
    public void mouseClicked(@NotNull EditorMouseEvent e) {
        if (e.getMouseEvent().getButton() == MouseEvent.BUTTON3) {
            Editor editor = e.getEditor();
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (file == null) {
                LOG.warn("File is null for mouse click");
                return;
            }

            int lineNumber = editor.getCaretModel().getLogicalPosition().line;
            LOG.info("Right click detected at line: " + (lineNumber + 1));

            String comment = Messages.showInputDialog(
                project,
                "Enter your comment:",
                "Add Line Comment",
                Messages.getQuestionIcon()
            );

            if (comment != null && !comment.trim().isEmpty()) {
                LineCommentService.getInstance(project).addComment(file, lineNumber, comment);
                LOG.info("Comment added at line: " + (lineNumber + 1));
            }
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        VirtualFile file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                .getFile(event.getDocument());
        if (file == null) {
            LOG.warn("File is null for document change");
            return;
        }

        int offset = event.getOffset();
        int oldLength = event.getOldLength();
        int newLength = event.getNewLength();
        
        // Satır değişikliğini hesapla
        Document document = event.getDocument();
        int oldLineCount = document.getLineCount() - (newLength - oldLength);
        int newLineCount = document.getLineCount();
        int lineDiff = newLineCount - oldLineCount;

        if (lineDiff != 0) {
            LOG.info("Document changed: offset=" + offset + ", oldLength=" + oldLength + 
                    ", newLength=" + newLength + ", lineDiff=" + lineDiff);
            
            // Değişikliğin yapıldığı satırı bul
            int changedLine = document.getLineNumber(offset);
            LOG.info("Change occurred at line: " + (changedLine + 1));
            
            // Yorumların satır numaralarını güncelle
            LineCommentService.getInstance(project).updateLineNumbers(file, changedLine, lineDiff);
            
            // Editörü güncelle
            Editor editor = EditorFactory.getInstance().getEditors(document)[0];
            if (editor != null) {
                // Tüm highlighters'ları temizle
                MarkupModel markupModel = editor.getMarkupModel();
                markupModel.removeAllHighlighters();
                
                // Yorumları yeniden yükle ve göster
                List<LineComment> comments = LineCommentService.getInstance(project).getCommentsForFile(file);
                for (LineComment comment : comments) {
                    int lineNumber = comment.getLineNumber();
                    if (lineNumber >= 0 && lineNumber < document.getLineCount()) {
                        RangeHighlighter highlighter = markupModel.addLineHighlighter(
                            lineNumber,
                            HighlighterLayer.SELECTION - 1,
                            new TextAttributes(null, null, null, null, Font.PLAIN)
                        );
                        highlighter.setGutterIconRenderer(new LineCommentGutterIconRenderer(
                            lineNumber,
                            comment.getComment(),
                            comment.getUsername(),
                            comment.getTimestamp(),
                            file,
                            project
                        ));
                        LOG.info("Updated comment icon at line: " + (lineNumber + 1));
                    }
                }
            }
        }
    }
} 