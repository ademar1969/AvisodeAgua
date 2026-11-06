package br.com.avisodeagua;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/** Gráficos desenhados com Java2D (sem bibliotecas externas, compatível com Java 8). */
public final class Graficos {

    static final Color AZUL = new Color(0x1E90DC);
    static final Color AZUL_CLARO = new Color(0xA9D6F3);
    static final Color AZUL_ESCURO = new Color(0x0B4F75);
    static final Color LARANJA = new Color(0xF0A030);
    static final Color CINZA = new Color(0xB8C2CC);
    static final Color VERDE = new Color(0x2E9E5B);
    static final Color TEXTO = new Color(0x33414E);
    static final Color GRADE = new Color(0xE4EAF0);

    private Graficos() { }

    /** Formatador de valores do eixo Y e das dicas. */
    public interface Formato { String formatar(double v); }

    /** Gráfico de barras com linha de meta opcional. */
    public static class Barras extends JComponent {
        private String titulo = "";
        private String[] rotulos = new String[0];
        private String[] dicas = new String[0];
        private double[] valores = new double[0];
        private double meta = -1;
        private String rotuloMeta = "Meta";
        private Formato formato = new Formato() { public String formatar(double v) { return String.valueOf((int) v); } };
        private boolean colorirPelaMeta;
        private Rectangle[] areas = new Rectangle[0];

        public Barras() {
            setPreferredSize(new Dimension(760, 340));
            setToolTipText("");
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        public void definir(String titulo, String[] rotulos, double[] valores, String[] dicas,
                            double meta, String rotuloMeta, Formato formato, boolean colorirPelaMeta) {
            this.titulo = titulo;
            this.rotulos = rotulos;
            this.valores = valores;
            this.dicas = dicas;
            this.meta = meta;
            this.rotuloMeta = rotuloMeta;
            this.formato = formato;
            this.colorirPelaMeta = colorirPelaMeta;
            repaint();
        }

        @Override public String getToolTipText(MouseEvent e) {
            for (int i = 0; i < areas.length; i++) {
                if (areas[i] != null && e.getX() >= areas[i].x - 2 && e.getX() <= areas[i].x + areas[i].width + 2
                        && e.getY() >= 30) {
                    return dicas != null && i < dicas.length ? dicas[i] : formato.formatar(valores[i]);
                }
            }
            return null;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setFont(TelaAlerta.fonte(Font.BOLD, 15));
            g.setColor(TEXTO);
            g.drawString(titulo, 16, 24);

            int esq = 70, dir = 24, topo = 50, base = 46;
            int w = getWidth() - esq - dir, h = getHeight() - topo - base;
            if (w < 50 || h < 50 || valores.length == 0) { g.dispose(); return; }

            double max = meta > 0 ? meta : 0;
            for (double v : valores) max = Math.max(max, v);
            if (max <= 0) max = 1;
            double passo = passoBonito(max / 4);
            max = Math.ceil(max * 1.1 / passo) * passo;

            // grade e eixo Y
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            for (double v = 0; v <= max + 1e-6; v += passo) {
                int y = topo + h - (int) (v / max * h);
                g.setColor(GRADE);
                g.drawLine(esq, y, esq + w, y);
                g.setColor(TEXTO);
                String s = formato.formatar(v);
                g.drawString(s, esq - 8 - fm.stringWidth(s), y + 4);
            }

            // barras
            int n = valores.length;
            double larg = w / (double) n;
            int bw = Math.max(2, (int) (larg * 0.68));
            areas = new Rectangle[n];
            int saltoRotulo = Math.max(1, (int) Math.ceil(n / (w / 46.0)));
            for (int i = 0; i < n; i++) {
                int x = esq + (int) (i * larg + (larg - bw) / 2);
                int bh = (int) (valores[i] / max * h);
                int y = topo + h - bh;
                Color cor = AZUL;
                if (colorirPelaMeta && meta > 0) cor = valores[i] >= meta ? AZUL : AZUL_CLARO;
                g.setColor(cor);
                if (bh > 0) g.fillRoundRect(x, y, bw, bh + 4, 6, 6);
                areas[i] = new Rectangle(x, y, bw, bh);
                // valor em cima da barra, quando cabe
                if (valores[i] > 0 && bw >= 22) {
                    String s = formato.formatar(valores[i]);
                    g.setColor(TEXTO);
                    if (fm.stringWidth(s) <= larg) g.drawString(s, x + (bw - fm.stringWidth(s)) / 2, y - 4);
                }
                if (i % saltoRotulo == 0 && i < rotulos.length) {
                    String r = rotulos[i];
                    g.setColor(TEXTO);
                    g.drawString(r, x + (bw - fm.stringWidth(r)) / 2, topo + h + 18);
                }
            }
            g.setColor(new Color(0x9AA8B5));
            g.drawLine(esq, topo + h, esq + w, topo + h);

            // linha da meta
            if (meta > 0) {
                int y = topo + h - (int) (meta / max * h);
                g.setColor(LARANJA);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
                g.drawLine(esq, y, esq + w, y);
                g.setFont(TelaAlerta.fonte(Font.BOLD, 12));
                String s = rotuloMeta + ": " + formato.formatar(meta);
                int sw = g.getFontMetrics().stringWidth(s);
                g.setColor(new Color(255, 255, 255, 220));
                g.fillRect(esq + w - sw - 8, y - 18, sw + 8, 16);
                g.setColor(new Color(0xB86E00));
                g.drawString(s, esq + w - sw - 4, y - 5);
            }

            // legenda
            if (colorirPelaMeta && meta > 0) {
                int ly = getHeight() - 14;
                g.setFont(TelaAlerta.fonte(Font.PLAIN, 12));
                g.setColor(AZUL); g.fillRect(esq, ly - 10, 12, 12);
                g.setColor(TEXTO); g.drawString("Meta atingida", esq + 18, ly);
                g.setColor(AZUL_CLARO); g.fillRect(esq + 130, ly - 10, 12, 12);
                g.setColor(TEXTO); g.drawString("Abaixo da meta", esq + 148, ly);
            }
            g.dispose();
        }

        private static double passoBonito(double bruto) {
            if (bruto <= 0) return 1;
            double exp = Math.pow(10, Math.floor(Math.log10(bruto)));
            double f = bruto / exp;
            double nf = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
            return Math.max(1, nf * exp);
        }
    }

    /** Gráfico de rosca (donut) com legenda. */
    public static class Rosca extends JComponent {
        private String titulo = "";
        private String[] nomes = new String[0];
        private double[] valores = new double[0];
        private Color[] cores = new Color[0];
        private String centro = "";

        public Rosca() {
            setPreferredSize(new Dimension(760, 340));
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        public void definir(String titulo, String[] nomes, double[] valores, Color[] cores, String centro) {
            this.titulo = titulo; this.nomes = nomes; this.valores = valores; this.cores = cores; this.centro = centro;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(TelaAlerta.fonte(Font.BOLD, 15));
            g.setColor(TEXTO);
            g.drawString(titulo, 16, 24);

            double total = 0;
            for (double v : valores) total += v;
            int d = Math.min(getHeight() - 80, getWidth() / 2 - 40);
            if (d < 60) { g.dispose(); return; }
            int cx = 40, cy = 50;
            if (total <= 0) {
                g.setColor(GRADE);
                g.fill(new Ellipse2D.Double(cx, cy, d, d));
            } else {
                double ini = 90;
                for (int i = 0; i < valores.length; i++) {
                    double ang = valores[i] / total * 360;
                    g.setColor(cores[i]);
                    g.fill(new Arc2D.Double(cx, cy, d, d, ini, -ang, Arc2D.PIE));
                    ini -= ang;
                }
            }
            int furo = (int) (d * 0.58);
            g.setColor(getBackground());
            g.fill(new Ellipse2D.Double(cx + (d - furo) / 2.0, cy + (d - furo) / 2.0, furo, furo));
            g.setColor(TEXTO);
            g.setFont(TelaAlerta.fonte(Font.BOLD, 26));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(centro, cx + (d - fm.stringWidth(centro)) / 2, cy + d / 2 + 4);
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 12));
            fm = g.getFontMetrics();
            String sub = "dos alertas";
            g.drawString(sub, cx + (d - fm.stringWidth(sub)) / 2, cy + d / 2 + 24);

            // legenda
            int lx = cx + d + 50, ly = cy + 30;
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 14));
            for (int i = 0; i < nomes.length; i++) {
                g.setColor(cores[i]);
                g.fillRoundRect(lx, ly - 12, 16, 16, 4, 4);
                g.setColor(TEXTO);
                String pct = total > 0 ? String.format("%.0f%%", valores[i] / total * 100) : "0%";
                g.drawString(nomes[i] + ":  " + (int) valores[i] + "  (" + pct + ")", lx + 26, ly + 1);
                ly += 34;
            }
            g.dispose();
        }
    }

    /** Gráfico de linha (evolução no tempo) com linha de meta opcional. Valores <= 0 são ignorados. */
    public static class Linha extends JComponent {
        private String titulo = "";
        private String[] rotulos = new String[0];
        private double[] valores = new double[0];
        private double meta = -1;
        private Formato formato = new Formato() { public String formatar(double v) { return String.valueOf(v); } };
        private final java.util.List<Point> pontos = new java.util.ArrayList<Point>();

        public Linha() {
            setPreferredSize(new Dimension(700, 300));
            setBackground(Color.WHITE);
            setOpaque(true);
            setToolTipText("");
        }

        public void definir(String titulo, String[] rotulos, double[] valores, double meta, Formato f) {
            this.titulo = titulo; this.rotulos = rotulos; this.valores = valores; this.meta = meta; this.formato = f;
            repaint();
        }

        @Override public String getToolTipText(MouseEvent e) {
            for (int i = 0; i < pontos.size(); i++) {
                Point p = pontos.get(i);
                if (p != null && Math.abs(p.x - e.getX()) < 8 && Math.abs(p.y - e.getY()) < 12)
                    return rotulos[i] + ": " + formato.formatar(valores[i]);
            }
            return null;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(TelaAlerta.fonte(Font.BOLD, 15));
            g.setColor(TEXTO);
            g.drawString(titulo, 16, 24);
            pontos.clear();

            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            int validos = 0;
            for (double v : valores) if (v > 0) { min = Math.min(min, v); max = Math.max(max, v); validos++; }
            if (meta > 0 && validos > 0) { min = Math.min(min, meta); max = Math.max(max, meta); }
            if (validos == 0) {
                g.setFont(TelaAlerta.fonte(Font.PLAIN, 13));
                g.setColor(new Color(0x8899AA));
                g.drawString("Sem dados para este indicador. Cadastre medições para ver a evolução.", 16, 60);
                g.dispose();
                return;
            }
            double folga = Math.max((max - min) * 0.15, Math.max(0.5, max * 0.02));
            min -= folga; max += folga;
            int esq = 64, dir = 44, topo = 44, base = 40;
            int w = getWidth() - esq - dir, h = getHeight() - topo - base;
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            for (int k = 0; k <= 4; k++) {
                double v = min + (max - min) * k / 4;
                int y = topo + h - (int) ((v - min) / (max - min) * h);
                g.setColor(GRADE);
                g.drawLine(esq, y, esq + w, y);
                g.setColor(TEXTO);
                String sv = formato.formatar(v);
                g.drawString(sv, esq - 8 - fm.stringWidth(sv), y + 4);
            }
            int n = valores.length;
            double passo = n > 1 ? w / (double) (n - 1) : 0;
            if (meta > 0) {
                int y = topo + h - (int) ((meta - min) / (max - min) * h);
                g.setColor(LARANJA);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
                g.drawLine(esq, y, esq + w, y);
                g.setFont(TelaAlerta.fonte(Font.BOLD, 12));
                g.setColor(new Color(0xB86E00));
                String sm = "Meta: " + formato.formatar(meta);
                g.drawString(sm, esq + w - g.getFontMetrics().stringWidth(sm), y - 5);
            }
            java.awt.geom.Path2D linha = new java.awt.geom.Path2D.Double();
            boolean primeiro = true;
            for (int i = 0; i < n; i++) {
                if (valores[i] <= 0) { pontos.add(null); continue; }
                int x = esq + (n > 1 ? (int) (i * passo) : w / 2);
                int y = topo + h - (int) ((valores[i] - min) / (max - min) * h);
                pontos.add(new Point(x, y));
                if (primeiro) { linha.moveTo(x, y); primeiro = false; } else linha.lineTo(x, y);
            }
            g.setColor(AZUL);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(linha);
            int salto = Math.max(1, (int) Math.ceil(n / (w / 70.0)));
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
            for (int i = 0; i < n; i++) {
                Point p = pontos.get(i);
                if (p == null) continue;
                g.setColor(Color.WHITE);
                g.fillOval(p.x - 5, p.y - 5, 10, 10);
                g.setColor(AZUL);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(p.x - 5, p.y - 5, 10, 10);
                if (i % salto == 0 || i == n - 1) {
                    g.setColor(TEXTO);
                    String sv = formato.formatar(valores[i]);
                    g.drawString(sv, p.x - fm.stringWidth(sv) / 2, p.y - 10);
                    String r = rotulos[i];
                    g.drawString(r, p.x - fm.stringWidth(r) / 2, topo + h + 18);
                }
            }
            g.dispose();
        }
    }
}
