package br.com.avisodeagua;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Tela de alerta: abre por cima de tudo com a imagem de uma cachoeira
 * e a mensagem "Hora de tomar água".
 * Para usar uma foto sua, salve-a como %APPDATA%\AvisodeAgua\cachoeira.jpg (ou .png).
 */
public class TelaAlerta extends JFrame {

    public interface Resposta {
        void bebeu();
        void adiar(int minutos);
        void ignorou();
    }

    private static BufferedImage imagemCache;
    private boolean respondido;

    public TelaAlerta(Usuario u, int coposHoje, int mlHoje, Resposta resposta) {
        super("Hora de tomar água!");
        setUndecorated(true);
        setAlwaysOnTop(true);
        setIconImage(Icones.gota(64));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Rectangle tela = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int w = Math.min(1100, (int) (tela.width * 0.8));
        int h = Math.min(650, (int) (tela.height * 0.8));
        setSize(w, h);
        setLocationRelativeTo(null);
        try { setShape(new RoundRectangle2D.Double(0, 0, w, h, 30, 30)); } catch (Exception ignored) { }

        BufferedImage img = imagem();
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String primeiroNome = u.getNome().trim().split("\\s+")[0];

        JPanel fundo = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (img != null) {
                    double esc = Math.max(getWidth() / (double) img.getWidth(), getHeight() / (double) img.getHeight());
                    int iw = (int) (img.getWidth() * esc), ih = (int) (img.getHeight() * esc);
                    g.drawImage(img, (getWidth() - iw) / 2, (getHeight() - ih) / 2, iw, ih, null);
                } else {
                    g.setPaint(new GradientPaint(0, 0, new Color(0x4FB3E8), 0, getHeight(), new Color(0x0B5E8A)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                // faixa escura no topo para legibilidade do texto
                g.setPaint(new GradientPaint(0, 0, new Color(0, 30, 60, 190), 0, getHeight() * 0.55f, new Color(0, 30, 60, 0)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(new GradientPaint(0, getHeight() * 0.6f, new Color(0, 30, 60, 0), 0, getHeight(), new Color(0, 30, 60, 200)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        fundo.setBorder(BorderFactory.createEmptyBorder(40, 40, 36, 40));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.add(rotulo(hora, 34, Font.PLAIN, new Color(255, 255, 255, 220)));
        textos.add(Box.createVerticalStrut(4));
        JLabel titulo = rotulo("Hora de tomar água!", 56, Font.BOLD, Color.WHITE);
        titulo.setIcon(new ImageIcon(Icones.gota(64)));
        titulo.setIconTextGap(14);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(10));
        textos.add(rotulo(primeiroNome + ", pare um minutinho e beba um copo de água (" + u.getCopoMl() + " ml).", 24, Font.PLAIN, Color.WHITE));
        fundo.add(textos, BorderLayout.NORTH);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        int metaMl = u.getMetaMl();
        int falta = Math.max(0, metaMl - mlHoje);
        JPanel prog = new JPanel();
        prog.setOpaque(false);
        prog.setLayout(new BoxLayout(prog, BoxLayout.Y_AXIS));
        prog.add(rotulo("Hoje: " + Usuario.formatarMl(mlHoje) + " de " + Usuario.formatarMl(metaMl)
                + "  (" + coposHoje + " de " + u.getMetaCopos() + " copos)", 20, Font.BOLD, Color.WHITE));
        prog.add(Box.createVerticalStrut(6));
        final double frac = metaMl > 0 ? Math.min(1.0, mlHoje / (double) metaMl) : 0;
        JComponent barra = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(255, 255, 255, 90));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.setColor(frac >= 1 ? new Color(0x3CCB7F) : new Color(0x5EC4FF));
                int w = (int) (getWidth() * frac);
                if (w > 0) g.fillRoundRect(0, 0, Math.max(w, getHeight()), getHeight(), getHeight(), getHeight());
                g.dispose();
            }
        };
        barra.setPreferredSize(new Dimension(340, 12));
        barra.setMaximumSize(new Dimension(340, 12));
        barra.setAlignmentX(Component.LEFT_ALIGNMENT);
        prog.add(barra);
        prog.add(Box.createVerticalStrut(6));
        prog.add(rotulo(falta > 0 ? "Faltam " + Usuario.formatarMl(falta) + " para a meta de hoje"
                : "Meta de água de hoje atingida!", 15, Font.PLAIN, new Color(255, 255, 255, 230)));
        rodape.add(prog, BorderLayout.WEST);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        botoes.setOpaque(false);
        JButton bebi = botao("Já bebi", new Color(0x1E90DC), Color.WHITE);
        JButton adiar = botao("Lembrar em 5 min", new Color(255, 255, 255, 235), new Color(0x0B4F75));
        JButton fechar = botao("Fechar", new Color(255, 255, 255, 150), new Color(0x0B4F75));
        bebi.addActionListener(e -> { respondido = true; resposta.bebeu(); dispose(); });
        adiar.addActionListener(e -> { respondido = true; resposta.adiar(5); dispose(); });
        fechar.addActionListener(e -> dispose());
        botoes.add(fechar);
        botoes.add(adiar);
        botoes.add(bebi);
        rodape.add(botoes, BorderLayout.EAST);
        fundo.add(rodape, BorderLayout.SOUTH);

        setContentPane(fundo);
        getRootPane().setDefaultButton(bebi);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (!respondido) resposta.ignorou();
            }
        });
    }

    public void mostrar() {
        setVisible(true);
        toFront();
        requestFocus();
        new Thread(TelaAlerta::tocarSom, "som-agua").start();
    }

    private static JLabel rotulo(String txt, int tam, int estilo, Color cor) {
        JLabel l = new JLabel(txt);
        l.setFont(fonte(estilo, tam));
        l.setForeground(cor);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static Font fonte(int estilo, int tam) {
        return new Font(InicializacaoWindows.isWindows() ? "Segoe UI" : Font.SANS_SERIF, estilo, tam);
    }

    private static JButton botao(String txt, Color fundo, Color texto) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isRollover() ? fundo.brighter() : fundo;
                g.setColor(c);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setFont(fonte(Font.BOLD, 18));
        b.setForeground(texto);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(12, 26, 12, 26));
        return b;
    }

    /** Usa a foto do usuário, se existir; senão a cachoeira embutida no programa. */
    private static synchronized BufferedImage imagem() {
        for (String n : new String[]{"cachoeira.jpg", "cachoeira.jpeg", "cachoeira.png"}) {
            Path p = Database.pastaDados().resolve(n);
            if (Files.exists(p)) {
                try { return ImageIO.read(p.toFile()); } catch (Exception ignored) { }
            }
        }
        if (imagemCache == null) {
            try (InputStream in = TelaAlerta.class.getResourceAsStream("/img/cachoeira.png")) {
                if (in != null) imagemCache = ImageIO.read(in);
            } catch (Exception ignored) { }
        }
        return imagemCache;
    }

    /** Som suave de "gotas" gerado pelo próprio programa. */
    private static void tocarSom() {
        try {
            float taxa = 44100f;
            AudioFormat fmt = new AudioFormat(taxa, 16, 1, true, false);
            try (SourceDataLine linha = AudioSystem.getSourceDataLine(fmt)) {
                linha.open(fmt);
                linha.start();
                double[] freqs = {880, 1175, 1568};
                for (double f : freqs) {
                    int n = (int) (taxa * 0.22);
                    byte[] buf = new byte[n * 2];
                    for (int i = 0; i < n; i++) {
                        double t = i / taxa;
                        double env = Math.exp(-t * 14);
                        double freq = f * (1 + 0.4 * Math.exp(-t * 30));
                        short v = (short) (Math.sin(2 * Math.PI * freq * t) * env * 9000);
                        buf[2 * i] = (byte) v;
                        buf[2 * i + 1] = (byte) (v >> 8);
                    }
                    linha.write(buf, 0, buf.length);
                }
                linha.drain();
            }
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
