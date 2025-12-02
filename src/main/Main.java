package main;

import javax.swing.*;
import java.awt.*;
import entities.GameLandingPage;
import entities.StoryScreen;
import entities.ActScreen;
import javax.sound.sampled.*;
import java.io.File;
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
    private static Clip backgroundMusic;
    private static boolean isFullscreen = false;

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

        // Start background music after window is visible
        playBackgroundMusic("src/assets/bgm.wav");
        cardLayout.show(mainPanel, "LANDING");
    }

    // Method to play background music
   private static void playBackgroundMusic(String filepath) {
    try {
        File musicPath = new File(filepath);
        System.out.println("Attempting to load music from: " + musicPath.getAbsolutePath());
        System.out.println("File exists: " + musicPath.exists());
        
        if (musicPath.exists()) {
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInput);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
            System.out.println("Background music started successfully!");
        } else {
            System.out.println("Music file not found: " + filepath);
        }
    } catch (Exception e) {
        System.out.println("Error playing background music:");
        e.printStackTrace();
    }
}
    // Method to stop background music
    public static void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    // Method to restart background music
    public static void restartBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.setFramePosition(0);
            backgroundMusic.start();
        }
    }

    // Method to set volume (0.0 to 1.0)
    public static void setMusicVolume(float volume) {
        if (backgroundMusic != null) {
            try {
                FloatControl volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                volumeControl.setValue(dB);
            } catch (Exception e) {
                System.out.println("Unable to set volume");
            }
        }
    }

    // Method to play a one-shot sound effect
    public static void playSoundEffect(String filepath) {
        try {
            // Convert path to resource path
            String resourcePath = "/" + filepath.replace("src/", "");
            java.io.InputStream audioStream = Main.class.getResourceAsStream(resourcePath);
            if (audioStream != null) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(audioStream);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } else {
                System.out.println("Sound effect file not found: " + filepath);
            }
        } catch (Exception e) {
            System.out.println("Error playing sound effect:");
            e.printStackTrace();
        }
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
        // Add fade out transition before showing story screen
        new FadeTransition(window, FadeTransition.FadeType.FADE_OUT, () -> {
            cardLayout.show(mainPanel, "STORY");
            storyScreen.requestFocusInWindow();
            stopBackgroundMusic(); // Stop music after the landing page
            new FadeTransition(window, FadeTransition.FadeType.FADE_IN, null);
        });
    }

    public static void showActScreen() {
        // Create new ActScreen instance for this transition
        actScreen = new ActScreen(Main::startActualGame);
        mainPanel.add(actScreen, "ACT");

        // Fade out music from story screen
        if (storyScreen != null) {
            storyScreen.fadeOutMusic();
        }

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
        stopBackgroundMusic(); // Stop any playing background music
        playSoundEffect("src/assets/audio/game_over_bad_chest.wav"); // Play game over sound
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

    // Method to toggle fullscreen mode
    public static void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (isFullscreen) {
            // Exit fullscreen
            window.dispose();
            window.setUndecorated(false);
            window.setResizable(true);
            gd.setFullScreenWindow(null);
            window.setVisible(true);
            window.pack();
            window.setLocationRelativeTo(null);
            isFullscreen = false;
        } else {
            // Enter fullscreen
            window.dispose();
            window.setUndecorated(true);
            window.setResizable(false);
            gd.setFullScreenWindow(window);
            window.setVisible(true);
            isFullscreen = true;
        }

        // Update layout after fullscreen change
        updateResponsiveLayout();
    }
}
