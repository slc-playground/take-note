package com.yuunus90.linecomment;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class LineCommentGutterIconRenderer extends GutterIconRenderer {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("tr", "TR"));
    private final LineComment lineComment;
    private final Editor editor;

    private LineCommentGutterIconRenderer(LineComment lineComment, Editor editor) {
        this.lineComment = lineComment;
        this.editor = editor;
    }

    @Override
    public @NotNull Icon getIcon() {
        return IconLoader.getIcon("/icons/comment.svg", LineCommentGutterIconRenderer.class);
    }

    @Override
    public String getTooltipText() {
        String formattedDate = DATE_FORMAT.format(new Date(lineComment.getCreationTimestamp()));
        return "<html><b>Comment:</b> " + lineComment.getComment() +
               "<br><b>Added:</b> " + formattedDate + "</html>";
    }

    @Nullable
    @Override
    public AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Project project = e.getProject();
                if (project == null) return;

                String formattedDate = DATE_FORMAT.format(new Date(lineComment.getCreationTimestamp()));

                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBorder(new EmptyBorder(10, 10, 10, 10));
                JLabel commentLabel = new JLabel("<html><b>Yorum:</b> " + lineComment.getComment() + "<br><b>Eklenme:</b> " + formattedDate + "</html>");
                commentLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                panel.add(commentLabel);
                panel.add(Box.createVerticalStrut(10));
                
                // Butonları yan yana yerleştirmek için ayrı panel
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
                buttonPanel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
                
                JButton deleteButton = new JButton("Yorumu Sil");
                JButton closeButton = new JButton("Kapat");
                
                buttonPanel.add(deleteButton);
                buttonPanel.add(Box.createHorizontalStrut(10)); // Butonlar arası boşluk
                buttonPanel.add(closeButton);
                
                panel.add(buttonPanel);

                JDialog dialog = new JDialog();
                dialog.setTitle("Yorum Detayı");
                dialog.setModal(true);
                dialog.setContentPane(panel);
                dialog.pack();
                
                // Popup'ı IDEA'nın ana penceresine göre konumlandır
                WindowManager windowManager = WindowManager.getInstance();
                if (windowManager != null) {
                    java.awt.Window ideWindow = windowManager.getFrame(project);
                    if (ideWindow != null) {
                        dialog.setLocationRelativeTo(ideWindow);
                    } else {
                        dialog.setLocationRelativeTo(null);
                    }
                } else {
                    dialog.setLocationRelativeTo(null);
                }

                deleteButton.addActionListener(evt -> {
                    int result = JOptionPane.showConfirmDialog(dialog, "Bu yorumu silmek istediğinize emin misiniz?", "Yorumu Sil", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        // Yorum silinsin
                        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
                        if (file != null) {
                            String relativePath = CommentPathUtil.getRelativePath(project, file);
                            LineCommentService service = LineCommentService.getInstance(project);
                            service.removeComment(relativePath, lineComment.getLineNumber());
                            redrawAllIconsForEditor(project, editor, relativePath);
                        }
                        dialog.dispose();
                    }
                });

                closeButton.addActionListener(evt -> dialog.dispose());

                dialog.setVisible(true);
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LineCommentGutterIconRenderer that = (LineCommentGutterIconRenderer) obj;
        return lineComment.getLineNumber() == that.lineComment.getLineNumber() &&
               Objects.equals(lineComment.getComment(), that.lineComment.getComment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineComment.getComment(), lineComment.getLineNumber());
    }

    public static void addGutterIcon(Editor editor, LineComment comment) {
        LineCommentGutterIconRenderer renderer = new LineCommentGutterIconRenderer(comment, editor);
        final RangeHighlighter highlighter = editor.getMarkupModel().addLineHighlighter(comment.getLineNumber(), HighlighterLayer.ERROR, null);
        highlighter.setGutterIconRenderer(renderer);
    }

    private static boolean hasGutterIcon(Editor editor, int lineNumber) {
        for (var highlighter : editor.getMarkupModel().getAllHighlighters()) {
            if (highlighter.getGutterIconRenderer() instanceof LineCommentGutterIconRenderer) {
                LineCommentGutterIconRenderer renderer = (LineCommentGutterIconRenderer) highlighter.getGutterIconRenderer();
                if (renderer.lineComment.getLineNumber() == lineNumber) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void redrawAllIconsForEditor(Project project, Editor editor, String filePath) {
        editor.getMarkupModel().removeAllHighlighters();

        LineCommentService service = LineCommentService.getInstance(project);
        Map<Integer, LineComment> comments = service.getCommentsForFile(filePath);
        if (comments != null) {
            comments.forEach((line, comment) -> {
                if (!hasGutterIcon(editor, line)) {
                    addGutterIcon(editor, comment);
                }
            });
        }
    }
} 