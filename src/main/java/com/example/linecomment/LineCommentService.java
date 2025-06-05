package com.example.linecomment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
public final class LineCommentService {
    private final Map<String, List<LineComment>> comments;
    private final Gson gson;
    private static final String NOTES_FILE = "notes.json";
    private static final Logger LOG = Logger.getInstance(LineCommentService.class);
    private final Project project;

    public LineCommentService(Project project) {
        this.project = project;
        this.comments = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadComments();
    }

    public static LineCommentService getInstance(@NotNull Project project) {
        return project.getService(LineCommentService.class);
    }

    public void addComment(@NotNull VirtualFile file, int lineNumber, @NotNull String comment) {
        String relativePath = getRelativePath(file);
        String fileName = file.getName();
        String username = getUsername();
        
        LOG.info("Adding comment for file: " + relativePath + ", line: " + lineNumber + ", user: " + username);
        
        List<LineComment> fileComments = comments.computeIfAbsent(relativePath, k -> new ArrayList<>());
        fileComments.add(new LineComment(relativePath, fileName, lineNumber, comment, username));
        
        // Yorumları kaydet
        saveComments();
        LOG.info("Comment saved to notes.json");
    }

    private String getRelativePath(@NotNull VirtualFile file) {
        String projectPath = project.getBasePath();
        String filePath = file.getPath();
        return filePath.substring(projectPath.length() + 1);
    }

    private String getUsername() {
        return System.getProperty("user.name");
    }

    public void deleteComment(@NotNull VirtualFile file, int lineNumber) {
        String relativePath = getRelativePath(file);
        List<LineComment> fileComments = comments.get(relativePath);
        if (fileComments != null) {
            fileComments.removeIf(comment -> comment.getLineNumber() == lineNumber);
            if (fileComments.isEmpty()) {
                comments.remove(relativePath);
            }
            saveComments();

            // İlgili editörü bul ve ikonu kaldır
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null && editor.getDocument().equals(FileDocumentManager.getInstance().getDocument(file))) {
                MarkupModel markupModel = editor.getMarkupModel();
                markupModel.removeAllHighlighters();
                
                // Kalan yorumların ikonlarını yeniden ekle
                for (LineComment comment : fileComments) {
                    int commentLine = comment.getLineNumber();
                    if (commentLine > 0 && commentLine <= editor.getDocument().getLineCount()) {
                        LineCommentGutterIconRenderer iconRenderer = new LineCommentGutterIconRenderer(
                            commentLine,
                            comment.getComment(),
                            comment.getUsername(),
                            comment.getTimestamp(),
                            file,
                            project
                        );
                        
                        markupModel.addLineHighlighter(
                            commentLine - 1,
                            HighlighterLayer.ADDITIONAL_SYNTAX,
                            new TextAttributes()
                        ).setGutterIconRenderer(iconRenderer);
                    }
                }
            }
        }
    }

    public List<LineComment> getCommentsForFile(@NotNull VirtualFile file) {
        String relativePath = getRelativePath(file);
        String fileName = file.getName();
        
        LOG.info("Getting comments for file: " + relativePath);
        
        // Önce göreceli dosya yolu ile dene
        List<LineComment> fileComments = comments.get(relativePath);
        
        // Eğer bulunamazsa, dosya adı ile eşleşen yorumları ara
        if (fileComments == null) {
            LOG.info("No comments found for exact path, searching by filename: " + fileName);
            for (Map.Entry<String, List<LineComment>> entry : comments.entrySet()) {
                List<LineComment> existingComments = entry.getValue();
                if (!existingComments.isEmpty() && existingComments.get(0).getFileName().equals(fileName)) {
                    LOG.info("Found comments by filename, moving to new path");
                    // Eski yorumları yeni dosya yoluna taşı
                    fileComments = existingComments;
                    comments.remove(entry.getKey());
                    comments.put(relativePath, fileComments);
                    saveComments();
                    break;
                }
            }
        }
        
        if (fileComments != null) {
            LOG.info("Found " + fileComments.size() + " comments");
        } else {
            LOG.info("No comments found");
        }
        
        return fileComments != null ? fileComments : new ArrayList<>();
    }

    public void removeComments(String relativePath, List<LineComment> commentsToRemove) {
        comments.computeIfPresent(relativePath, (k, v) -> {
            v.removeAll(commentsToRemove);
            return v;
        });
        saveComments();
    }

    public void updateComments(@NotNull VirtualFile file, @NotNull List<LineComment> newComments) {
        String relativePath = getRelativePath(file);
        LOG.info("Updating comments for file: " + relativePath + ", new comment count: " + newComments.size());
        
        if (newComments.isEmpty()) {
            comments.remove(relativePath);
        } else {
            comments.put(relativePath, newComments);
        }
        saveComments();
    }

    public void updateLineNumbers(@NotNull VirtualFile file, int changedLine, int lineDiff) {
        String relativePath = getRelativePath(file);
        List<LineComment> fileComments = comments.get(relativePath);
        
        if (fileComments != null) {
            boolean updated = false;
            LOG.info("Updating line numbers for file: " + relativePath);
            LOG.info("Change occurred at line: " + (changedLine + 1) + ", line difference: " + lineDiff);
            
            // Tüm yorumları güncelle
            for (LineComment comment : fileComments) {
                int oldLineNumber = comment.getLineNumber();
                // Eğer yorum, değişikliğin yapıldığı satırdan sonraki bir satırdaysa
                if (oldLineNumber > changedLine) {
                    int newLineNumber = oldLineNumber + lineDiff;
                    comment.setLineNumber(newLineNumber);
                    updated = true;
                    LOG.info("Updated comment line number: " + oldLineNumber + " -> " + newLineNumber);
                }
            }
            
            if (updated) {
                LOG.info("Saving updated comments to notes.json");
                saveComments();
            }
        }
    }

    private void saveComments() {
        File notesDir = new File(project.getBasePath(), ".notes");
        if (!notesDir.exists()) {
            notesDir.mkdirs();
        }

        File notesFile = new File(notesDir, NOTES_FILE);
        try (Writer writer = new FileWriter(notesFile)) {
            gson.toJson(comments, writer);
            LOG.info("Saved comments to: " + notesFile.getPath());
        } catch (IOException e) {
            LOG.error("Error saving comments", e);
        }
    }

    private void loadComments() {
        File notesDir = new File(project.getBasePath(), ".notes");
        File notesFile = new File(notesDir, NOTES_FILE);
        
        if (notesFile.exists()) {
            try (Reader reader = new FileReader(notesFile)) {
                Type type = new TypeToken<Map<String, List<LineComment>>>(){}.getType();
                Map<String, List<LineComment>> loadedComments = gson.fromJson(reader, type);
                if (loadedComments != null) {
                    comments.clear();
                    comments.putAll(loadedComments);
                    LOG.info("Loaded " + comments.size() + " files with comments from: " + notesFile.getPath());
                }
            } catch (IOException e) {
                LOG.error("Error loading comments", e);
            }
        } else {
            LOG.info("No comments file found at: " + notesFile.getPath());
        }
    }
} 