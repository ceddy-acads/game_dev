package entities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class DialogueUI extends JPanel {

    private List<String> dialogueLines = new ArrayList<>();
    private int currentLineIndex = 0;
    private JTextArea dialogueTextArea;
    private JButton continueButton;
    private JButton closeButton;
    private boolean isVisible = false;
    private Object keyHandler; // Reference to KeyHandler for clearing movement keys

    // Colors matching the game's theme
    private final Color PARCHMENT = new Color(217,195,154);
    private final Color PARCHMENT_DARK = new Color(200,175,130);
    private final Color TEXT_BROWN = new Color(59,47,35);
    private final Color PANEL_BORDER = new Color(91,74,48);
    private final Color BUTTON_BROWN = new Color(152,117,78);

    public DialogueUI(int screenWidth, int screenHeight) {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(new Color(0, 0, 0, 255)); // Fully opaque black background
        setLayout(new BorderLayout());

        // Main dialogue panel - positioned at semi-bottom
        JPanel dialoguePanel = new JPanel();
        dialoguePanel.setLayout(new BorderLayout());
        dialoguePanel.setBackground(PARCHMENT);
        dialoguePanel.setBorder(BorderFactory.createLineBorder(PANEL_BORDER, 3));
        dialoguePanel.setPreferredSize(new Dimension(600, 200)); // Fixed size for dialogue panel

        // Dialogue text area
        dialogueTextArea = new JTextArea();
        dialogueTextArea.setEditable(false);
        dialogueTextArea.setLineWrap(true);
        dialogueTextArea.setWrapStyleWord(true);
        dialogueTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
        dialogueTextArea.setBackground(PARCHMENT);
        dialogueTextArea.setForeground(TEXT_BROWN);
        dialogueTextArea.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(dialogueTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(PARCHMENT);

        dialoguePanel.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(PARCHMENT);

        continueButton = new JButton("Continue (Space)");
        styleButton(continueButton, BUTTON_BROWN);
        continueButton.addActionListener(e -> nextLine());

        closeButton = new JButton("Close (ESC)");
        styleButton(closeButton, new Color(168,92,61));
        closeButton.addActionListener(e -> closeDialogue());

        buttonPanel.add(continueButton);
        buttonPanel.add(closeButton);

        dialoguePanel.add(buttonPanel, BorderLayout.SOUTH);

        // Position the dialogue panel at the semi-bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(dialoguePanel, BorderLayout.SOUTH);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0)); // 50px from bottom

        add(bottomPanel, BorderLayout.SOUTH);

        // Initially hidden
        setVisible(false);
    }

    private void styleButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(PANEL_BORDER, 2));
        button.setFont(new Font("Arial", Font.BOLD, 12));
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
            dialogueTextArea.setText(dialogueLines.get(currentLineIndex));
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
        dialogueTextArea.setText("");
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
}
