package com.ymcmp.seihou;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author YTENG
 */
public class JoeSchmoeGame extends AbstractGame {

    private static final Color LIGHT_GRAY_SHADER = new Color(192, 192, 192, 100);

    private static final byte LOADING = -1;
    private static final byte INIT = 0;
    private static final byte PLAYING = 1;
    private static final byte DIE_ANIM = 2;
    private static final byte PAUSE = 3;
    private static final byte LOSE = 4;
    private static final byte WIN = 5;

    private static final int PLAYER_R = 8;
    private static final float PLAYER_FAST_V = 85;
    private static final float PLAYER_SLOW_V = 40;

    private static final float PB_V = 120;
    private static final float PB_RATE = 0.25f;

    private final AtomicInteger state = new AtomicInteger(INIT);
    private final AtomicBoolean fireFlag = new AtomicBoolean(true);

    private float bossX = 0f;
    private float bossY = 0f;
    private float bossHp = 100f;

    private float playerX = 0f;
    private float playerY = 0f;
    private int playerHp = 5;

    private float pbTimer = 0f;
    private float bfTimer = 0f;

    // These *must* have the same size
    private final List<Float> bulletX = new ArrayList<>(64);
    private final List<Float> bulletY = new ArrayList<>(64);
    private final List<Float> bulletR = new ArrayList<>(64);
    private final List<Float> bulletDx = new ArrayList<>(64);
    private final List<Float> bulletDy = new ArrayList<>(64);

    private final List<Float> pbX = new ArrayList<>(20);
    private final List<Float> pbY = new ArrayList<>(20);
    private final List<Float> pbR = new ArrayList<>(20);

    @Override
    public void init() {
        super.init();
        frame.setTitle("joe.schmoe");
        frame.setSize(500, 350);
        frame.setResizable(false);
        frame.setVisible(true);

        reset();
        System.out.println("GAME STARTED");
    }

    private void reset() {
        playerX = canvas.getWidth() / 2f;
        playerY = canvas.getHeight() - 20;
        playerHp = 5;

        pbTimer = 0f;
        bfTimer = 0f;
        fireFlag.compareAndSet(false, true);

        bossX = canvas.getWidth() / 2f;
        bossY = 30;
        bossHp = 100f;

        pbX.clear();
        pbY.clear();
        pbR.clear();
        bulletX.clear();
        bulletY.clear();
        bulletR.clear();
        bulletDx.clear();
        bulletDy.clear();
    }

    @Override
    public void update(long deltaT) {
        switch (state.get()) {
        case LOADING:
            try {
                Thread.sleep(3000);
                state.set(INIT);
            } catch (InterruptedException ex) {
                abort();
                System.err.println("Could not complete simulated loading");
            }
            break;
        case PLAYING:
            if (this.isKeyDown(KeyEvent.VK_ESCAPE)) {
                state.set(PAUSE);
                return;
            }

            final float playerV = this.isKeyDown(KeyEvent.VK_SHIFT) ? PLAYER_SLOW_V : PLAYER_FAST_V;
            final float dt = deltaT / 1000f;
            final float dv = playerV * dt;
            if (this.isKeyDown(KeyEvent.VK_RIGHT)) {
                if (playerX + dv < canvas.getWidth()) {
                    playerX += dv;
                }
            }
            if (this.isKeyDown(KeyEvent.VK_LEFT)) {
                if (playerX - dv > 0) {
                    playerX -= dv;
                }
            }
            if (this.isKeyDown(KeyEvent.VK_DOWN)) {
                if (playerY + dv < canvas.getHeight()) {
                    playerY += dv;
                }
            }
            if (this.isKeyDown(KeyEvent.VK_UP)) {
                if (playerY - dv > 0) {
                    playerY -= dv;
                }
            }

            if ((bfTimer += dt) >= 2) {
                bulletPatternRadial(bossX, bossY, (float) Math.toRadians(30), (float) Math.toRadians(0), 6f, PB_V);
                bulletPatternRadial(bossX, bossY, (float) Math.toRadians(60), (float) Math.toRadians(15), 6f, PB_V);
                bfTimer = 0f;
            }

            if ((pbTimer += dt) >= PB_RATE) {
                fireFlag.set(true);
            }
            if (fireFlag.get() && this.isKeyDown(KeyEvent.VK_Z)) {
                fireFlag.set(false);
                pbTimer = 0f;
                pbX.add(playerX);
                pbY.add(playerY);
                pbR.add(4f);
            }

            for (int i = 0; i < bulletR.size(); ++i) {
                final float r = bulletR.get(i);
                final float newX = bulletX.get(i) + bulletDx.get(i) * dt;
                if (newX + r > 0 && newX - r < canvas.getWidth()) {
                    final float newY = bulletY.get(i) + bulletDy.get(i) * dt;
                    if (newY + r > 0 && newY - r < canvas.getHeight()) {
                        bulletX.set(i, newX);
                        bulletY.set(i, newY);
                        continue;
                    }
                }

                // Reaching here means bullet went out of bounds, destroy
                bulletR.remove(i);
                bulletX.remove(i);
                bulletY.remove(i);
                bulletDx.remove(i);
                bulletDy.remove(i);
                --i;
            }

            for (int i = 0; i < pbR.size(); ++i) {
                if (pbY.get(i) - pbR.get(i) <= 0) {
                    pbR.remove(i);
                    pbX.remove(i);
                    pbY.remove(i);
                    --i;
                    continue;
                }

                pbY.set(i, pbY.get(i) - PB_V * dt);
            }

            for (int i = 0; i < bulletR.size(); ++i) {
                if (circlesCollide(bulletX.get(i), bulletY.get(i), bulletR.get(i),
                                   playerX, playerY, PLAYER_R)) {
                    bulletR.remove(i);
                    bulletX.remove(i);
                    bulletY.remove(i);
                    bulletDx.remove(i);
                    bulletDy.remove(i);
                    state.set(DIE_ANIM);
                    break;
                }
            }

            for (int i = 0; i < pbR.size(); ++i) {
                if (circlesCollide(pbX.get(i), pbY.get(i), pbR.get(i),
                                   bossX, bossY, PLAYER_R)) {
                    pbR.remove(i);
                    pbX.remove(i);
                    pbY.remove(i);
                    if ((bossHp -= 0.5f) <= 0f) {
                        state.set(WIN);
                    }
                    break;
                }
            }
            break;
        case DIE_ANIM:
            try {
                if (--playerHp <= 0) {
                    state.set(LOSE);
                    return;
                }
                Thread.sleep(1000);
                state.set(PLAYING);
            } catch (InterruptedException ex) {
                abort();
            }
            break;
        case INIT:
            reset();
        // FALLTHROUGH
        case PAUSE:
            if (this.isKeyDown(KeyEvent.VK_ENTER)) {
                state.set(PLAYING);
                return;
            }
            if (this.isKeyDown(KeyEvent.VK_Q)) {
                abort();
            }
            break;
        case LOSE:
        case WIN:
            if (this.isKeyDown(KeyEvent.VK_ENTER)) {
                while (this.isKeyDown(KeyEvent.VK_ENTER)) {
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException ex) {
                    }
                }
                state.set(INIT);
            }
            break;
        default:
        }
    }

    private static boolean circlesCollide(float x1, float y1, float r1,
                                          float x2, float y2, float r2) {
        return (r1 + r2) > Math.hypot(x2 - x1, y2 - y1);
    }

    private void bulletPatternRadial(float originX, float originY,
                                     float spacing, float tilt,
                                     float size, float speed) {
        // o -> apply [radial]
        //
        // \ | /
        // - o -   (angle between is spacing (rad), angle offset is tilt (rad))
        // / | \

        if (size == 0) {
            return;
        }

        for (float counter = 0; counter < 2 * Math.PI; counter += spacing) {
            bulletR.add(size);
            bulletX.add(originX);
            bulletY.add(originY);

            final float actAngle = counter + tilt;
            bulletDx.add((float) Math.cos(actAngle) * speed);
            bulletDy.add((float) Math.sin(actAngle) * speed);
        }
    }

    @Override
    public void render(Graphics g) {
        g.setColor(Color.black);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        switch (state.get()) {
        case LOADING:
            g.setColor(Color.gray);
            g.drawString("Now loading...", 0, 12);
            break;
        case INIT:
            g.setColor(Color.blue);
            g.drawString("Project Seihou", canvas.getWidth() / 2 - 40, 60);
            g.drawString("[ENTER]", canvas.getWidth() / 2 - 24, 82);
            break;
        case PLAYING:
            drawGame(g);
            break;
        case DIE_ANIM:
            drawGame(g);
            g.setColor(LIGHT_GRAY_SHADER);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.setColor(Color.red);
            g.drawString("HP - 1", canvas.getWidth() / 2 - 16, 60);
            break;
        case PAUSE:
            drawGame(g);
            g.setColor(LIGHT_GRAY_SHADER);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.setColor(Color.blue);
            g.drawString("PAUSED", canvas.getWidth() / 2 - 24, 60);
            break;
        case LOSE:
            g.setColor(Color.BLUE);
            g.drawString("GAME OVER", canvas.getWidth() / 2 - 32, 60);
            break;
        case WIN:
            g.setColor(Color.BLUE);
            g.drawString("+1", canvas.getWidth() / 2 - 8, 60);
            break;
        default:
        }
    }

    private void drawGame(Graphics g) {
        g.setColor(Color.white);
        g.fillOval((int) playerX - PLAYER_R, (int) playerY - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);
        for (int i = 0; i < pbR.size(); ++i) {
            final float r = pbR.get(i);
            g.drawOval((int) (pbX.get(i) - r), (int) (pbY.get(i) - r), (int) (r * 2), (int) (r * 2));
        }
        g.setColor(Color.yellow);
        g.fillOval((int) bossX - PLAYER_R, (int) bossY - PLAYER_R, PLAYER_R * 2, PLAYER_R * 2);
        for (int i = 0; i < bulletX.size(); ++i) {
            final float r = bulletR.get(i);
            g.drawOval((int) (bulletX.get(i) - r), (int) (bulletY.get(i) - r), (int) (r * 2), (int) (r * 2));
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        System.out.println("GAME ENDED");
    }
}
