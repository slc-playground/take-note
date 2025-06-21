package com.yuunus90.linecomment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class CommentPathUtil {

    private CommentPathUtil() {
        // Private constructor for utility class
    }

    /**
     * Gets the file path relative to the project's base directory.
     * This ensures that paths are portable across different machines.
     *
     * @param project The current project.
     * @param file    The file whose relative path is needed.
     * @return The project-relative path string, or the absolute path if relativity cannot be determined.
     */
    public static String getRelativePath(@NotNull Project project, @NotNull VirtualFile file) {
        String relativePath = VfsUtilCore.getRelativePath(file, project.getBaseDir());
        return relativePath != null ? relativePath : file.getPath();
    }
} 