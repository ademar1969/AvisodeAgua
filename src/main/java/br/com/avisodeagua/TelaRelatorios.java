package br.com.avisodeagua;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Relatórios e gráficos do consumo de água. */
public class TelaRelatorios extends JFrame {

    private static final Locale BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] PERIODOS = {"Últimos 7 dias", "Últimos 15 dias", "Últimos 30 dias",
            "Últimos 90 dias", "Este mês", "Mês passado"};

    private final Database db;
    private Usuario usuario;

    private final JComboBox<String> comboPeriodo = new JComboBox<String>(PERIODOS);
    private final JPanel painelResumo = new JPanel();
    private final Graficos.Barras graficoDiario = new Graficos.Barras();
    private final Graficos.Barras graficoHoras = new Graficos.Barras();
    private final Graficos.Rosca graficoAlertas = new Graficos.Rosca();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Data", "Dia", "Copos", "Quantidade", "% da meta", "Meta", "Adiados", "Ignorados"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private final JLabel lblStatus = new JLabel(" ");

    private List<Database.DiaConsumo> dados;
    private LocalDate de, ate;

    public TelaRelatorios(Database db) {
        super("Aviso de Água - Relatórios e Gráficos");
        this.db = db;
        setIconImage(Icones.gota(64));
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);
        raiz.add(barraTopo(), BorderLayout.NORTH);

        JTabbedPane abas = new JTabbedPane();
        painelResumo.setBackground(Color.WHITE);
        abas.addTab("Resumo", new JScrollPane(painelResumo));
        abas.addTab("Consumo por dia", graficoDiario);
        abas.addTab("Horários", graficoHoras);
        abas.addTab("Respostas aos alertas", graficoAlertas);
        abas.addTab("Histórico", criarTabela());
        abas.setBorder(new EmptyBorder(6, 8, 0, 8));
        raiz.add(abas, BorderLayout.CENTER);

        lblStatus.setBorder(new EmptyBorder(6, 14, 8, 14));
        lblStatus.setForeground(new Color(0x667788));
        raiz.add(lblStatus, BorderLayout.SOUTH);

        setContentPane(raiz);
        setSize(860, 600);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);
        comboPeriodo.addActionListener(e -> atualizar());
    }

    private JComponent barraTopo() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, Graficos.AZUL, getWidth(), 0, Graficos.AZUL_ESCURO));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel t = new JLabel("Relatórios e Gráficos");
        t.setIcon(new ImageIcon(Icones.gota(32)));
        t.setIconTextGap(10);
        t.setFont(TelaAlerta.fonte(Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        p.add(t, BorderLayout.WEST);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        JLabel lp = new JLabel("Período:");
        lp.setForeground(Color.WHITE);
        JButton atualizar = new JButton("Atualizar");
        JButton csv = new JButton("Exportar CSV (Excel)");
        JButton imprimir = new JButton("Imprimir");
        atualizar.addActionListener(e -> atualizar());
        csv.addActionListener(e -> exportarCsv());
        imprimir.addActionListener(e -> imprimir());
        acoes.add(lp);
        acoes.add(comboPeriodo);
        acoes.add(atualizar);
        acoes.add(csv);
        acoes.add(imprimir);
        p.add(acoes, BorderLayout.EAST);
        return p;
    }

    private JComponent criarTabela() {
        tabela.setRowHeight(24);
        tabela.setAutoCreateRowSorter(false);
        tabela.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer centro = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(c <= 1 ? LEFT : CENTER);
                if (!s) {
                    Object meta = t.getValueAt(r, 5);
                    comp.setForeground("Atingida".equals(meta) && c == 5 ? Graficos.VERDE : Graficos.TEXTO);
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF4F9FD));
                }
                return comp;
            }
        };
        tabela.setDefaultRenderer(Object.class, centro);
        return new JScrollPane(tabela);
    }

    public void abrir(Usuario u) {
        this.usuario = u;
        setVisible(true);
        setExtendedState(NORMAL);
        toFront();
        atualizar();
    }

    private void calcularPeriodo() {
        LocalDate hoje = LocalDate.now();
        ate = hoje;
        switch (comboPeriodo.getSelectedIndex()) {
            case 0: de = hoje.minusDays(6); break;
            case 1: de = hoje.minusDays(14); break;
            case 2: de = hoje.minusDays(29); break;
            case 3: de = hoje.minusDays(89); break;
            case 4: de = hoje.withDayOfMonth(1); break;
            default:
                de = hoje.minusMonths(1).withDayOfMonth(1);
                ate = hoje.withDayOfMonth(1).minusDays(1);
        }
    }

    public void atualizar() {
        if (usuario == null) return;
        calcularPeriodo();
        lblStatus.setText("Carregando dados do MySQL...");
        final LocalDate fDe = de, fAte = ate;
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{db.consumoPorDia(usuario, fDe, fAte), db.coposPorHora(usuario, fDe, fAte)};
            }
            @SuppressWarnings("unchecked")
            @Override protected void done() {
                try {
                    Object[] r = get();
                    dados = (List<Database.DiaConsumo>) r[0];
                    preencher(dados, (int[]) r[1]);
                    lblStatus.setText("Período: " + fDe.format(DATA) + " a " + fAte.format(DATA)
                            + "   •   Dados do banco MySQL (avisodeagua)   •   Meta: "
                            + Usuario.formatarMl(usuario.getMetaMl()) + " por dia");
                } catch (Exception e) {
                    lblStatus.setText("Erro ao carregar: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void preencher(List<Database.DiaConsumo> lista, int[] horas) {
        int metaMl = usuario.getMetaMl();
        int n = lista.size();

        // ---------- indicadores
        int totalMl = 0, totalCopos = 0, manuais = 0, adiados = 0, ignorados = 0, diasMeta = 0, diasComRegistro = 0;
        Database.DiaConsumo melhor = null;
        for (Database.DiaConsumo d : lista) {
            totalMl += d.ml; totalCopos += d.copos; manuais += d.manuais; adiados += d.adiados; ignorados += d.ignorados;
            if (d.ml >= metaMl && metaMl > 0) diasMeta++;
            if (d.copos > 0) diasComRegistro++;
            if (melhor == null || d.ml > melhor.ml) melhor = d;
        }
        int mediaMl = n > 0 ? totalMl / n : 0;
        int sequencia = sequenciaAtual();
        int horaPico = -1;
        for (int h = 0; h < 24; h++) if (horas[h] > 0 && (horaPico < 0 || horas[h] > horas[horaPico])) horaPico = h;
        int bebeuAlerta = totalCopos - manuais;
        int totalAlertas = bebeuAlerta + adiados + ignorados;

        Database.DiaConsumo hoje = null;
        for (Database.DiaConsumo d : lista) if (d.dia.equals(LocalDate.now())) hoje = d;
        int mlHoje = hoje != null ? hoje.ml : db.mlHoje(usuario);
        int coposHoje = hoje != null ? hoje.copos : db.coposHoje(usuario.getId());

        // ---------- aba Resumo
        painelResumo.removeAll();
        painelResumo.setLayout(new BorderLayout(0, 12));
        painelResumo.setBorder(new EmptyBorder(14, 14, 14, 14));
        painelResumo.add(painelMeta(mlHoje, coposHoje), BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(0, 3, 12, 12));
        cards.setOpaque(false);
        cards.add(card("Média diária", Usuario.formatarMl(mediaMl),
                metaMl > 0 ? String.format("%.0f%% da meta", mediaMl * 100.0 / metaMl) : ""));
        cards.add(card("Dias com meta atingida", diasMeta + " de " + n,
                n > 0 ? String.format("%.0f%% dos dias", diasMeta * 100.0 / n) : ""));
        cards.add(card("Sequência atual", sequencia + (sequencia == 1 ? " dia" : " dias"), "seguidos batendo a meta"));
        cards.add(card("Total no período", Usuario.formatarMl(totalMl), totalCopos + " copos"
                + (manuais > 0 ? " (" + manuais + " registrados pelo menu)" : "")));
        cards.add(card("Melhor dia", melhor != null && melhor.ml > 0 ? Usuario.formatarMl(melhor.ml) : "—",
                melhor != null && melhor.ml > 0 ? melhor.dia.format(DATA) : "sem registros"));
        cards.add(card("Horário em que mais bebe", horaPico >= 0 ? String.format("%02dh", horaPico) : "—",
                horaPico >= 0 ? horas[horaPico] + " copos nesse horário" : "sem registros"));
        cards.add(card("Alertas respondidos", totalAlertas > 0
                        ? String.format("%.0f%%", bebeuAlerta * 100.0 / totalAlertas) : "—",
                "bebeu em " + bebeuAlerta + " de " + totalAlertas + " alertas"));
        cards.add(card("Lembretes adiados", String.valueOf(adiados), "\"Lembrar em 5 min\""));
        cards.add(card("Dias com registro", diasComRegistro + " de " + n, "dias em que bebeu ao menos 1 copo"));
        painelResumo.add(cards, BorderLayout.CENTER);
        painelResumo.revalidate();
        painelResumo.repaint();

        // ---------- gráfico diário
        String[] rot = new String[n];
        String[] dicas = new String[n];
        double[] val = new double[n];
        for (int i = 0; i < n; i++) {
            Database.DiaConsumo d = lista.get(i);
            rot[i] = d.dia.format(DIA_MES);
            val[i] = d.ml;
            dicas[i] = "<html><b>" + d.dia.format(DATA) + " (" + nomeDia(d.dia) + ")</b><br>"
                    + d.copos + " copos — " + Usuario.formatarMl(d.ml)
                    + (metaMl > 0 ? String.format("<br>%.0f%% da meta", d.ml * 100.0 / metaMl) : "") + "</html>";
        }
        Graficos.Formato fml = new Graficos.Formato() {
            public String formatar(double v) { return Usuario.formatarMl((int) v); }
        };
        graficoDiario.definir("Água bebida por dia (ml)", rot, val, dicas, metaMl, "Meta", fml, true);

        // ---------- gráfico por hora (das 5h às 23h e qualquer outra com registro)
        int hIni = 5, hFim = 23;
        for (int h = 0; h < 24; h++) if (horas[h] > 0) { hIni = Math.min(hIni, h); hFim = Math.max(hFim, h); }
        int m = hFim - hIni + 1;
        String[] rh = new String[m];
        String[] dh = new String[m];
        double[] vh = new double[m];
        for (int i = 0; i < m; i++) {
            int h = hIni + i;
            rh[i] = h + "h";
            vh[i] = horas[h];
            dh[i] = String.format("%02d:00 às %02d:59 — %d copos", h, h, horas[h]);
        }
        graficoHoras.definir("Copos bebidos por horário do dia", rh, vh, dh, -1, "", new Graficos.Formato() {
            public String formatar(double v) { return String.valueOf((int) Math.round(v)); }
        }, false);

        // ---------- rosca dos alertas
        graficoAlertas.definir("Como você respondeu aos alertas no período",
                new String[]{"Bebeu", "Adiou (5 min)", "Ignorou / fechou"},
                new double[]{bebeuAlerta, adiados, ignorados},
                new Color[]{Graficos.AZUL, Graficos.LARANJA, Graficos.CINZA},
                totalAlertas > 0 ? String.format("%.0f%%", bebeuAlerta * 100.0 / totalAlertas) : "—");

        // ---------- tabela (mais recente primeiro)
        modelo.setRowCount(0);
        for (int i = n - 1; i >= 0; i--) {
            Database.DiaConsumo d = lista.get(i);
            modelo.addRow(new Object[]{
                    d.dia.format(DATA), nomeDia(d.dia), d.copos, Usuario.formatarMl(d.ml),
                    metaMl > 0 ? String.format("%.0f%%", d.ml * 100.0 / metaMl) : "—",
                    metaMl > 0 && d.ml >= metaMl ? "Atingida" : "Não atingida",
                    d.adiados, d.ignorados});
        }
    }

    /** Dias seguidos (até hoje) batendo a meta. Se hoje ainda não bateu, conta a partir de ontem. */
    private int sequenciaAtual() {
        int metaMl = usuario.getMetaMl();
        if (metaMl <= 0) return 0;
        List<Database.DiaConsumo> l = db.consumoPorDia(usuario, LocalDate.now().minusDays(365), LocalDate.now());
        int seq = 0;
        for (int i = l.size() - 1; i >= 0; i--) {
            Database.DiaConsumo d = l.get(i);
            if (d.ml >= metaMl) seq++;
            else if (i == l.size() - 1) continue; // hoje ainda em andamento
            else break;
        }
        return seq;
    }

    private JComponent painelMeta(int mlHoje, int coposHoje) {
        int metaMl = usuario.getMetaMl();
        JPanel p = new JPanel(new BorderLayout(14, 6));
        p.setBackground(new Color(0xEAF5FC));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC6E3F5)), new EmptyBorder(14, 16, 14, 16)));
        JLabel titulo = new JLabel("Meta de água: " + Usuario.formatarMl(metaMl) + " por dia  ("
                + usuario.getMetaCopos() + " copos de " + usuario.getCopoMl() + " ml)");
        titulo.setFont(TelaAlerta.fonte(Font.BOLD, 16));
        titulo.setForeground(Graficos.AZUL_ESCURO);
        String rec = usuario.getMetaRecomendadaMl() > 0
                ? "Recomendado para " + String.format(BR, "%.1f", usuario.getPesoKg()) + " kg (35 ml por kg): "
                  + Usuario.formatarMl(usuario.getMetaRecomendadaMl())
                : "Informe seu peso no cadastro para calcular a meta recomendada (35 ml por kg).";
        JLabel lrec = new JLabel(rec);
        lrec.setForeground(new Color(0x557085));
        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 2));
        txt.setOpaque(false);
        txt.add(titulo);
        txt.add(lrec);
        p.add(txt, BorderLayout.NORTH);

        int pct = metaMl > 0 ? (int) Math.round(mlHoje * 100.0 / metaMl) : 0;
        JProgressBar barra = new JProgressBar(0, 100);
        barra.setValue(Math.min(100, pct));
        barra.setStringPainted(true);
        barra.setString("Hoje: " + Usuario.formatarMl(mlHoje) + " de " + Usuario.formatarMl(metaMl)
                + "  (" + coposHoje + " copos — " + pct + "%)"
                + (mlHoje >= metaMl && metaMl > 0 ? "  - meta atingida!" : "  - faltam " + Usuario.formatarMl(Math.max(0, metaMl - mlHoje))));
        barra.setForeground(mlHoje >= metaMl ? Graficos.VERDE : Graficos.AZUL);
        barra.setPreferredSize(new Dimension(100, 28));
        barra.setFont(TelaAlerta.fonte(Font.BOLD, 13));
        p.add(barra, BorderLayout.CENTER);
        return p;
    }

    private static JComponent card(String titulo, String valor, String sub) {
        JPanel c = new JPanel(new GridLayout(3, 1, 0, 2));
        c.setBackground(Color.WHITE);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDCE6EE)), new EmptyBorder(10, 12, 10, 12)));
        JLabel t = new JLabel(titulo);
        t.setForeground(new Color(0x667788));
        t.setFont(TelaAlerta.fonte(Font.PLAIN, 12));
        JLabel v = new JLabel(valor);
        v.setForeground(Graficos.AZUL_ESCURO);
        v.setFont(TelaAlerta.fonte(Font.BOLD, 22));
        JLabel s = new JLabel(sub);
        s.setForeground(new Color(0x8899AA));
        s.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
        c.add(t); c.add(v); c.add(s);
        return c;
    }

    private static String nomeDia(LocalDate d) {
        String s = d.getDayOfWeek().getDisplayName(TextStyle.FULL, BR);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void exportarCsv() {
        if (dados == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("relatorio_agua_" + de + "_a_" + ate + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        int metaMl = usuario.getMetaMl();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write('﻿'); // BOM: o Excel abre com acentos corretos
            w.write("Relatório de consumo de água;" + usuario.getNome() + ";" + usuario.getCelular() + "\r\n");
            w.write("Período;" + de.format(DATA) + " a " + ate.format(DATA) + "\r\n");
            w.write("Meta diária (ml);" + metaMl + "\r\n\r\n");
            w.write("Data;Dia da semana;Copos;Quantidade (ml);% da meta;Meta atingida;Adiados;Ignorados\r\n");
            int total = 0, copos = 0;
            for (Database.DiaConsumo d : dados) {
                total += d.ml; copos += d.copos;
                w.write(d.dia.format(DATA) + ";" + nomeDia(d.dia) + ";" + d.copos + ";" + d.ml + ";"
                        + (metaMl > 0 ? Math.round(d.ml * 100.0 / metaMl) : 0) + ";"
                        + (metaMl > 0 && d.ml >= metaMl ? "Sim" : "Não") + ";" + d.adiados + ";" + d.ignorados + "\r\n");
            }
            w.write("TOTAL;;" + copos + ";" + total + ";;;;\r\n");
            w.write("MÉDIA DIÁRIA;;;" + (dados.isEmpty() ? 0 : total / dados.size()) + ";;;;\r\n");
            JOptionPane.showMessageDialog(this, "Relatório salvo em:\n" + f.getAbsolutePath(),
                    "Aviso de Água", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage(),
                    "Aviso de Água", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimir() {
        if (dados == null) return;
        try {
            tabela.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(("Consumo de água - " + usuario.getNome() + " - "
                            + de.format(DATA) + " a " + ate.format(DATA)
                            + " - Meta " + Usuario.formatarMl(usuario.getMetaMl()) + "/dia").replace("'", "''")),
                    new MessageFormat("Página {0}"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao imprimir: " + e.getMessage(),
                    "Aviso de Água", JOptionPane.ERROR_MESSAGE);
        }
    }
}
