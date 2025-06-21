package com.yuunus90.linecomment;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class LineCommentFileEditorListener implements FileEditorManagerListener {
    private final Project project;

    public LineCommentFileEditorListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        LineCommentService service = LineCommentService.getInstance(project);
        String relativePath = CommentPathUtil.getRelativePath(project, file);
        Map<Integer, LineComment> comments = service.getCommentsForFile(relativePath);

        if (comments.isEmpty()) {
            return;
        }

        FileEditor[] allEditors = source.getAllEditors(file);
        for (FileEditor fileEditor : allEditors) {
            if (fileEditor instanceof TextEditor) {
                Editor editor = ((TextEditor) fileEditor).getEditor();
                comments.forEach((line, comment) -> {
                    LineCommentGutterIconRenderer.addGutterIcon(editor, comment);
                });
            }
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        LineCommentService service = LineCommentService.getInstance(project);
        List<LineCommentEditorListener.DeletionCandidate> pendingDeletions = service.getAndClearPendingDeletedComments(file);

        if (pendingDeletions != null && !pendingDeletions.isEmpty()) {
            String message = String.format("%d adet silinmiş yorum bulundu. Bu yorumları arşivlemek ister misiniz?", pendingDeletions.size());
            int result = Messages.showYesNoDialog(
                    project,
                    message,
                    "Silinen Yorumlar",
                    "Arşivle",
                    "Kalıcı Olarak Sil",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) { // Archive
                CommentArchiveService archiveService = CommentArchiveService.getInstance(project);
                for (LineCommentEditorListener.DeletionCandidate deleted : pendingDeletions) {
                    ArchivedComment archivedComment = new ArchivedComment(
                            CommentPathUtil.getRelativePath(project, file),
                            deleted.comment().getLineNumber(),
                            deleted.comment().getComment(),
                            deleted.lineContent()
                    );
                    archiveService.archiveComment(archivedComment);
                }
            }
        }
    }
} 