package entities;

import main.FadeTransition;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class GameLandingPage extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

	private int cloudDirection = 1;

	private float flashAlpha = 0f;
	private boolean flashing = false;
	private int flashCooldown = 0;

    private BufferedImage background;
    private BufferedImage cloud;
    private BufferedImage sword;

    private final List<Point> clouds = new ArrayList<>();
    private final Timer timer;
    private Rectangle playButton = new Rectangle(0, 0, 180, 60);
    private boolean hoveringPlay = false;
    private Rectangle exitButton = new Rectangle(0, 0, 120, 40);
    private boolean hoveringExit = false;

    private int swordY = 0;
    private boolean swordUp = true;
    private Runnable onPlay;

    public GameLandingPage(Runnable onPlay) {
        this.onPlay = onPlay;
        setPreferredSize(new Dimension(800, 600));

        loadAssets();

        for (int i = 0; i < 5; i++)
            clouds.add(new Point(-150 + i * 220, 50 + (int) (Math.random() * 120)));

        timer = new Timer(40, this);
        timer.start();

        addMouseListener(this);
        addMouseMotionListener(this);
        setFocusable(true);

        // Add key listener for spacebar to trigger play, F for fullscreen, and ESC to exit
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    onPlay.run();
                } else if (e.getKeyCode() == KeyEvent.VK_F) {
                    main.Main.toggleFullscreen();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });
    }

    private void loadAssets() {
        try {
            background = ImageIO.read(getClass().getResourceAsStream("/assets/ui/background.png"));
        } catch (Exception e) {
            background = null;
            System.err.println("background.png not found in /com/darkcastle/assets/");
        }
        try {
            cloud = ImageIO.read(getClass().getResourceAsStream("/assets/ui/clouds.png"));
        } catch (Exception e) {
            cloud = null;
            System.err.println("clouds.png not found in /com/darkcastle/assets/");
        }
        try {
            sword = ImageIO.read(getClass().getResourceAsStream("/assets/ui/sword.png"));
        } catch (Exception e) {
            sword = null;
            System.err.println("sword.png not found in /com/darkcastle/assets/");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // move clouds left and right gently
        for (Point p : clouds) {
            p.x += cloudDirection * 0.5; // move in current direction
        }

        // reverse direction when reaching sides
        if (!clouds.isEmpty()) {
            Point first = clouds.get(0);
            if (first.x > 40 || first.x < -40) {
                cloudDirection *= -1; // reverse direction
            }
        }

        repaint();

        // Sword bob animation
      //  if (swordUp) swordY--;
      //  else swordY++;
      //  if (swordY < -8) swordUp = false;
      //  if (swordY > 8) swordUp = true;

       if (flashCooldown > 0) flashCooldown--;
        else if (!flashing && Math.random() < 0.01) {  // random flash start
            flashing = true;
            flashAlpha = 0.0f;
        }

        if (flashing) {
            flashAlpha += 0.08f;
            if (flashAlpha >= 0.4f) flashing = false;
        } else if (flashAlpha > 0f) {
            flashAlpha -= 0.05f;
            if (flashAlpha <= 0f) {
                flashAlpha = 0f;
                flashCooldown = 100 + (int)(Math.random() * 100);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();

        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        if (cloud != null) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            for (Point p : clouds) g.drawImage(cloud, p.x, p.y, 140, 70, null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        g.setFont(new Font("Georgia", Font.BOLD, 72));
        g.setColor(new Color(255, 210, 120));

        String leftWord = "BLADE";
        String rightWord = "QUEST";

        int leftWidth = g.getFontMetrics().stringWidth(leftWord);
        int rightWidth = g.getFontMetrics().stringWidth(rightWord);
        int swordSpacing = 120; // Space for the sword image
        int totalWidth = leftWidth + rightWidth + swordSpacing;

        int baseX = getWidth() / 2 - totalWidth / 2;
        int baseY = 130; // Moved down slightly for larger text

        g.drawString(leftWord, baseX, baseY);

        if (sword != null) {
        	// Center the sword between BLADE and QUEST
        	int swordCenterX = baseX + leftWidth + (swordSpacing / 2);
        	int swordX = swordCenterX - 70; // 70 is half of sword width (140/2)
        	int swordYdraw = baseY - 85 + swordY; // Adjusted for larger font
        	g.drawImage(sword, swordX, swordYdraw, 140, 140, null); // Slightly larger sword
        }

        int rightX = baseX + leftWidth + swordSpacing;
        g.drawString(rightWord, rightX, baseY);

        int bx = getWidth() / 2 - 90;
        int by = getHeight() / 2 + 100;
        playButton.setBounds(bx, by, 180, 60);

        if (hoveringPlay) {
            g.setColor(new Color(255, 255, 255, 230));
            g.fillRoundRect(bx - 3, by - 3, 186, 66, 15, 15);
            g.setColor(new Color(255, 120, 0));
        } else {
            g.setColor(Color.orange);
        }
        g.fillRoundRect(bx, by, 180, 60, 15, 15);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Georgia", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String text = "PLAY";
        int tx2 = bx + (180 - fm.stringWidth(text)) / 2;
        int ty2 = by + ((60 - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text, tx2, ty2);

        // Exit button below play button with spacing (centered)
        int ex = getWidth() / 2 - 60; // Center the 120px wide EXIT button
        int ey = by + 60 + 20; // Add 20 pixels of spacing between buttons
        exitButton.setBounds(ex, ey, 120, 40);

        if (hoveringExit) {
            g.setColor(new Color(255, 255, 255, 230));
            g.fillRoundRect(ex - 3, ey - 3, 126, 46, 12, 12);
            g.setColor(new Color(255, 120, 0));
        } else {
            g.setColor(Color.red);
        }
        g.fillRoundRect(ex, ey, 120, 40, 10, 10);

        g.setColor(Color.BLACK);
        String exitText = "EXIT";
        int ex2 = ex + (120 - fm.stringWidth(exitText)) / 2;
        int ey2 = ey + ((40 - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(exitText, ex2, ey2);

        if (flashAlpha > 0f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashAlpha));
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        g.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (playButton.contains(e.getPoint())) {
            onPlay.run();
        } else if (exitButton.contains(e.getPoint())) {
            System.exit(0);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mouseDragged(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {
        hoveringPlay = playButton.contains(e.getPoint());
        hoveringExit = exitButton.contains(e.getPoint());
        repaint();
    }
}
