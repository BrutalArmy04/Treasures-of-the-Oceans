import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class BannerRenderer {

    static final int W = 640, H = 64, RADIUS = 18;

    static final Color DEEP_2   = Color.decode("#0a3a52");
    static final Color ABYSS    = Color.decode("#04141f");
    static final Color TEAL_BRT = Color.decode("#2fb6c4");
    static final Color FOAM     = Color.decode("#d6eef4");
    static final Color ACCENT   = Color.decode("#ffc857");
    static final Color ACCENT_D = Color.decode("#c98f14");
    static final Color INK      = Color.decode("#0b2230");

    static String fontFamily;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: java BannerRenderer.java <output-dir>");
            System.exit(2);
        }
        System.setProperty("java.awt.headless", "true");
        fontFamily = pickFont("Segoe UI", "Roboto", "Helvetica Neue", "Arial", "DejaVu Sans");
        Path out = Path.of(args[0]);
        Files.createDirectories(out);

        write(plain("Rândul tău:  1 Speed   ·   2 Size   ·   3 Danger"), out, "banner_your_turn.png");
        write(plain("Rândul botului:  apasă SPACE"), out, "banner_bot_turn.png");
        write(plain("Ai câștigat runda"), out, "banner_round_you.png");
        write(plain("Botul câștigă runda"), out, "banner_round_bot.png");
        write(plain("Egalitate: război!"), out, "banner_war.png");
        write(gold("Ai câștigat jocul!   R = joc nou"), out, "banner_game_you.png");
        write(gold("Botul a câștigat jocul.   R = joc nou"), out, "banner_game_bot.png");

        System.out.println("Wrote 7 banners (" + W + "x" + H + ", font: " + fontFamily + ") to " + out.toAbsolutePath());
    }

    static BufferedImage plain(String text) {
        return render(text, new GradientPaint(0, 0, DEEP_2, 0, H, ABYSS), TEAL_BRT, FOAM);
    }

    static BufferedImage gold(String text) {
        return render(text, new GradientPaint(0, 0, ACCENT, 0, H, ACCENT_D), ACCENT, INK);
    }

    static BufferedImage render(String text, GradientPaint fill, Color edge, Color ink) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, W, H, 2 * RADIUS, 2 * RADIUS);
        g.setPaint(fill);
        g.fill(shape);
        g.setColor(edge);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Float(1, 1, W - 2, H - 2, 2 * RADIUS - 2, 2 * RADIUS - 2));

        float size = 26f;
        Font f = new Font(fontFamily, Font.BOLD, Math.round(size));
        while (g.getFontMetrics(f).stringWidth(text) > W - 48 && size > 12) {
            size -= 1f;
            f = new Font(fontFamily, Font.BOLD, Math.round(size));
        }
        g.setFont(f);
        g.setColor(ink);
        FontMetrics fm = g.getFontMetrics();
        int x = (W - fm.stringWidth(text)) / 2;
        int y = (H - (fm.getAscent() + fm.getDescent())) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();
        return img;
    }

    static void write(BufferedImage img, Path dir, String name) throws IOException {
        if (!ImageIO.write(img, "png", dir.resolve(name).toFile())) {
            throw new IOException("No PNG writer for " + name);
        }
    }

    static String pickFont(String... wanted) {
        List<String> have = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String w : wanted) if (have.contains(w)) return w;
        return Font.SANS_SERIF;
    }
}
