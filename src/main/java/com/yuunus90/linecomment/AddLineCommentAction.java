package com.yuunus90.linecomment;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AddLineCommentAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || editor == null || file == null) {
            return;
        }

        String relativePath = CommentPathUtil.getRelativePath(project, file);
        int lineNumber = editor.getCaretModel().getLogicalPosition().line;

        LineCommentService service = LineCommentService.getInstance(project);

        if (service.hasComment(relativePath, lineNumber)) {
            String existingComment = service.getComment(relativePath, lineNumber).getComment();
            String newComment = Messages.showInputDialog(
                    project,
                    "Edit comment for line " + (lineNumber + 1),
                    "Edit Line Comment",
                    Messages.getQuestionIcon(),
                    existingComment,
                    null
            );

            if (newComment != null && !newComment.isEmpty()) {
                service.updateComment(relativePath, lineNumber, newComment);
            }
        } else {
            String comment = Messages.showInputDialog(project, "Enter comment for line " + (lineNumber + 1), "Add Line Comment", Messages.getQuestionIcon());

            if (comment != null && !comment.isEmpty()) {
                service.addComment(relativePath, lineNumber, comment);
            }
        }
        // Always redraw all icons for the current editor to reflect changes.
        LineCommentGutterIconRenderer.redrawAllIconsForEditor(project, editor, relativePath);
    }
} 