package com.example.linecomment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.editor.EditorFactory;
import org.jetbrains.annotations.NotNull;

public class LineCommentStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        EditorFactory.getInstance().addEditorFactoryListener(new LineCommentEditorListener(project), project);
    }
} 