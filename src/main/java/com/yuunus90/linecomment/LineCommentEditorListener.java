package com.yuunus90.linecomment;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class LineCommentEditorListener implements EditorFactoryListener {
    private static final Key<PerEditorListener> PER_EDITOR_LISTENER_KEY = Key.create("line.comment.per.editor.listener");
    private final Project project;

    public record DeletionCandidate(LineComment comment, String lineContent) {}


    public LineCommentEditorListener(Project project) {
        this.project = project;
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        if (editor.getProject() != project) {
            return;
        }

        PerEditorListener listener = new PerEditorListener(project, editor);
        editor.putUserData(PER_EDITOR_LISTENER_KEY, listener);
        editor.getDocument().addDocumentListener(listener);
        editor.addEditorMouseListener(listener);
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        if (editor.getProject() != project) {
            return;
        }

        PerEditorListener listener = editor.getUserData(PER_EDITOR_LISTENER_KEY);
        if (listener != null) {
            editor.getDocument().removeDocumentListener(listener);
            editor.removeEditorMouseListener(listener);
            editor.putUserData(PER_EDITOR_LISTENER_KEY, null);
        }
    }

    private static class PerEditorListener implements DocumentListener, EditorMouseListener {
        private final Project project;
        private final Editor editor;
        private final LineCommentService lineCommentService;
        private int linesToDelete = 0; // Temp storage for lines being deleted

        public PerEditorListener(Project project, Editor editor) {
            this.project = project;
            this.editor = editor;
            this.lineCommentService = LineCommentService.getInstance(project);
        }

        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent event) {
            this.linesToDelete = 0; // Reset on each new event
            if (event.getNewLength() < event.getOldLength()) { // Deletion or replacement
                Document document = event.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                if (file == null) return;
                String relativePath = CommentPathUtil.getRelativePath(project, file);

                // Calculate how many lines are in the fragment being deleted
                this.linesToDelete = (int) event.getOldFragment().toString().chars().filter(ch -> ch == '\n').count();

                int startLine = document.getLineNumber(event.getOffset());
                int endLine = document.getLineNumber(event.getOffset() + event.getOldLength());

                for (int i = startLine; i <= endLine; i++) {
                    if (lineCommentService.hasComment(relativePath, i)) {
                        LineComment comment = lineCommentService.getComment(relativePath, i);
                        int lineStartOffset = document.getLineStartOffset(i);
                        int lineEndOffset = document.getLineEndOffset(i);
                        String lineContent = document.getText().substring(lineStartOffset, lineEndOffset);
                        lineCommentService.addPendingDeletedComment(file, new DeletionCandidate(comment, lineContent));
                    }
                }
            }
        }

        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            Document document = event.getDocument();
            VirtualFile file = FileDocumentManager.getInstance().getFile(document);
            if (file == null) {
                return;
            }
            String relativePath = CommentPathUtil.getRelativePath(project, file);

            int startLine = document.getLineNumber(event.getOffset());
            int newLines = (int) event.getNewFragment().toString().chars().filter(ch -> ch == '\n').count();
            int lineDelta = newLines - this.linesToDelete;

            if (lineDelta != 0) {
                lineCommentService.updateLineNumbers(relativePath, startLine, lineDelta);
                redrawAllIcons();
            }
        }

        private void redrawAllIcons() {
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (file != null) {
                String relativePath = CommentPathUtil.getRelativePath(project, file);
                LineCommentGutterIconRenderer.redrawAllIconsForEditor(project, editor, relativePath);
            }
        }

        // mouseClicked is no longer needed here as the GutterIconRenderer's getClickAction handles it.
    }
} 