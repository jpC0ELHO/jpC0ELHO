import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class SnakeGame extends JPanel implements ActionListener {

    // Grade
    private static final int COLS = 40, ROWS = 20, CELL = 20, GAP = 3, HUD = 50;

    // Cores (mesma paleta do README)
    private static final Color BG = new Color(0x0d1117);
    private static final Color EMPTY = new Color(0x161b22);
    private static final Color[] FOOD = {
            new Color(0x4d4d4d), new Color(0x8c8c8c), new Color(0xc8c8c8), new Color(0xffffff)
    };
    private static final Color SNAKE_HEAD = new Color(0xb000b0);
    private static final Color SNAKE_BODY = new Color(0x8b008b);
    private static final Color TEXT = Color.WHITE;

    private enum State { WAITING, PLAYING, PAUSED, GAME_OVER }

    private final Deque<Point> snake = new ArrayDeque<>();
    private final Random random = new Random();
    private final Timer timer = new Timer(120, this);

    private Point food;
    private int foodLevel;
    private int dx = 1, dy = 0, nextDx = 1, nextDy = 0;
    private int score, best;
    private State state = State.WAITING;

    public SnakeGame() {
        setPreferredSize(new Dimension(COLS * CELL, ROWS * CELL + HUD));
        setBackground(BG);
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (state == State.WAITING || state == State.GAME_OVER) start();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });

        resetSnake();
    }

    private void resetSnake() {
        snake.clear();
        int y = ROWS / 2;
        for (int x = 5; x >= 2; x--) snake.addLast(new Point(x, y));
        dx = nextDx = 1;
        dy = nextDy = 0;
        score = 0;
        timer.setDelay(120);
        spawnFood();
    }

    private void start() {
        resetSnake();
        state = State.PLAYING;
        timer.start();
        repaint();
    }

    private void handleKey(int key) {
        switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> turn(0, -1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> turn(0, 1);
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> turn(-1, 0);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> turn(1, 0);
            case KeyEvent.VK_P -> togglePause();
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> {
                if (state == State.WAITING || state == State.GAME_OVER) start();
                else togglePause();
            }
            default -> { }
        }
    }

    private void turn(int ndx, int ndy) {
        if (state != State.PLAYING) return;
        // impede voltar na direção oposta
        if (ndx == -dx && ndy == -dy) return;
        nextDx = ndx;
        nextDy = ndy;
    }

    private void togglePause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
            timer.stop();
        } else if (state == State.PAUSED) {
            state = State.PLAYING;
            timer.start();
        }
        repaint();
    }

    private void spawnFood() {
        Point p;
        do {
            p = new Point(random.nextInt(COLS), random.nextInt(ROWS));
        } while (snake.contains(p));
        food = p;
        foodLevel = random.nextInt(FOOD.length);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state != State.PLAYING) return;

        dx = nextDx;
        dy = nextDy;
        Point head = snake.peekFirst();
        Point next = new Point(head.x + dx, head.y + dy);

        boolean hitWall = next.x < 0 || next.y < 0 || next.x >= COLS || next.y >= ROWS;
        boolean eating = next.equals(food);
        // a cauda sai do lugar neste passo, então só conta colisão com ela se estiver comendo
        boolean hitSelf = snake.contains(next) && !(next.equals(snake.peekLast()) && !eating);

        if (hitWall || hitSelf) {
            state = State.GAME_OVER;
            best = Math.max(best, score);
            timer.stop();
            repaint();
            return;
        }

        snake.addFirst(next);
        if (eating) {
            score += foodLevel + 1; // quadrados mais claros valem mais
            spawnFood();
            timer.setDelay(Math.max(55, timer.getDelay() - 3)); // acelera aos poucos
        } else {
            snake.removeLast();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawHud(g2);

        // grade
        for (int x = 0; x < COLS; x++)
            for (int y = 0; y < ROWS; y++)
                drawCell(g2, x, y, EMPTY);

        // comida
        drawCell(g2, food.x, food.y, FOOD[foodLevel]);

        // cobra
        boolean first = true;
        for (Point p : snake) {
            drawCell(g2, p.x, p.y, first ? SNAKE_HEAD : SNAKE_BODY);
            first = false;
        }

        switch (state) {
            case WAITING -> overlay(g2, "Click to play", "Setas / WASD para mover  •  P para pausar");
            case PAUSED -> overlay(g2, "Pausado", "Pressione P para continuar");
            case GAME_OVER -> overlay(g2, "Game Over", "Pontos: " + score + "  •  Clique para jogar de novo");
            default -> { }
        }
    }

    private void drawCell(Graphics2D g2, int x, int y, Color c) {
        g2.setColor(c);
        g2.fillRoundRect(x * CELL + GAP / 2, HUD + y * CELL + GAP / 2, CELL - GAP, CELL - GAP, 5, 5);
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(TEXT);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        g2.drawString("Score: " + score, 12, 32);
        String bestText = "Best: " + best;
        int w = g2.getFontMetrics().stringWidth(bestText);
        g2.drawString(bestText, getWidth() - w - 12, 32);
    }

    private void overlay(Graphics2D g2, String title, String subtitle) {
        g2.setColor(new Color(13, 17, 23, 200));
        g2.fillRect(0, HUD, getWidth(), getHeight() - HUD);

        g2.setColor(TEXT);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
        FontMetrics fm = g2.getFontMetrics();
        int cy = HUD + (getHeight() - HUD) / 2;
        g2.drawString(title, (getWidth() - fm.stringWidth(title)) / 2, cy);

        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(0xc8c8c8));
        g2.drawString(subtitle, (getWidth() - fm.stringWidth(subtitle)) / 2, cy + 36);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake — Coelho Techne");
            SnakeGame game = new SnakeGame();
            frame.add(game);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
