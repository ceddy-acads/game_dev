package entities;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.GridBagConstraints;

public class DialogueUI extends JPanel {

    private List<String> dialogueLines = new ArrayList<>();
    private int currentLineIndex = 0;
    private JTextPane dialogueTextPane;
    private JButton continueButton;
    private JButton closeButton;
    private boolean isVisible = false;
    private Object keyHandler; // Reference to KeyHandler for clearing movement keys

    // Colors matching the game's theme - updated for black background
    private final Color BACKGROUND_BLACK = new Color(0, 0, 0);
    private final Color PARCHMENT_DARK = new Color(200,175,130);
    private final Color TEXT_WHITE = new Color(255, 255, 255);
    private final Color PANEL_BORDER = new Color(91,74,48);
    private final Color BUTTON_BROWN = new Color(152,117,78);

    public DialogueUI(int screenWidth, int screenHeight) {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setOpaque(false); // Make the overlay transparent
        setLayout(new GridBagLayout());

        // Centered dialogue panel
        JPanel dialoguePanel = new JPanel();
        dialoguePanel.setLayout(new BorderLayout());
        dialoguePanel.setBackground(BACKGROUND_BLACK);
        dialoguePanel.setBorder(BorderFactory.createLineBorder(PANEL_BORDER, 3));
        dialoguePanel.setOpaque(true);
        // Set preferred size for centered dialog box
        dialoguePanel.setPreferredSize(new Dimension((int)(screenWidth * 0.8), (int)(screenHeight * 0.8)));

        // Dialogue text pane for centered dialog box
        JTextPane dialogueTextPane = new JTextPane();
        dialogueTextPane.setEditable(false);
        dialogueTextPane.setFont(new Font("Georgia", Font.PLAIN, 24));
        dialogueTextPane.setBackground(BACKGROUND_BLACK);
        dialogueTextPane.setForeground(TEXT_WHITE);
        dialogueTextPane.setBorder(BorderFactory.createEmptyBorder(80, 40, 80, 40));
        // Set preferred size for centered dialog
        dialogueTextPane.setPreferredSize(new Dimension((int)(screenWidth * 0.8 - 80), (int)(screenHeight * 0.8 - 200)));

        // Center the text horizontally
        StyledDocument doc = dialogueTextPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(dialogueTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(BACKGROUND_BLACK);

        dialoguePanel.add(scrollPane, BorderLayout.CENTER);

        // Store reference for later use
        this.dialogueTextPane = dialogueTextPane;

        // Button panel for centered dialog
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20)); // More spacing
        buttonPanel.setBackground(BACKGROUND_BLACK);

        continueButton = new JButton("Continue (Space)");
        styleButtonFullscreen(continueButton, BUTTON_BROWN);
        continueButton.addActionListener(e -> nextLine());

        closeButton = new JButton("Close (ESC)");
        styleButtonFullscreen(closeButton, new Color(168,92,61));
        closeButton.addActionListener(e -> closeDialogue());

        buttonPanel.add(continueButton);
        buttonPanel.add(closeButton);

        dialoguePanel.add(buttonPanel, BorderLayout.SOUTH);

        // Center the dialogue panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(dialoguePanel, gbc);

        // Initially hidden
        setVisible(false);
    }

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(PANEL_BORDER, 2));
        button.setFont(new Font("Arial", Font.BOLD, 14));
    }

    private void styleButtonFullscreen(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(PANEL_BORDER, 3)); // Thicker border
        button.setFont(new Font("Arial", Font.BOLD, 18)); // Larger font for full screen
        button.setPreferredSize(new Dimension(180, 50)); // Fixed larger size
        button.setMinimumSize(new Dimension(180, 50));
        button.setMaximumSize(new Dimension(180, 50));
    }

    public void startDialogue(String npcName, List<String> lines) {
        this.dialogueLines = new ArrayList<>(lines);
        this.currentLineIndex = 0;
        this.isVisible = true;

        updateText();
        setVisible(true);
        requestFocusInWindow();

        // Clear movement keys to prevent auto-movement after dialogue ends
        clearMovementKeys();
    }

    private void clearMovementKeys() {
        if (keyHandler != null) {
            // Clear all movement key states to prevent auto-movement after dialogue
            try {
                // Use reflection to access KeyHandler fields
                java.lang.reflect.Field upPressedField = keyHandler.getClass().getField("upPressed");
                java.lang.reflect.Field downPressedField = keyHandler.getClass().getField("downPressed");
                java.lang.reflect.Field leftPressedField = keyHandler.getClass().getField("leftPressed");
                java.lang.reflect.Field rightPressedField = keyHandler.getClass().getField("rightPressed");

                upPressedField.setBoolean(keyHandler, false);
                downPressedField.setBoolean(keyHandler, false);
                leftPressedField.setBoolean(keyHandler, false);
                rightPressedField.setBoolean(keyHandler, false);
            } catch (Exception e) {
                System.err.println("Failed to clear movement keys: " + e.getMessage());
            }
        }
    }

    public void setKeyHandler(Object keyHandler) {
        this.keyHandler = keyHandler;
    }

    private void updateText() {
        if (currentLineIndex < dialogueLines.size()) {
            dialogueTextPane.setText(dialogueLines.get(currentLineIndex));
            continueButton.setText(currentLineIndex < dialogueLines.size() - 1 ? "Continue (Space)" : "Finish (Space)");
        } else {
            closeDialogue();
        }
    }

    private void nextLine() {
        currentLineIndex++;
        if (currentLineIndex < dialogueLines.size()) {
            updateText();
        } else {
            closeDialogue();
        }
    }

    private void closeDialogue() {
        this.isVisible = false;
        setVisible(false);
        dialogueLines.clear();
        currentLineIndex = 0;
        dialogueTextPane.setText("");
    }

    public boolean isDialogueVisible() {
        return isVisible;
    }

    // Handle key presses
    public void handleKeyPress(int keyCode) {
        if (!isVisible) return;

        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                nextLine();
                break;
            case KeyEvent.VK_ESCAPE:
                closeDialogue();
                break;
        }
    }

    // Quick dialogue starter for simple conversations
    public void showSimpleDialogue(String text) {
        List<String> lines = new ArrayList<>();
        lines.add(text);
        startDialogue("", lines);
    }

    // Update size for responsive layout
    public void updateSize(int newWidth, int newHeight) {
        setPreferredSize(new Dimension(newWidth, newHeight));
        revalidate();
        repaint();
    }
}
