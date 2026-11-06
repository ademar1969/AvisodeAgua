package br.com.avisodeagua;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Ícone de gota d'água usado na bandeja do sistema e nas janelas. */
public final class Icones {
    private static BufferedImage base;

    private Icones() { }

    public static synchronized Image gota(int tamanho) {
        if (base == null) {
            try (InputStream in = Icones.class.getResourceAsStream("/img/icone.png")) {
                if (in != null) base = ImageIO.read(in);
            } catch (Exception ignored) { }
        }
        if (base == null) {
            base = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = base.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x1E90DC));
            g.fillOval(12, 24, 40, 38);
            g.fillPolygon(new int[]{32, 14, 50}, new int[]{4, 34, 34}, 3);
            g.dispose();
        }
        BufferedImage out = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(base, 0, 0, tamanho, tamanho, null);
        g.dispose();
        return out;
    }
}
