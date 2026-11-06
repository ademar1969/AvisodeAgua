package br.com.avisodeagua;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Régua de progressão da dieta: peso inicial -> peso atual -> peso meta,
 * régua do IMC e evolução de cada medida desde a primeira medição.
 */
public class ReguaProgresso extends JComponent {

    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color VERDE = new Color(0x2E9E5B);
    private static final Color LARANJA = new Color(0xD9822B);
    private static final Color TEXTO = new Color(0x33414E);
    private static final Color SUAVE = new Color(0x7A8896);
    private static final Color TRILHO = new Color(0xE4EAF0);

    private Usuario usuario;
    private List<Medida> medidas;

    public ReguaProgresso() {
        setBackground(Color.WHITE);
        setOpaque(true);
        setPreferredSize(new Dimension(760, 700));
    }

    public void definir(Usuario u, List<Medida> medidas) {
        this.usuario = u;
        this.medidas = medidas;
        revalidate();
        repaint();
    }

    public static BufferedImage imagem(Usuario u, List<Medida> m, int w, int h) {
        ReguaProgresso r = new ReguaProgresso();
        r.definir(u, m);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        r.desenhar(g, w);
        g.dispose();
        return img;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        desenhar(g, getWidth());
        g.dispose();
    }

    // ------------------------------------------------------------------ resumo em texto (também usado no e-mail)

    public static Medida primeiraComPeso(List<Medida> l) {
        for (Medida m : l) if (m.getPeso() > 0) return m;
        return null;
    }

    public static Medida ultimaComPeso(List<Medida> l) {
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i).getPeso() > 0) return l.get(i);
        return null;
    }

    /** Frase com o progresso do peso, ou "" se não houver dados. */
    public static String resumoPeso(Usuario u, List<Medida> l) {
        Medida ini = primeiraComPeso(l), at = ultimaComPeso(l);
        if (ini == null || at == null) return "";
        double meta = u.getPesoMetaKg();
        double dif = at.getPeso() - ini.getPeso();
        StringBuilder sb = new StringBuilder();
        if (Math.abs(dif) < 0.05) sb.append("Peso estável desde o início (").append(Medida.fmt(at.getPeso())).append(" kg).");
        else sb.append(dif < 0 ? "Perdeu " : "Ganhou ").append(Medida.fmt(Math.abs(dif))).append(" kg desde ")
                .append(ini.getData().format(DM)).append(" (").append(Medida.fmt(ini.getPeso())).append(" → ")
                .append(Medida.fmt(at.getPeso())).append(" kg).");
        if (meta > 0) {
            double total = meta - ini.getPeso();
            double feito = at.getPeso() - ini.getPeso();
            double pct = Math.abs(total) < 0.05 ? 100 : Math.max(0, Math.min(100, feito / total * 100));
            double falta = meta - at.getPeso();
            sb.append(" Meta: ").append(Medida.fmt(meta)).append(" kg — ").append(String.format("%.0f", pct)).append("% do caminho");
            if (Math.abs(falta) >= 0.05 && Math.signum(falta) == Math.signum(total)) {
                sb.append(", faltam ").append(Medida.fmt(Math.abs(falta))).append(" kg.");
                String prev = previsao(ini, at, meta);
                if (!prev.isEmpty()) sb.append(" ").append(prev);
            } else {
                sb.append(". Meta alcançada!");
            }
        }
        return sb.toString();
    }

    /** Previsão de quando chega na meta mantendo o ritmo atual. */
    static String previsao(Medida ini, Medida at, double meta) {
        long dias = ChronoUnit.DAYS.between(ini.getData(), at.getData());
        if (dias < 7) return "";
        double porDia = (at.getPeso() - ini.getPeso()) / dias;
        double falta = meta - at.getPeso();
        if (Math.abs(porDia) < 1e-4 || Math.signum(porDia) != Math.signum(falta)) return "";
        long diasFalta = (long) Math.ceil(falta / porDia);
        if (diasFalta > 3650) return "";
        LocalDate quando = at.getData().plusDays(diasFalta);
        return "Ritmo: " + Medida.fmt(Math.abs(porDia * 7)) + " kg por semana; neste ritmo chega na meta por volta de "
                + quando.format(DM) + ".";
    }

    // ------------------------------------------------------------------ desenho

    void desenhar(Graphics2D g, int w) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (usuario == null || medidas == null || medidas.isEmpty()) {
            g.setColor(TEXTO);
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 14));
            g.drawString("Cadastre a primeira medição para começar a acompanhar o progresso.", 20, 40);
            return;
        }
        int y = 28;
        y = reguaPeso(g, w, y);
        y = reguaImc(g, w, y + 24);
        evolucaoMedidas(g, w, y + 24);
    }

    private int reguaPeso(Graphics2D g, int w, int y) {
        g.setFont(TelaAlerta.fonte(Font.BOLD, 16));
        g.setColor(TEXTO);
        g.drawString("Régua de progresso do peso", 20, y);
        Medida ini = primeiraComPeso(medidas), at = ultimaComPeso(medidas);
        if (ini == null) {
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 13));
            g.setColor(SUAVE);
            g.drawString("Informe o peso nas medições para ver a régua.", 20, y + 28);
            return y + 40;
        }
        double meta = usuario.getPesoMetaKg();
        double a = ini.getPeso(), b = meta > 0 ? meta : at.getPeso();
        if (Math.abs(b - a) < 0.5) b = a + (meta > 0 ? (meta >= a ? 0.5 : -0.5) : -1);
        double lo = Math.min(a, b), hi = Math.max(a, b);
        // a régua vai do início (esquerda) para a meta (direita)
        int x1 = 60, x2 = w - 60, yr = y + 62;
        double faixa = hi - lo;
        final double fa = a, fb = b;
        java.util.function.DoubleToIntFunction px = v -> (int) Math.round(x1 + (v - fa) / (fb - fa) * (x2 - x1));

        // trilho
        g.setColor(TRILHO);
        g.fill(new RoundRectangle2D.Double(x1, yr - 7, x2 - x1, 14, 14, 14));
        // progresso
        double atual = Math.max(lo, Math.min(hi, at.getPeso()));
        int xa = px.applyAsInt(atual);
        boolean bom = meta <= 0 || Math.signum(at.getPeso() - a) == Math.signum(b - a) || Math.abs(at.getPeso() - a) < 0.05;
        g.setColor(bom ? VERDE : LARANJA);
        g.fill(new RoundRectangle2D.Double(Math.min(x1, xa), yr - 7, Math.max(14, Math.abs(xa - x1)), 14, 14, 14));

        // marcas a cada 1 kg (ou 2 / 5 se a faixa for grande)
        double passo = faixa <= 12 ? 1 : faixa <= 30 ? 2 : 5;
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics();
        for (double v = Math.ceil(lo / passo) * passo; v <= hi + 1e-6; v += passo) {
            int x = px.applyAsInt(v);
            g.setColor(new Color(0xA8B6C4));
            g.draw(new Line2D.Double(x, yr + 10, x, yr + 16));
            String s = String.valueOf((int) Math.round(v));
            g.setColor(SUAVE);
            g.drawString(s, x - fm.stringWidth(s) / 2, yr + 28);
        }

        // extremidades
        g.setFont(TelaAlerta.fonte(Font.BOLD, 12));
        g.setColor(TEXTO);
        g.drawString("Início: " + Medida.fmt(a) + " kg", x1 - 10, yr - 32);
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
        g.setColor(SUAVE);
        g.drawString(ini.getData().format(DM), x1 - 10, yr - 18);
        String fim = meta > 0 ? "Meta: " + Medida.fmt(meta) + " kg" : "Sem meta definida";
        g.setFont(TelaAlerta.fonte(Font.BOLD, 12));
        g.setColor(meta > 0 ? new Color(0xB86E00) : SUAVE);
        int fw = g.getFontMetrics().stringWidth(fim);
        g.drawString(fim, x2 + 10 - fw, yr - 32);

        // marcador de hoje
        Path2D tri = new Path2D.Double();
        tri.moveTo(xa, yr + 9);
        tri.lineTo(xa - 8, yr + 22);
        tri.lineTo(xa + 8, yr + 22);
        tri.closePath();
        g.setColor(new Color(0x0B4F75));
        g.fillOval(xa - 9, yr - 9, 18, 18);
        g.setColor(Color.WHITE);
        g.fillOval(xa - 4, yr - 4, 8, 8);
        String hoje = "Atual: " + Medida.fmt(at.getPeso()) + " kg";
        g.setFont(TelaAlerta.fonte(Font.BOLD, 13));
        int hw = g.getFontMetrics().stringWidth(hoje);
        int hx = Math.max(x1 + 120, Math.min(x2 - 130 - hw, xa - hw / 2));
        g.setColor(new Color(0x0B4F75));
        g.drawString(hoje, hx, yr - 14);

        // frase de resumo
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 13));
        g.setColor(TEXTO);
        String resumo = resumoPeso(usuario, medidas);
        if (meta <= 0) resumo += "  Defina o peso meta na aba \"Perfil e metas\".";
        return quebrar(g, resumo, 20, yr + 52, w - 40) + 4;
    }

    private int reguaImc(Graphics2D g, int w, int y) {
        g.setFont(TelaAlerta.fonte(Font.BOLD, 16));
        g.setColor(TEXTO);
        g.drawString("Régua do IMC", 20, y);
        Medida ini = primeiraComPeso(medidas), at = ultimaComPeso(medidas);
        double h = usuario.getAlturaCm();
        if (at == null || h <= 0) {
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 13));
            g.setColor(SUAVE);
            g.drawString("Informe a altura em \"Perfil e metas\" e o peso nas medições para calcular o IMC.", 20, y + 26);
            return y + 36;
        }
        double[] lim = {15, 18.5, 25, 30, 35, 40};
        Color[] cores = {new Color(0x7FB8E6), new Color(0x58B87A), new Color(0xF2C94C), new Color(0xF2994A), new Color(0xE05A4F)};
        String[] nomes = {"Abaixo", "Normal", "Sobrepeso", "Obesidade I", "Obesidade II+"};
        int x1 = 60, x2 = w - 60, yr = y + 30;
        java.util.function.DoubleToIntFunction px = v -> (int) Math.round(x1 + (Math.max(15, Math.min(40, v)) - 15) / 25.0 * (x2 - x1));
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < 5; i++) {
            int a = px.applyAsInt(lim[i]), b = px.applyAsInt(lim[i + 1]);
            g.setColor(cores[i]);
            g.fillRect(a, yr, b - a, 12);
            g.setColor(SUAVE);
            g.drawString(nomes[i], a + (b - a - fm.stringWidth(nomes[i])) / 2, yr + 26);
            if (i > 0) {
                String s = Medida.fmt(lim[i]);
                g.drawString(s, a - fm.stringWidth(s) / 2, yr - 4);
            }
        }
        double imcIni = ini.imc(h), imcAt = at.imc(h);
        if (ini != at && Math.abs(imcIni - imcAt) > 0.05) {
            int xi = px.applyAsInt(imcIni);
            g.setColor(new Color(0x9AA8B5));
            g.fillOval(xi - 5, yr + 1, 10, 10);
        }
        int xa = px.applyAsInt(imcAt);
        g.setColor(new Color(0x0B4F75));
        g.fillOval(xa - 8, yr - 2, 16, 16);
        g.setColor(Color.WHITE);
        g.fillOval(xa - 3, yr + 3, 6, 6);
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 13));
        g.setColor(TEXTO);
        String txt = "IMC atual: " + Medida.fmt(imcAt) + " (" + Medida.classificarImc(imcAt) + ")";
        if (ini != at) txt += "   •   no início: " + Medida.fmt(imcIni);
        double pMin = 18.5 * Math.pow(h / 100, 2), pMax = 24.9 * Math.pow(h / 100, 2);
        txt += "   •   Peso saudável para " + Medida.fmt(h) + " cm: " + Medida.fmt(pMin) + " a " + Medida.fmt(pMax) + " kg";
        return quebrar(g, txt, 20, yr + 50, w - 40);
    }

    private int evolucaoMedidas(Graphics2D g, int w, int y) {
        g.setFont(TelaAlerta.fonte(Font.BOLD, 16));
        g.setColor(TEXTO);
        g.drawString("Evolução das medidas desde a primeira medição", 20, y);
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
        g.setColor(SUAVE);
        g.drawString("Verde = diminuiu   •   Laranja = aumentou   (em cm)", 20, y + 16);
        double maxAbs = 1;
        double[] dif = new double[Medida.COLUNAS.length];
        double[][] ext = new double[Medida.COLUNAS.length][];
        for (int i = 0; i < Medida.COLUNAS.length; i++) {
            Medida p = null, u = null;
            for (Medida m : medidas) if (m.get(i) > 0) { if (p == null) p = m; u = m; }
            if (p != null && u != null && p != u) {
                dif[i] = u.get(i) - p.get(i);
                ext[i] = new double[]{p.get(i), u.get(i)};
                maxAbs = Math.max(maxAbs, Math.abs(dif[i]));
            }
        }
        int cx = w / 2 + 20, larg = (w - 40) / 2 - 150;
        int yl = y + 40;
        g.setColor(new Color(0xA8B6C4));
        g.drawLine(cx, yl - 12, cx, yl + Medida.COLUNAS.length * 26 - 8);
        for (int i = 0; i < Medida.COLUNAS.length; i++) {
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 12));
            g.setColor(TEXTO);
            g.drawString(Medida.NOMES[i], 20, yl + 4);
            if (ext[i] == null) {
                g.setColor(new Color(0xB0BCC8));
                g.drawString("—", cx + 8, yl + 4);
            } else {
                int bw = (int) Math.round(Math.abs(dif[i]) / maxAbs * larg);
                boolean desceu = dif[i] < 0;
                g.setColor(Math.abs(dif[i]) < 0.05 ? new Color(0xB0BCC8) : desceu ? VERDE : LARANJA);
                if (desceu) g.fillRoundRect(cx - bw, yl - 8, Math.max(bw, 2), 14, 6, 6);
                else g.fillRoundRect(cx, yl - 8, Math.max(bw, 2), 14, 6, 6);
                String s = (dif[i] > 0 ? "+" : "") + Medida.fmt(dif[i]) + "  (" + Medida.fmt(ext[i][0]) + " → " + Medida.fmt(ext[i][1]) + ")";
                g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
                g.setColor(TEXTO);
                int sw = g.getFontMetrics().stringWidth(s);
                if (desceu) g.drawString(s, cx + 8, yl + 4);
                else g.drawString(s, cx - 8 - sw, yl + 4);
            }
            yl += 26;
        }
        return yl;
    }

    private static int quebrar(Graphics2D g, String txt, int x, int y, int larg) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder linha = new StringBuilder();
        for (String p : txt.split(" ")) {
            if (fm.stringWidth(linha + p) > larg && linha.length() > 0) {
                g.drawString(linha.toString(), x, y);
                y += fm.getHeight();
                linha.setLength(0);
            }
            linha.append(p).append(' ');
        }
        if (linha.length() > 0) { g.drawString(linha.toString(), x, y); y += fm.getHeight(); }
        return y;
    }
}
