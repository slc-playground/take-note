package com.example.linecomment;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.List;

public class LineCommentEditorListener implements EditorFactoryListener, EditorMouseListener {
    private static final Logger LOG = Logger.getInstance(LineCommentEditorListener.class);
    private final Project project;

    public LineCommentEditorListener(Project project) {
        this.project = project;
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        
        if (file == null) {
            LOG.warn("File is null for editor");
            return;
        }

        LOG.info("Editor created for file: " + file.getPath());
        
        List<LineComment> comments = LineCommentService.getInstance(project).getCommentsForFile(file);
        LOG.info("Found " + comments.size() + " comments for file: " + file.getPath());

        for (LineComment comment : comments) {
            int lineNumber = comment.getLineNumber();
            if (lineNumber > 0 && lineNumber <= editor.getDocument().getLineCount()) {
                LOG.info("Adding comment for line " + lineNumber + ": " + comment.getComment());
                MarkupModel markupModel = editor.getMarkupModel();
                LineCommentGutterIconRenderer iconRenderer = new LineCommentGutterIconRenderer(
                        lineNumber,
                        comment.getComment(),
                        comment.getUsername(),
                        comment.getTimestamp(),
                        file,
                        project
                );
                RangeHighlighter highlighter = markupModel.addLineHighlighter(
                    lineNumber - 1,
                    HighlighterLayer.ADDITIONAL_SYNTAX,
                    new TextAttributes()
                );
                highlighter.setGutterIconRenderer(iconRenderer);
                LOG.info("Added highlighter for line " + lineNumber);
            } else {
                LOG.warn("Line number out of bounds: " + lineNumber);
            }
        }
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        LOG.info("Editor released");
    }

    @Override
    public void mouseClicked(@NotNull EditorMouseEvent e) {
        if (e.getMouseEvent().getButton() == java.awt.event.MouseEvent.BUTTON3) { // Sağ tık
            Editor editor = e.getEditor();
            VirtualFile file = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
                    .getFile(editor.getDocument());
            if (file == null) {
                LOG.warn("File is null for editor");
                return;
            }

            int lineNumber = editor.xyToLogicalPosition(e.getMouseEvent().getPoint()).line + 1;
            LOG.info("Right click detected at line: " + lineNumber);

            String comment = Messages.showInputDialog(
                    project,
                    "Yorumunuzu girin:",
                    "Yorum Ekle",
                    Messages.getQuestionIcon()
            );

            if (comment != null && !comment.trim().isEmpty()) {
                LineCommentService.getInstance(project).addComment(file, lineNumber, comment);
                updateComments(editor, file);
            }
        }
    }

    private void updateComments(Editor editor, VirtualFile file) {
        MarkupModel markupModel = editor.getMarkupModel();
        markupModel.removeAllHighlighters();

        List<LineComment> comments = LineCommentService.getInstance(project).getCommentsForFile(file);
        LOG.info("Found " + comments.size() + " comments for file: " + file.getPath());

        for (LineComment comment : comments) {
            int lineNumber = comment.getLineNumber();
            if (lineNumber > 0 && lineNumber <= editor.getDocument().getLineCount()) {
                LOG.info("Adding comment at line: " + lineNumber);
                LineCommentGutterIconRenderer iconRenderer = new LineCommentGutterIconRenderer(
                    lineNumber,
                    comment.getComment(),
                    comment.getUsername(),
                    comment.getTimestamp(),
                    file,
                    project
                );
                
                RangeHighlighter highlighter = markupModel.addLineHighlighter(
                    lineNumber - 1,
                    HighlighterLayer.ADDITIONAL_SYNTAX,
                    new TextAttributes()
                );
                highlighter.setGutterIconRenderer(iconRenderer);
            } else {
                LOG.warn("Line number out of bounds: " + lineNumber);
            }
        }
    }

    @Override
    public void mousePressed(@NotNull EditorMouseEvent e) {}

    @Override
    public void mouseReleased(@NotNull EditorMouseEvent e) {}

    @Override
    public void mouseEntered(@NotNull EditorMouseEvent e) {}

    @Override
    public void mouseExited(@NotNull EditorMouseEvent e) {}
} 