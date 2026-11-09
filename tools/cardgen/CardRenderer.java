import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public class CardRenderer {

    static final int W = 320, H = 448;
    static final double SCALE = W / 168.0;

    static final Color ABYSS     = hex("#04141f");
    static final Color DEEP      = hex("#072a3d");
    static final Color DEEP_2    = hex("#0a3a52");
    static final Color TEAL_BRT  = hex("#2fb6c4");
    static final Color FOAM      = hex("#d6eef4");
    static final Color INK       = hex("#0b2230");
    static final Color CARD_FACE = hex("#f4fafc");
    static final Color CARD_EDGE = hex("#b9d7e0");
    static final Color STAT_KEY  = hex("#4a6a7a");
    static final Color MEDIA_A   = hex("#dceef4");
    static final Color MEDIA_B   = hex("#d2e8f0");

    static final int RADIUS   = px(12);
    static final int TITLE_H  = px(2.4 * 16);
    static final int ROW_H    = px(0.82 * 16 * 1.15 + 2 * 0.14 * 16);
    static final int STATS_PAD_TOP = px(0.35 * 16), STATS_PAD_BOTTOM = px(0.4 * 16);
    static final int STATS_H  = STATS_PAD_TOP + 3 * ROW_H + STATS_PAD_BOTTOM;
    static final int MEDIA_Y  = TITLE_H;
    static final int MEDIA_H  = H - TITLE_H - STATS_H;
    static final int STATS_Y  = MEDIA_Y + MEDIA_H;

    static final String[] STATS = { "Speed", "Size", "Danger" };

    static String fontFamily;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: java CardRenderer.java <fish_seed.csv> <output-dir>");
            System.exit(2);
        }
        System.setProperty("java.awt.headless", "true");
        fontFamily = pickFont("Segoe UI", "Roboto", "Helvetica Neue", "Arial", "DejaVu Sans");

        Path out = Path.of(args[1]);
        Files.createDirectories(out);

        List<String[]> rows = readCsv(Path.of(args[0]));
        for (String[] r : rows) {
            int id = Integer.parseInt(r[0]);
            int[] values = { Integer.parseInt(r[2]), Integer.parseInt(r[3]), Integer.parseInt(r[4]) };
            write(renderFront(r[1], values), out.resolve("card_" + id + ".png"));
        }
        write(renderBack(), out.resolve("card_back.png"));
        writeLayout(out.resolve("layout.txt"));

        System.out.println("Wrote " + rows.size() + " fronts + card_back.png (" + W + "x" + H
                + ", font: " + fontFamily + ") to " + out.toAbsolutePath());
    }

    static BufferedImage renderFront(String name, int[] values) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = begin(img);
        Shape card = cardShape();
        g.setClip(card);

        g.setColor(CARD_FACE);
        g.fillRect(0, 0, W, H);

        g.setPaint(new GradientPaint(0, 0, DEEP_2, 0, TITLE_H, DEEP));
        g.fillRect(0, 0, W, TITLE_H);
        drawTitle(g, name);

        int stripe = px(8);
        for (int y = 0; y < MEDIA_H; y += stripe) {
            g.setColor(((y / stripe) % 2 == 0) ? MEDIA_A : MEDIA_B);
            g.fillRect(0, MEDIA_Y + y, W, Math.min(stripe, MEDIA_H - y));
        }
        float cx = W / 2f, cy = MEDIA_Y + MEDIA_H * 1.2f;
        float glowR = (float) Math.hypot(W / 2.0, MEDIA_H * 1.2) * 0.7f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), glowR,
                new float[] { 0f, 1f },
                new Color[] { new Color(47, 182, 196, 89), new Color(47, 182, 196, 0) }));
        g.fillRect(0, MEDIA_Y, W, MEDIA_H);

        g.setColor(CARD_EDGE);
        g.fillRect(0, STATS_Y - px(1), W, px(1));

        Font keyFont = new Font(fontFamily, Font.PLAIN, px(0.82 * 16));
        Font valFont = new Font(fontFamily, Font.BOLD,  px(0.82 * 16));
        int padX = px(0.5 * 16 + 0.3 * 16);
        for (int i = 0; i < 3; i++) {
            int top = STATS_Y + STATS_PAD_TOP + i * ROW_H;
            int baseline = centeredBaseline(g, keyFont, top, ROW_H);
            g.setFont(keyFont);
            g.setColor(STAT_KEY);
            g.drawString(STATS[i], padX, baseline);
            g.setFont(valFont);
            g.setColor(INK);
            String v = String.valueOf(values[i]);
            g.drawString(v, W - padX - g.getFontMetrics().stringWidth(v), baseline);
        }

        drawEdge(g, card);
        g.dispose();
        return img;
    }

    static BufferedImage renderBack() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = begin(img);
        Shape card = cardShape();
        g.setClip(card);

        int band = px(10);
        g.rotate(Math.toRadians(-45), W / 2.0, H / 2.0);
        int span = (int) Math.hypot(W, H) + 2 * band;
        for (int x = -span, k = 0; x < span; x += band, k++) {
            g.setColor((k % 2 == 0) ? DEEP : DEEP_2);
            g.fillRect(W / 2 + x, H / 2 - span, band, 2 * span);
        }
        g.rotate(Math.toRadians(45), W / 2.0, H / 2.0);

        int inset = px(7);
        g.setColor(new Color(47, 182, 196, 90));
        g.setStroke(new BasicStroke(px(1.5)));
        g.draw(new RoundRectangle2D.Float(inset, inset, W - 2 * inset, H - 2 * inset,
                2 * (RADIUS - inset / 2f), 2 * (RADIUS - inset / 2f)));

        drawEdge(g, card);
        g.dispose();
        return img;
    }

        static void drawTitle(Graphics2D g, String name) {
        int maxW = W - 2 * px(0.55 * 16);
        float size = px(0.82 * 16);
        List<String> lines;
        Font f;
        while (true) {
            f = new Font(fontFamily, Font.BOLD, Math.round(size));
            lines = wrap(name, g.getFontMetrics(f), maxW);
            if (lines.size() <= 2 || size < 12) break;
            size -= 1;
        }
        g.setFont(f);
        g.setColor(FOAM);
        FontMetrics fm = g.getFontMetrics();
        int lineH = Math.round(fm.getAscent() * 1.15f);
        int blockH = lineH * lines.size();
        int y = (TITLE_H - blockH) / 2 + fm.getAscent();
        for (String line : lines) {
            g.drawString(line, (W - fm.stringWidth(line)) / 2, y);
            y += lineH;
        }
    }

    static List<String> wrap(String text, FontMetrics fm, int maxW) {
        List<String> lines = new ArrayList<>();
        String cur = "";
        for (String word : text.replace(" o' ", " o' ").split(" ")) {
            String next = cur.isEmpty() ? word : cur + " " + word;
            if (fm.stringWidth(next) <= maxW || cur.isEmpty()) cur = next;
            else { lines.add(cur); cur = word; }
        }
        if (!cur.isEmpty()) lines.add(cur);
        return lines;
    }

    static Graphics2D begin(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    static Shape cardShape() {
        return new RoundRectangle2D.Float(0, 0, W, H, 2 * RADIUS, 2 * RADIUS);
    }

    static void drawEdge(Graphics2D g, Shape card) {
        g.setClip(null);
        g.setColor(CARD_EDGE);
        float s = px(1);
        g.setStroke(new BasicStroke(s));
        g.draw(new RoundRectangle2D.Float(s / 2, s / 2, W - s, H - s, 2 * RADIUS - s, 2 * RADIUS - s));
    }

    static int centeredBaseline(Graphics2D g, Font f, int top, int height) {
        FontMetrics fm = g.getFontMetrics(f);
        return top + (height - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent();
    }

    static void writeLayout(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Card texture layout, ").append(W).append("x").append(H).append(" px\n");
        sb.append(String.format(Locale.ROOT, "title  v %.4f .. %.4f%n", 0.0, TITLE_H / (double) H));
        sb.append(String.format(Locale.ROOT, "media  v %.4f .. %.4f%n", MEDIA_Y / (double) H, STATS_Y / (double) H));
        for (int i = 0; i < 3; i++) {
            int top = STATS_Y + STATS_PAD_TOP + i * ROW_H;
            sb.append(String.format(Locale.ROOT, "%-6s v %.4f .. %.4f%n",
                    STATS[i].toLowerCase(), top / (double) H, (top + ROW_H) / (double) H));
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    static List<String[]> readCsv(Path csv) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null || !header.replace("﻿", "").startsWith("id,name,speed,size,danger")) {
                throw new IllegalStateException("Unexpected header in " + csv + ": " + header);
            }
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] f = line.split(",", -1);
                if (f.length < 5) throw new IllegalStateException("Short row: " + line);
                rows.add(Arrays.copyOf(f, 5));
            }
        }
        return rows;
    }

    static void write(BufferedImage img, Path file) throws IOException {
        if (!ImageIO.write(img, "png", file.toFile())) {
            throw new IOException("No PNG writer available for " + file);
        }
    }

    static String pickFont(String... wanted) {
        List<String> have = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String w : wanted) if (have.contains(w)) return w;
        return Font.SANS_SERIF;
    }

    static int px(double cssPx) { return (int) Math.round(cssPx * SCALE); }

    static Color hex(String h) { return Color.decode(h); }
}
