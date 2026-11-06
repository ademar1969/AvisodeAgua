package br.com.avisodeagua;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Desenho aproximado do corpo humano (vista de frente) gerado a partir das medidas.
 * As larguras saem das circunferências (largura ≈ circunferência / π para braços e pernas,
 * e uma proporção típica para o tronco). Medidas não informadas usam valores médios.
 */
public class DesenhoCorpo extends JComponent {

    private static final Color PELE_1 = new Color(0xF2DCCB);
    private static final Color PELE_2 = new Color(0xE2BBA0);
    private static final Color CONTORNO = new Color(0x8A6450);
    private static final Color FITA = new Color(0x1E90DC);
    private static final Color COMPARA = new Color(0x2E6FD0);
    private static final Color TEXTO = new Color(0x33414E);
    private static final Color DESCEU = new Color(0x2E9E5B);
    private static final Color SUBIU = new Color(0xD9822B);

    private Usuario usuario;
    private Medida atual;
    private Medida comparacao;
    private String rotuloComparacao = "";

    public DesenhoCorpo() {
        setPreferredSize(new Dimension(560, 640));
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void definir(Usuario u, Medida atual, Medida comparacao, String rotuloComparacao) {
        this.usuario = u;
        this.atual = atual;
        this.comparacao = comparacao;
        this.rotuloComparacao = rotuloComparacao == null ? "" : rotuloComparacao;
        repaint();
    }

    /** Gera o desenho como imagem (usado no e-mail). */
    public static BufferedImage imagem(Usuario u, Medida atual, Medida comp, String rot, int w, int h) {
        DesenhoCorpo d = new DesenhoCorpo();
        d.definir(u, atual, comp, rot);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        d.desenhar(g, w, h);
        g.dispose();
        return img;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        desenhar(g, getWidth(), getHeight());
        g.dispose();
    }

    // ------------------------------------------------------------------ geometria (em cm)

    /** Formas do corpo em centímetros: x a partir do centro, y a partir do topo da cabeça. */
    private static class Geo {
        Path2D corpo, bracoDir, bracoEsq;
        Ellipse2D cabeca;
        double altura, maxX;
        /** Para cada medida: y da fita, x esquerdo e x direito da fita (cm). */
        double[] fitaY = new double[Medida.COLUNAS.length];
        double[] fitaX1 = new double[Medida.COLUNAS.length];
        double[] fitaX2 = new double[Medida.COLUNAS.length];
    }

    private static Geo geometria(Usuario u, Medida m) {
        boolean fem = u.isFeminino();
        double A = u.getAlturaCm() > 100 ? u.getAlturaCm() : (fem ? 162 : 173);
        Geo g = new Geo();
        g.altura = A;
        double pi = Math.PI;

        double headW = m.valorDesenho(Medida.CABECA, fem) / 3.6;
        double headH = A * 0.125;
        double neckR = m.valorDesenho(Medida.PESCOCO, fem) / pi / 2;
        double torax = m.valorDesenho(Medida.TORAX, fem);
        double shoulderHalf = torax / 2.3 / 2 * (fem ? 0.92 : 1.0);
        double chestHalf = torax / 2.9 / 2;
        double waistHalf = m.valorDesenho(Medida.CINTURA, fem) / 3.0 / 2;
        double bellyHalf = m.valorDesenho(Medida.BARRIGA, fem) / 3.0 / 2;
        double hipHalf = m.valorDesenho(Medida.QUADRIL, fem) / 2.85 / 2;
        double thighR = m.valorDesenho(Medida.COXA, fem) / pi / 2;
        double pant = m.valorDesenho(Medida.PANTURRILHA, fem);
        double kneeR = pant * 0.95 / pi / 2;
        double calfR = pant / pi / 2;
        double ankleR = pant * 0.55 / pi / 2;
        double footW = m.valorDesenho(Medida.PE, fem) * 0.38;
        double upperR = m.valorDesenho(Medida.BRACO, fem) / pi / 2;
        double ante = m.valorDesenho(Medida.ANTEBRACO, fem);
        double elbowR = ante * 0.8 / pi / 2;
        double foreR = ante / pi / 2;
        double wristR = ante * 0.58 / pi / 2;
        double handHalf = m.valorDesenho(Medida.MAO, fem) / 2.5 / 2;

        double yNeckTop = A * 0.118, yNeckBase = A * 0.16, yShoulder = A * 0.176, yDeltoid = A * 0.196;
        double yArmpit = A * 0.232, yChest = A * 0.262, yWaist = A * (fem ? 0.372 : 0.385);
        double yBelly = A * 0.425, yHip = A * 0.50, yCrotch = A * 0.522, yThigh = A * 0.565;
        double yKnee = A * 0.72, yCalf = A * 0.80, yAnkle = A * 0.945;

        double legCx = Math.max(thighR + 0.4, hipHalf - thighR * 0.95);
        double kneeCx = Math.max(kneeR + 0.7, legCx * 0.8);
        double calfCx = Math.max(calfR + 0.6, legCx * 0.76);
        double ankleCx = Math.max(ankleR + 1.0, legCx * 0.72);

        // contorno do lado direito: do pescoço até o pé e de volta pela parte interna da perna
        double[][] dir = {
                {neckR, yNeckTop},
                {neckR * 1.06, yNeckBase - A * 0.006},
                {shoulderHalf * 0.68, yShoulder - A * 0.002},
                {shoulderHalf, yDeltoid},
                {chestHalf * 1.03, yArmpit},
                {chestHalf, yChest},
                {(chestHalf + waistHalf) / 2 * 0.99, (yChest + yWaist) / 2},
                {waistHalf, yWaist},
                {bellyHalf, yBelly},
                {hipHalf, yHip},
                {Math.max(hipHalf * 0.98, legCx + thighR), yThigh},
                {kneeCx + kneeR, yKnee},
                {calfCx + calfR, yCalf},
                {ankleCx + ankleR, yAnkle},
                {ankleCx + footW * 0.55, A * 0.985},
                {ankleCx + footW * 0.45, A},
                {ankleCx - footW * 0.40, A},
                {ankleCx - footW * 0.45, A * 0.985},
                {ankleCx - ankleR, yAnkle},
                {calfCx - calfR * 0.78, yCalf},
                {kneeCx - kneeR * 0.9, yKnee},
                {legCx - thighR * 0.92, yThigh},
                {0.5, yCrotch}};
        List<Point2D> pts = new ArrayList<Point2D>();
        for (double[] p : dir) pts.add(new Point2D.Double(p[0], p[1]));
        for (int i = dir.length - 1; i >= 0; i--) pts.add(new Point2D.Double(-dir[i][0], dir[i][1]));
        g.corpo = curva(pts, true);

        // braços: linha central com raio em cada ponto
        double x0 = shoulderHalf - upperR * 0.55;
        double xElbow = Math.max(x0 + 1.5, Math.max(waistHalf, chestHalf * 0.95) + elbowR + 1.4);
        double xWrist = Math.max(xElbow + 2.0, hipHalf + wristR + 1.2);
        double xHand = xWrist + 0.6;
        double[][] centro = {
                {x0, yDeltoid, upperR * 1.08},
                {(x0 + xElbow) / 2 + 0.3, A * 0.275, upperR},
                {xElbow, A * 0.385, elbowR},
                {(xElbow + xWrist) / 2, A * 0.432, foreR},
                {xWrist, A * 0.49, wristR},
                {xHand, A * 0.53, handHalf},
                {xHand + 0.2, A * 0.566, handHalf * 0.92}};
        g.bracoDir = braco(centro, x0, yShoulder, yArmpit, upperR, A, 1);
        g.bracoEsq = braco(centro, x0, yShoulder, yArmpit, upperR, A, -1);

        g.cabeca = new Ellipse2D.Double(-headW / 2, 0, headW, headH);

        // fitas de medida (lado esquerdo da figura = tronco; lado direito = braços e pernas)
        fita(g, Medida.CABECA, headH * 0.42, -headW / 2, headW / 2);
        fita(g, Medida.PESCOCO, (yNeckTop + yNeckBase) / 2, -neckR, neckR);
        fita(g, Medida.TORAX, yChest, -chestHalf, chestHalf);
        fita(g, Medida.CINTURA, yWaist, -waistHalf, waistHalf);
        fita(g, Medida.BARRIGA, yBelly, -bellyHalf, bellyHalf);
        fita(g, Medida.QUADRIL, yHip, -hipHalf, hipHalf);
        double xu = centro[1][0];
        fita(g, Medida.BRACO, A * 0.275, xu - upperR, xu + upperR);
        double xf = centro[3][0];
        fita(g, Medida.ANTEBRACO, A * 0.432, xf - foreR, xf + foreR);
        fita(g, Medida.MAO, A * 0.535, xHand - handHalf, xHand + handHalf);
        fita(g, Medida.COXA, yThigh, legCx - thighR * 0.92, Math.max(hipHalf * 0.98, legCx + thighR));
        fita(g, Medida.PANTURRILHA, yCalf, calfCx - calfR * 0.78, calfCx + calfR);
        fita(g, Medida.PE, A * 0.994, ankleCx - footW * 0.42, ankleCx + footW * 0.5);

        g.maxX = xHand + handHalf + 1;
        return g;
    }

    private static void fita(Geo g, int i, double y, double x1, double x2) {
        g.fitaY[i] = y; g.fitaX1[i] = x1; g.fitaX2[i] = x2;
    }

    private static Path2D braco(double[][] c, double x0, double yShoulder, double yArmpit,
                                double upperR, double A, int lado) {
        int n = c.length;
        List<Point2D> fora = new ArrayList<Point2D>();
        List<Point2D> dentro = new ArrayList<Point2D>();
        for (int i = 0; i < n; i++) {
            double[] a = c[Math.max(0, i - 1)], b = c[Math.min(n - 1, i + 1)];
            double dx = b[0] - a[0], dy = b[1] - a[1], len = Math.hypot(dx, dy);
            double nx = dy / len, ny = -dx / len; // normal para fora (lado direito)
            fora.add(new Point2D.Double(c[i][0] + nx * c[i][2], c[i][1] + ny * c[i][2]));
            dentro.add(new Point2D.Double(c[i][0] - nx * c[i][2], c[i][1] - ny * c[i][2]));
        }
        List<Point2D> pts = new ArrayList<Point2D>();
        pts.add(new Point2D.Double(x0 - upperR * 0.3, yShoulder));
        pts.addAll(fora);
        double[] ult = c[n - 1];
        pts.add(new Point2D.Double(ult[0] + 0.3, ult[1] + A * 0.028)); // ponta dos dedos
        for (int i = n - 1; i >= 1; i--) pts.add(dentro.get(i));
        pts.add(new Point2D.Double(x0 - upperR * 0.9, yArmpit));
        if (lado < 0) for (Point2D p : pts) p.setLocation(-p.getX(), p.getY());
        return curva(pts, true);
    }

    /** Curva suave (Catmull-Rom convertida em Bézier) passando pelos pontos. */
    private static Path2D curva(List<Point2D> p, boolean fechar) {
        Path2D path = new Path2D.Double();
        int n = p.size();
        path.moveTo(p.get(0).getX(), p.get(0).getY());
        for (int i = 0; i < n - 1; i++) {
            Point2D p0 = p.get(Math.max(0, i - 1)), p1 = p.get(i), p2 = p.get(i + 1), p3 = p.get(Math.min(n - 1, i + 2));
            double c1x = p1.getX() + (p2.getX() - p0.getX()) / 6, c1y = p1.getY() + (p2.getY() - p0.getY()) / 6;
            double c2x = p2.getX() - (p3.getX() - p1.getX()) / 6, c2y = p2.getY() - (p3.getY() - p1.getY()) / 6;
            path.curveTo(c1x, c1y, c2x, c2y, p2.getX(), p2.getY());
        }
        if (fechar) path.closePath();
        return path;
    }

    // ------------------------------------------------------------------ desenho

    void desenhar(Graphics2D g, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (usuario == null || atual == null) {
            g.setColor(TEXTO);
            g.drawString("Informe as medidas para gerar o desenho.", 20, 30);
            return;
        }
        Geo geo = geometria(usuario, atual);
        Geo geoComp = comparacao != null ? geometria(usuario, comparacao) : null;

        int colunaRotulo = Math.max(110, Math.min(150, w / 4));
        int topo = 46, base = 46;
        double areaH = h - topo - base;
        double maxX = geo.maxX;
        if (geoComp != null) maxX = Math.max(maxX, geoComp.maxX);
        double s = Math.min(areaH / geo.altura, (w / 2.0 - colunaRotulo - 10) / maxX);
        double cx = w / 2.0;
        double oy = topo + (areaH - geo.altura * s) / 2;

        // título
        g.setFont(TelaAlerta.fonte(Font.BOLD, 14));
        g.setColor(TEXTO);
        String titulo = "Corpo aproximado — " + atual.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        g.drawString(titulo, 12, 22);
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
        g.setColor(new Color(0x7A8896));
        g.drawString("Desenho gerado pelas medidas (proporções aproximadas)", 12, 37);

        AffineTransform at = new AffineTransform();
        at.translate(cx, oy);
        at.scale(s, s);

        Shape corpo = at.createTransformedShape(geo.corpo);
        Shape bd = at.createTransformedShape(geo.bracoDir);
        Shape be = at.createTransformedShape(geo.bracoEsq);
        Shape cab = at.createTransformedShape(geo.cabeca);
        Rectangle2D bb = corpo.getBounds2D();
        GradientPaint pele = new GradientPaint((float) bb.getMinX(), 0, PELE_2, (float) cx, 0, PELE_1, true);

        g.setStroke(new BasicStroke(1.4f));
        for (Shape sh : new Shape[]{bd, be, corpo, cab}) {
            g.setPaint(pele);
            g.fill(sh);
            g.setColor(CONTORNO);
            g.draw(sh);
        }
        // traços leves: separação das pernas e umbigo
        g.setColor(new Color(138, 100, 80, 120));
        double yUmb = geo.fitaY[Medida.BARRIGA] * s + oy;
        g.fill(new Ellipse2D.Double(cx - 1.6, yUmb - 1.6, 3.2, 3.2));

        // contorno de comparação (tracejado)
        if (geoComp != null) {
            g.setColor(new Color(COMPARA.getRed(), COMPARA.getGreen(), COMPARA.getBlue(), 170));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 5f}, 0f));
            for (Shape sh : new Shape[]{geoComp.corpo, geoComp.bracoDir, geoComp.bracoEsq, geoComp.cabeca}) {
                g.draw(at.createTransformedShape(sh));
            }
        }

        // fitas e rótulos
        List<Rotulo> esquerda = new ArrayList<Rotulo>();
        List<Rotulo> direita = new ArrayList<Rotulo>();
        for (int i = 0; i < Medida.COLUNAS.length; i++) {
            double y = oy + geo.fitaY[i] * s;
            double x1 = cx + geo.fitaX1[i] * s, x2 = cx + geo.fitaX2[i] * s;
            boolean informado = atual.get(i) > 0;
            g.setColor(informado ? FITA : new Color(0xB0BCC8));
            g.setStroke(new BasicStroke(informado ? 2f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{4f, 3f}, 0f));
            g.draw(new Line2D.Double(x1, y, x2, y));
            boolean ladoEsq = i <= Medida.QUADRIL;
            Rotulo r = new Rotulo(i, ladoEsq ? x1 : x2, y);
            (ladoEsq ? esquerda : direita).add(r);
        }
        desenharRotulos(g, esquerda, true, colunaRotulo, w, topo, h - base);
        desenharRotulos(g, direita, false, colunaRotulo, w, topo, h - base);

        // rodapé: indicadores
        g.setFont(TelaAlerta.fonte(Font.PLAIN, 12));
        g.setColor(TEXTO);
        StringBuilder sb = new StringBuilder();
        if (atual.getPeso() > 0) sb.append("Peso: ").append(Medida.fmt(atual.getPeso())).append(" kg");
        double imc = atual.imc(usuario.getAlturaCm());
        if (imc > 0) sb.append("   IMC: ").append(Medida.fmt(imc)).append(" (").append(Medida.classificarImc(imc)).append(")");
        double gord = atual.gorduraEstimada(usuario);
        if (gord > 0) sb.append("   Gordura estimada: ").append(Medida.fmt(gord)).append("%");
        g.drawString(sb.toString(), 12, h - 24);
        if (geoComp != null) {
            g.setColor(COMPARA);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{6f, 5f}, 0f));
            g.drawLine(12, h - 9, 40, h - 9);
            g.setColor(TEXTO);
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
            g.drawString("Tracejado = " + rotuloComparacao, 46, h - 5);
        }
    }

    private static class Rotulo {
        final int i; final double ax, ay; double y;
        Rotulo(int i, double ax, double ay) { this.i = i; this.ax = ax; this.ay = ay; this.y = ay; }
    }

    private void desenharRotulos(Graphics2D g, List<Rotulo> lista, boolean esq, int col, int w, int min, int max) {
        int alto = 40;
        // evita sobreposição: empurra para baixo e depois para cima se passou do limite
        for (int k = 0; k < lista.size(); k++) {
            Rotulo r = lista.get(k);
            r.y = Math.max(r.ay, k == 0 ? min + 14 : lista.get(k - 1).y + alto);
        }
        for (int k = lista.size() - 1; k >= 0; k--) {
            Rotulo r = lista.get(k);
            double lim = k == lista.size() - 1 ? max - 18 : lista.get(k + 1).y - alto;
            if (r.y > lim) r.y = lim;
        }
        for (Rotulo r : lista) {
            int x = esq ? 8 : w - col + 4;
            int xl = esq ? col - 4 : w - col;
            g.setColor(new Color(0xA8B6C4));
            g.setStroke(new BasicStroke(1f));
            g.draw(new Line2D.Double(r.ax, r.ay, esq ? xl : xl, r.y));
            g.fill(new Ellipse2D.Double(r.ax - 2.5, r.ay - 2.5, 5, 5));

            double v = atual.get(r.i);
            g.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
            g.setColor(new Color(0x667788));
            drawAlinhado(g, Medida.NOMES[r.i], x, (int) r.y - 6, col - 12, esq);
            g.setFont(TelaAlerta.fonte(Font.BOLD, 13));
            g.setColor(v > 0 ? TEXTO : new Color(0xA0AAB4));
            String val = v > 0 ? Medida.fmt(v) + " cm" : "—";
            String delta = "";
            Color corDelta = TEXTO;
            if (v > 0 && comparacao != null && comparacao.get(r.i) > 0) {
                double d = v - comparacao.get(r.i);
                if (Math.abs(d) >= 0.05) {
                    delta = (d > 0 ? "+" : "") + Medida.fmt(d);
                    corDelta = d < 0 ? DESCEU : SUBIU;
                }
            }
            FontMetrics fm = g.getFontMetrics();
            int wVal = fm.stringWidth(val);
            g.setFont(TelaAlerta.fonte(Font.BOLD, 11));
            int wDel = delta.isEmpty() ? 0 : g.getFontMetrics().stringWidth(delta) + 6;
            int total = wVal + wDel;
            int xv = esq ? x + col - 12 - total : x;
            g.setFont(TelaAlerta.fonte(Font.BOLD, 13));
            g.setColor(v > 0 ? TEXTO : new Color(0xA0AAB4));
            g.drawString(val, xv, (int) r.y + 10);
            if (!delta.isEmpty()) {
                g.setFont(TelaAlerta.fonte(Font.BOLD, 11));
                g.setColor(corDelta);
                g.drawString(delta, xv + wVal + 6, (int) r.y + 10);
            }
        }
    }

    private static void drawAlinhado(Graphics2D g, String s, int x, int y, int larg, boolean direita) {
        int sw = g.getFontMetrics().stringWidth(s);
        g.drawString(s, direita ? x + larg - sw : x, y);
    }
}
