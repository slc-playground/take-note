package com.example.linecomment;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LineCommentGutterIconRenderer extends GutterIconRenderer {
    private final int lineNumber;
    private final String comment;
    private final String username;
    private final long timestamp;
    private final VirtualFile file;
    private final Project project;
    private static final Icon COMMENT_ICON = IconLoader.getIcon("/icons/comment.svg", LineCommentGutterIconRenderer.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM EEE, HH:mm", new Locale("tr", "TR"));

    public LineCommentGutterIconRenderer(int lineNumber, String comment, String username, long timestamp, VirtualFile file, Project project) {
        this.lineNumber = lineNumber;
        this.comment = comment;
        this.username = username;
        this.timestamp = timestamp;
        this.file = file;
        this.project = project;
    }

    @Override
    public @NotNull Icon getIcon() {
        return COMMENT_ICON;
    }

    @Override
    public @Nullable String getTooltipText() {
        return comment;
    }

    @Override
    public @Nullable ActionGroup getPopupMenuActions() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Sil") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                LineCommentService.getInstance(project).deleteComment(file, lineNumber);
            }
        });
        return group;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LineCommentGutterIconRenderer that = (LineCommentGutterIconRenderer) obj;
        return lineNumber == that.lineNumber && file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return 31 * lineNumber + file.hashCode();
    }

    @Override
    public AnAction getClickAction() {
        return new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                JBPopupFactory factory = JBPopupFactory.getInstance();
                JBPopup popup = factory.createComponentPopupBuilder(createPopupContent(), null)
                        .setTitle("Yorum")
                        .setMovable(true)
                        .setResizable(true)
                        .setRequestFocus(true)
                        .createPopup();

                popup.addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        // Popup kapandığında yapılacak işlemler
                    }
                });

                popup.showInFocusCenter();
            }

            private JComponent createPopupContent() {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                
                // Yorum metni
                JTextArea textArea = new JTextArea(comment);
                textArea.setEditable(false);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setBackground(panel.getBackground());
                
                // Kullanıcı adı ve zaman bilgisi
                JLabel infoLabel = new JLabel(String.format("%s - %s", 
                    username, 
                    DATE_FORMAT.format(new Date(timestamp))));
                infoLabel.setForeground(Color.GRAY);
                
                // Butonlar için panel
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
                
                // Kapat butonu
                JButton closeButton = new JButton("Kapat");
                closeButton.addActionListener(e -> {
                    Component source = (Component) e.getSource();
                    Window window = SwingUtilities.getWindowAncestor(source);
                    if (window != null) {
                        window.dispose();
                    }
                });
                
                // Sil butonu
                JButton deleteButton = new JButton("Sil");
                deleteButton.addActionListener(e -> {
                    LineCommentService.getInstance(project).deleteComment(file, lineNumber);
                    Component source = (Component) e.getSource();
                    Window window = SwingUtilities.getWindowAncestor(source);
                    if (window != null) {
                        window.dispose();
                    }
                });
                
                buttonPanel.add(closeButton);
                buttonPanel.add(Box.createHorizontalStrut(10));
                buttonPanel.add(deleteButton);
                
                panel.add(textArea);
                panel.add(Box.createVerticalStrut(5));
                panel.add(infoLabel);
                panel.add(Box.createVerticalStrut(10));
                panel.add(buttonPanel);
                
                return panel;
            }
        };
    }
} 