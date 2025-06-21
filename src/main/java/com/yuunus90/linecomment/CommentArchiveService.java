package com.yuunus90.linecomment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public final class CommentArchiveService {

    private final Project project;
    private static final String ARCHIVE_FILE_NAME = "archive.json";

    public CommentArchiveService(Project project) {
        this.project = project;
    }

    public static CommentArchiveService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CommentArchiveService.class);
    }

    public void archiveComment(ArchivedComment comment) {
        Path archivePath = getArchivePath();
        List<ArchivedComment> archivedComments = loadArchivedComments();
        archivedComments.add(comment);
        saveArchivedComments(archivedComments);
    }

    private List<ArchivedComment> loadArchivedComments() {
        Path archivePath = getArchivePath();
        if (!Files.exists(archivePath)) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(archivePath.toFile())) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<ArchivedComment>>() {}.getType();
            List<ArchivedComment> comments = gson.fromJson(reader, listType);
            return comments != null ? comments : new ArrayList<>();
        } catch (IOException e) {
            // Log or handle error appropriately
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveArchivedComments(List<ArchivedComment> comments) {
        Path archivePath = getArchivePath();
        try {
            Files.createDirectories(archivePath.getParent());
            try (FileWriter writer = new FileWriter(archivePath.toFile())) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(comments, writer);
            }
        } catch (IOException e) {
            // Log or handle error appropriately
            e.printStackTrace();
        }
    }

    private Path getArchivePath() {
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            // Handle case where project base path is not available
            // Maybe default to a temp directory or user home
            return Paths.get(System.getProperty("user.home"), ".idea_line_comments", ARCHIVE_FILE_NAME);
        }
        return Paths.get(projectBasePath, ".idea", ARCHIVE_FILE_NAME);
    }
} 