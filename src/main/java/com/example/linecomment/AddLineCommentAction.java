package com.example.linecomment;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

// hello world

public class AddLineCommentAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (editor == null) return;

        VirtualFile file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);
        if (file == null) return;

        int lineNumber = editor.getCaretModel().getLogicalPosition().line + 1;

        String comment = Messages.showInputDialog(
                project,
                "Yorumunuzu girin:",
                "Yorum Ekle",
                Messages.getQuestionIcon()
        );

        if (comment != null && !comment.trim().isEmpty()) {
            LineCommentService.getInstance(project).addComment(file, lineNumber, comment);

            MarkupModel markupModel = editor.getMarkupModel();
            LineCommentGutterIconRenderer iconRenderer = new LineCommentGutterIconRenderer(
                    lineNumber,
                    comment,
                    System.getProperty("user.name"),
                    System.currentTimeMillis(),
                    file,
                    project
            );
            
            RangeHighlighter highlighter = markupModel.addLineHighlighter(
                lineNumber - 1,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                new TextAttributes()
            );
            highlighter.setGutterIconRenderer(iconRenderer);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(project != null && editor != null);
    }
} 