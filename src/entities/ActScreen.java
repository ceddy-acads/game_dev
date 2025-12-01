package entities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ActScreen extends JPanel {
    private float alpha = 0.0f; // For fade in effect
    private Timer displayTimer;

public ActScreen(Runnable onComplete) {
    setBackground(Color.BLACK);
    setFocusable(true);

    // Start fully opaque (no fade-in)
    alpha = 1.0f;

    // Start display timer (show "Act 1" for 2 seconds, then complete)
    displayTimer = new Timer(2000, displayEvent -> {
        displayTimer.stop();
        if (onComplete != null) {
            onComplete.run();
        }
    });
    displayTimer.setRepeats(false);
    displayTimer.start();

    // Handle key press to skip
    addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (displayTimer != null) {
                displayTimer.stop();
            }
            if (onComplete != null) {
                onComplete.run();
            }
        }
    });
}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw "Act 1" text
        g2d.setColor(new Color(255, 255, 255, (int)(alpha * 255)));
        g2d.setFont(new Font("Serif", Font.BOLD, 72));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "Act 1";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() + textHeight) / 2 - fm.getDescent();

        g2d.drawString(text, x, y);

        // Draw subtitle
        g2d.setFont(new Font("Serif", Font.PLAIN, 24));
        String subtitle = "The Journey Begins";
        FontMetrics fm2 = g2d.getFontMetrics();
        int subtitleWidth = fm2.stringWidth(subtitle);
        int subtitleX = (getWidth() - subtitleWidth) / 2;
        int subtitleY = y + 60;

        g2d.drawString(subtitle, subtitleX, subtitleY);


    }
}
