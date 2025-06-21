package com.yuunus90.linecomment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
public final class LineCommentService {

    private static final Logger LOG = Logger.getInstance(LineCommentService.class);

    private final Project project;
    private final Path notesFilePath;
    private final Map<VirtualFile, List<LineCommentEditorListener.DeletionCandidate>> pendingDeletions = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, LineComment>> comments = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static LineCommentService getInstance(@NotNull Project project) {
        return project.getService(LineCommentService.class);
    }

    public LineCommentService(Project project) {
        this.project = project;
        this.notesFilePath = Paths.get(project.getBasePath(), ".notes", "notes.json");
        loadComments();
    }

    private void loadComments() {
        if (Files.exists(notesFilePath)) {
            try {
                String json = Files.readString(notesFilePath);
                Type type = new TypeToken<ConcurrentHashMap<String, Map<Integer, LineComment>>>() {}.getType();
                Map<String, Map<Integer, LineComment>> loadedComments = gson.fromJson(json, type);
                if (loadedComments != null) {
                    comments.clear();
                    comments.putAll(loadedComments);
                }
            } catch (IOException e) {
                LOG.warn("Could not load line comments from " + notesFilePath, e);
            }
        }
    }

    private void saveComments() {
        try {
            Files.createDirectories(notesFilePath.getParent());
            String json = gson.toJson(comments);
            Files.writeString(notesFilePath, json);
        } catch (IOException e) {
            LOG.warn("Could not save line comments to " + notesFilePath, e);
        }
    }

    public void addComment(String filePath, int lineNumber, String comment) {
        Map<Integer, LineComment> fileComments = comments.computeIfAbsent(filePath, k -> new ConcurrentHashMap<>());
        fileComments.put(lineNumber, new LineComment(filePath, lineNumber, comment));
        saveComments();
    }

    public void removeComment(String filePath, int lineNumber) {
        Map<Integer, LineComment> fileComments = comments.get(filePath);
        if (fileComments != null) {
            fileComments.remove(lineNumber);
            if (fileComments.isEmpty()) {
                comments.remove(filePath);
            }
            saveComments();
        }
    }

    public void updateComment(String filePath, int lineNumber, String newComment) {
        Map<Integer, LineComment> fileComments = comments.get(filePath);
        if (fileComments != null && fileComments.containsKey(lineNumber)) {
            fileComments.get(lineNumber).setComment(newComment);
            saveComments();
        }
    }

    public boolean hasComment(String filePath, int lineNumber) {
        Map<Integer, LineComment> fileComments = comments.get(filePath);
        return fileComments != null && fileComments.containsKey(lineNumber);
    }

    public LineComment getComment(String filePath, int lineNumber) {
        Map<Integer, LineComment> fileComments = comments.get(filePath);
        return (fileComments != null) ? fileComments.get(lineNumber) : null;
    }

    public Map<Integer, LineComment> getCommentsForFile(String filePath) {
        return comments.getOrDefault(filePath, new HashMap<>());
    }

    public Map<String, Map<Integer, LineComment>> getAllCommentsByFile() {
        return new HashMap<>(comments);
    }

    public void updateLineNumbers(String filePath, int startLine, int lineDelta) {
        Map<Integer, LineComment> originalFileComments = comments.get(filePath);

        if (originalFileComments == null || originalFileComments.isEmpty()) {
            return;
        }

        Map<Integer, LineComment> newFileComments = new ConcurrentHashMap<>();

        // Iterate over a copy of values to avoid concurrent modification issues on the map itself
        for (LineComment comment : new ArrayList<>(originalFileComments.values())) {
            int currentLine = comment.getLineNumber();
            int newLine = currentLine; // Assume no change by default

            if (lineDelta > 0) { // Insertion
                if (currentLine > startLine) {
                    newLine = currentLine + lineDelta;
                }
            } else { // Deletion
                int numDeleted = -lineDelta;
                int firstDeletedLine = startLine + 1;
                int lastDeletedLine = startLine + numDeleted;

                if (currentLine >= firstDeletedLine && currentLine <= lastDeletedLine) {
                    // This comment is within the deleted block of lines, so we effectively remove it
                    // by not adding it to the new map.
                    continue;
                } else if (currentLine > lastDeletedLine) {
                    // This comment was after the deleted block, so it gets shifted up.
                    newLine = currentLine + lineDelta;
                }
            }

            comment.setLineNumber(newLine);
            newFileComments.put(newLine, comment);
        }

        comments.put(filePath, newFileComments);
        saveComments();
    }

    public void addPendingDeletedComment(VirtualFile file, LineCommentEditorListener.DeletionCandidate candidate) {
        pendingDeletions.computeIfAbsent(file, k -> new ArrayList<>()).add(candidate);
    }

    public List<LineCommentEditorListener.DeletionCandidate> getAndClearPendingDeletedComments(VirtualFile file) {
        return pendingDeletions.remove(file);
    }
} 