package com.example.linecomment;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.PROJECT)
public final class CommentArchiveService {
    private final Project project;
    private final List<ArchivedComment> archivedComments;
    private static final String ARCHIVE_DIR = ".notes/archive";

    public CommentArchiveService(Project project) {
        this.project = project;
        this.archivedComments = new ArrayList<>();
        loadArchivedComments();
    }

    public static CommentArchiveService getInstance(@NotNull Project project) {
        return project.getService(CommentArchiveService.class);
    }

    public void archiveComment(String filePath, int lineNumber, String comment, String codeLine) {
        ArchivedComment archivedComment = new ArchivedComment(filePath, lineNumber, comment, codeLine);
        archivedComments.add(archivedComment);
        saveArchivedComments();
    }

    public List<ArchivedComment> getArchivedComments() {
        return new ArrayList<>(archivedComments);
    }

    public List<ArchivedComment> getArchivedCommentsForFile(String filePath) {
        return archivedComments.stream()
                .filter(c -> c.getFilePath().equals(filePath))
                .toList();
    }

    private void saveArchivedComments() {
        try {
            Path archiveDir = Paths.get(project.getBasePath(), ARCHIVE_DIR);
            if (!Files.exists(archiveDir)) {
                Files.createDirectories(archiveDir);
            }

            Path archiveFile = archiveDir.resolve("archived_comments.dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(archiveFile.toFile()))) {
                oos.writeObject(new ArrayList<>(archivedComments));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadArchivedComments() {
        try {
            Path archiveFile = Paths.get(project.getBasePath(), ARCHIVE_DIR, "archived_comments.dat");
            if (Files.exists(archiveFile)) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(archiveFile.toFile()))) {
                    List<ArchivedComment> loadedComments = (List<ArchivedComment>) ois.readObject();
                    archivedComments.addAll(loadedComments);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
} 