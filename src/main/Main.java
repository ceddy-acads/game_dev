package main;

import javax.swing.*;
import java.awt.*;
import entities.GameLandingPage;
import entities.StoryScreen;
import entities.ActScreen;
import entities.GameOverScreen;
import java.awt.image.BufferedImage; // Import for BufferedImage

public class Main {
    private static JFrame window;
    private static CardLayout cardLayout;
    private static JPanel mainPanel;
    private static GameLoop gameLoop;
    private static GameLandingPage landingPage;
    private static StoryScreen storyScreen;
    private static ActScreen actScreen;
    private static GameOverScreen gameOverScreen;

    public static void main(String[] args) {
        window = new JFrame("Blade Quest");
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        landingPage = new GameLandingPage(Main::showStoryScreen);
        storyScreen = new StoryScreen(Main::showActScreen); // Story screen now leads to Act screen
        gameLoop = new GameLoop(Main::showGameOverScreenWithScreenshot); // Pass a callback for game over with screenshot
        gameOverScreen = new GameOverScreen(Main::resetGame); // Pass a callback for continue

        mainPanel.add(landingPage, "LANDING");
        mainPanel.add(storyScreen, "STORY");
        mainPanel.add(gameLoop, "GAME");
        mainPanel.add(gameOverScreen, "GAME_OVER");

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        window.add(mainPanel);

        // Add window resize listener for responsive layout
        window.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateResponsiveLayout();
            }
        });
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        cardLayout.show(mainPanel, "LANDING");
    }

    // Handle responsive layout updates when window is resized
    private static void updateResponsiveLayout() {
        // Get current window size
        Dimension size = window.getSize();
        int width = size.width;
        int height = size.height;

        // Update main panel size
        mainPanel.setPreferredSize(new Dimension(width, height));
        mainPanel.revalidate();

        // Notify game components of size change
        if (gameLoop != null) {
            gameLoop.updateWindowSize(width, height);
        }

        // Force repaint
        window.repaint();
    }

    // Public method to get current window size
    public static Dimension getWindowSize() {
        return window.getSize();
    }

    public static void showStoryScreen() {
        // Show story screen immediately without fade transition
        cardLayout.show(mainPanel, "STORY");
        storyScreen.requestFocusInWindow();
    }

    public static void showActScreen() {
        // Create new ActScreen instance for this transition
        actScreen = new ActScreen(Main::startActualGame);
        mainPanel.add(actScreen, "ACT");

        // Show Act screen immediately (ActScreen handles its own fade transitions)
        cardLayout.show(mainPanel, "ACT");
        actScreen.requestFocusInWindow();

        // Force repaint to ensure clean transition
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public static void startActualGame() {
        // Transition to game with fade effect
        new FadeTransition(window, FadeTransition.FadeType.FADE_OUT, () -> {
            cardLayout.show(mainPanel, "GAME");
            gameLoop.requestFocusInWindow();
            gameLoop.start();
            new FadeTransition(window, FadeTransition.FadeType.FADE_IN, null);
        });
    }

    // Modified callback to accept screenshot
    public static void showGameOverScreenWithScreenshot(BufferedImage screenshot) {
        gameOverScreen.setBackgroundImage(screenshot); // Set the screenshot as background
        new FadeTransition(window, FadeTransition.FadeType.FADE_OUT, () -> {
            cardLayout.show(mainPanel, "GAME_OVER");
            gameOverScreen.requestFocusInWindow();
            new FadeTransition(window, FadeTransition.FadeType.FADE_IN, null);
        });
    }

    public static void resetGame() {
        new FadeTransition(window, FadeTransition.FadeType.FADE_OUT, () -> {
            gameLoop.reset(); // Reset game state
            cardLayout.show(mainPanel, "GAME");
            gameLoop.requestFocusInWindow();
            gameLoop.start(); // Restart the game loop if needed, or ensure it continues
            new FadeTransition(window, FadeTransition.FadeType.FADE_IN, null);
        });
    }
}
