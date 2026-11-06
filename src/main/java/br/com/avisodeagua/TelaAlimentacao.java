package br.com.avisodeagua;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Registro aproximado do que foi comido no dia, com calorias e comparação com a meta. */
public class TelaAlimentacao extends JFrame {

    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color AZUL = new Color(0x1E90DC);
    private static final Color AZUL_ESCURO = new Color(0x0B4F75);

    private final Database db;
    private final AlimentacaoDao dao;
    private Usuario usuario;
    private LocalDate dia = LocalDate.now();
    private List<AlimentacaoDao.Item> itens = new ArrayList<AlimentacaoDao.Item>();

    private final JLabel lblDia = new JLabel();
    private final JComboBox<String> comboRefeicao = new JComboBox<String>(AlimentacaoDao.REFEICOES);
    private final JComboBox<String> comboAlimento = new JComboBox<String>();
    private final JTextField campoPorcao = new JTextField(16);
    private final JSpinner campoQtd = new JSpinner(new SpinnerNumberModel(1.0, 0.25, 20.0, 0.5));
    private final JSpinner campoKcal = new JSpinner(new SpinnerNumberModel(0, 0, 5000, 10));
    private boolean ajustando;
    private int kcalPorcao = 0;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Refeição", "Alimento", "Porção", "Qtd.", "kcal (aprox.)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabela = new JTable(modelo);
    private final JProgressBar barraKcal = new JProgressBar(0, 100);
    private final JLabel lblResumo = new JLabel(" ");
    private final Graficos.Barras grafico = new Graficos.Barras();

    public TelaAlimentacao(Database db) {
        this(db, new AlimentacaoDao(db), new CorpoDao(db));
    }

    private final CorpoDao corpoDao;

    TelaAlimentacao(Database db, AlimentacaoDao dao, CorpoDao corpoDao) {
        super("Aviso de Água - Alimentação do dia");
        this.db = db;
        this.dao = dao;
        this.corpoDao = corpoDao;
        setIconImage(Icones.gota(64));
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);
        raiz.add(topo(), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBackground(Color.WHITE);
        centro.setBorder(new EmptyBorder(10, 12, 10, 12));
        centro.add(painelAdicionar(), BorderLayout.NORTH);

        tabela.setRowHeight(24);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(240);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(170);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(90);
        DefaultTableCellRenderer dir = new DefaultTableCellRenderer();
        dir.setHorizontalAlignment(SwingConstants.RIGHT);
        tabela.getColumnModel().getColumn(3).setCellRenderer(dir);
        tabela.getColumnModel().getColumn(4).setCellRenderer(dir);
        JPanel lista = new JPanel(new BorderLayout());
        lista.add(new JScrollPane(tabela), BorderLayout.CENTER);
        JPanel bl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bl.setBackground(Color.WHITE);
        JButton rem = new JButton("Remover item");
        rem.addActionListener(e -> remover());
        bl.add(rem);
        JLabel aviso = new JLabel("Calorias aproximadas por porção caseira; ajuste o valor se souber o exato.");
        aviso.setForeground(new Color(0x8899AA));
        bl.add(aviso);
        lista.add(bl, BorderLayout.SOUTH);

        JPanel resumo = new JPanel(new BorderLayout(0, 6));
        resumo.setBackground(Color.WHITE);
        barraKcal.setStringPainted(true);
        barraKcal.setPreferredSize(new Dimension(100, 28));
        barraKcal.setFont(TelaAlerta.fonte(Font.BOLD, 13));
        resumo.add(barraKcal, BorderLayout.NORTH);
        lblResumo.setVerticalAlignment(SwingConstants.TOP);
        resumo.add(lblResumo, BorderLayout.CENTER);
        grafico.setPreferredSize(new Dimension(420, 260));
        resumo.add(grafico, BorderLayout.SOUTH);
        resumo.setPreferredSize(new Dimension(430, 100));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lista, resumo);
        split.setResizeWeight(0.6);
        split.setBorder(BorderFactory.createEmptyBorder());
        centro.add(split, BorderLayout.CENTER);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);
        setSize(1060, 660);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
    }

    private JComponent topo() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, AZUL, getWidth(), 0, AZUL_ESCURO));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel t = new JLabel("Alimentação do dia");
        t.setFont(TelaAlerta.fonte(Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        p.add(t, BorderLayout.WEST);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nav.setOpaque(false);
        JButton ant = new JButton("< Dia anterior");
        JButton hoje = new JButton("Hoje");
        JButton prox = new JButton("Próximo dia >");
        JButton copiar = new JButton("Copiar do dia anterior");
        lblDia.setForeground(Color.WHITE);
        lblDia.setFont(TelaAlerta.fonte(Font.BOLD, 15));
        ant.addActionListener(e -> mudarDia(dia.minusDays(1)));
        hoje.addActionListener(e -> mudarDia(LocalDate.now()));
        prox.addActionListener(e -> { if (dia.isBefore(LocalDate.now())) mudarDia(dia.plusDays(1)); });
        copiar.addActionListener(e -> copiarDiaAnterior());
        nav.add(lblDia);
        nav.add(Box.createHorizontalStrut(10));
        nav.add(ant); nav.add(hoje); nav.add(prox); nav.add(copiar);
        p.add(nav, BorderLayout.EAST);
        return p;
    }

    private JComponent painelAdicionar() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(0xF2F8FC));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC6E3F5)), new EmptyBorder(8, 10, 8, 10)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        String[] rot = {"Refeição", "Alimento (digite para buscar ou escreva outro)", "Porção", "Qtd.", "kcal (total)", ""};
        for (int i = 0; i < rot.length; i++) {
            c.gridx = i; c.gridy = 0;
            JLabel l = new JLabel(rot[i]);
            l.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
            l.setForeground(new Color(0x557085));
            p.add(l, c);
        }
        comboAlimento.setEditable(true);
        comboAlimento.setPrototypeDisplayValue("Prato feito (arroz, feijão, carne e salada)xx");
        preencherSugestoes("");
        JTextComponent ed = (JTextComponent) comboAlimento.getEditor().getEditorComponent();
        ed.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_UP || k == KeyEvent.VK_DOWN || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_ESCAPE) return;
                String txt = ed.getText();
                preencherSugestoes(txt);
                ed.setText(txt);
                if (comboAlimento.getItemCount() > 0 && !txt.isEmpty()) comboAlimento.showPopup();
            }
        });
        comboAlimento.addActionListener(e -> escolherAlimento());
        campoQtd.addChangeListener(e -> recalcularKcal());
        JButton add = new JButton("Adicionar");
        add.setFont(add.getFont().deriveFont(Font.BOLD));
        add.addActionListener(e -> adicionar());
        JComponent[] cs = {comboRefeicao, comboAlimento, campoPorcao, campoQtd, campoKcal, add};
        for (int i = 0; i < cs.length; i++) { c.gridx = i; c.gridy = 1; p.add(cs[i], c); }
        return p;
    }

    private void preencherSugestoes(String filtro) {
        ajustando = true;
        comboAlimento.removeAllItems();
        String f = semAcento(filtro.toLowerCase());
        for (Alimentos.Alimento a : Alimentos.todos())
            if (f.isEmpty() || semAcento(a.nome.toLowerCase()).contains(f)) comboAlimento.addItem(a.nome);
        comboAlimento.setSelectedIndex(-1);
        ajustando = false;
    }

    private static String semAcento(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private void escolherAlimento() {
        if (ajustando) return;
        Object sel = comboAlimento.getEditor().getItem();
        Alimentos.Alimento a = sel == null ? null : Alimentos.buscar(sel.toString().trim());
        if (a != null) {
            campoPorcao.setText(a.porcao);
            kcalPorcao = a.kcal;
            recalcularKcal();
        }
    }

    private void recalcularKcal() {
        if (kcalPorcao > 0) campoKcal.setValue((int) Math.round(kcalPorcao * ((Number) campoQtd.getValue()).doubleValue()));
    }

    private void adicionar() {
        Object sel = comboAlimento.getEditor().getItem();
        String nome = sel == null ? "" : sel.toString().trim();
        if (nome.isEmpty()) { aviso("Escolha ou digite um alimento."); return; }
        Alimentos.Alimento a = Alimentos.buscar(nome);
        int kcal = (Integer) campoKcal.getValue();
        if (a == null && kcal == 0) {
            int r = JOptionPane.showConfirmDialog(this, "\"" + nome + "\" não está na tabela.\nInforme as calorias aproximadas "
                    + "no campo kcal (ou deixe 0).\n\nAdicionar mesmo assim com 0 kcal?", "Alimentação", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
        }
        AlimentacaoDao.Item it = new AlimentacaoDao.Item();
        it.data = dia;
        it.refeicao = (String) comboRefeicao.getSelectedItem();
        it.descricao = nome.length() > 150 ? nome.substring(0, 150) : nome;
        it.porcao = campoPorcao.getText().trim();
        it.quantidade = ((Number) campoQtd.getValue()).doubleValue();
        it.kcal = kcal;
        try {
            dao.adicionar(usuario.getId(), it);
            preencherSugestoes("");
            ((JTextComponent) comboAlimento.getEditor().getEditorComponent()).setText("");
            campoPorcao.setText("");
            campoQtd.setValue(1.0);
            kcalPorcao = 0;
            campoKcal.setValue(0);
            carregar();
        } catch (Exception e) {
            aviso("Erro ao gravar no MySQL:\n" + e.getMessage());
        }
    }

    private void remover() {
        int r = tabela.getSelectedRow();
        if (r < 0 || r >= itens.size()) { aviso("Selecione um item na tabela."); return; }
        try {
            dao.remover(usuario.getId(), itens.get(r).id);
            carregar();
        } catch (Exception e) {
            aviso("Erro ao remover: " + e.getMessage());
        }
    }

    private void copiarDiaAnterior() {
        if (!itens.isEmpty() && JOptionPane.showConfirmDialog(this, "Este dia já tem itens. Copiar mesmo assim (vai somar)?",
                "Copiar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            int n = dao.copiarDia(usuario.getId(), dia.minusDays(1), dia);
            if (n == 0) aviso("O dia anterior não tem itens registrados.");
            carregar();
        } catch (Exception e) {
            aviso("Erro ao copiar: " + e.getMessage());
        }
    }

    private void mudarDia(LocalDate d) {
        dia = d;
        carregar();
    }

    public void abrir(Usuario u) {
        this.usuario = u;
        dia = LocalDate.now();
        int h = java.time.LocalTime.now().getHour();
        comboRefeicao.setSelectedIndex(h < 10 ? 0 : h < 12 ? 1 : h < 15 ? 2 : h < 18 ? 3 : h < 22 ? 4 : 5);
        carregar();
        setVisible(true);
        setExtendedState(NORMAL);
        toFront();
    }

    private void carregar() {
        String nomeDia = dia.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        lblDia.setText((dia.equals(LocalDate.now()) ? "Hoje, " : "") + nomeDia + " " + dia.format(DM));
        try {
            itens = dao.itens(usuario.getId(), dia);
        } catch (Exception e) {
            aviso("Erro ao ler do MySQL:\n" + e.getMessage());
            itens = new ArrayList<AlimentacaoDao.Item>();
        }
        modelo.setRowCount(0);
        int total = 0;
        int[] porRef = new int[AlimentacaoDao.REFEICOES.length];
        for (AlimentacaoDao.Item it : itens) {
            modelo.addRow(new Object[]{it.refeicao, it.descricao, it.porcao,
                    Medida.fmt(it.quantidade).replace(",0", ""), it.kcal});
            total += it.kcal;
            for (int i = 0; i < porRef.length; i++) if (AlimentacaoDao.REFEICOES[i].equals(it.refeicao)) porRef[i] += it.kcal;
        }

        Medida ult = null;
        try {
            List<Medida> l = corpoDao.listar(usuario.getId());
            ult = ReguaProgresso.ultimaComPeso(l);
        } catch (Exception ignored) { }
        double peso = usuario.pesoReferencia(ult);
        int meta = usuario.getMetaKcalEfetiva(peso);
        if (meta > 0) {
            int pct = (int) Math.round(total * 100.0 / meta);
            barraKcal.setValue(Math.min(100, pct));
            barraKcal.setString(total + " de " + meta + " kcal (" + pct + "%)"
                    + (total > meta ? "  - passou " + (total - meta) + " kcal" : "  - restam " + (meta - total) + " kcal"));
            barraKcal.setForeground(total > meta * 1.05 ? new Color(0xD9822B) : new Color(0x2E9E5B));
        } else {
            barraKcal.setValue(0);
            barraKcal.setString(total + " kcal  (defina a meta em Medidas do corpo > Perfil e metas)");
        }
        StringBuilder sb = new StringBuilder("<html><div style='padding:6px 2px'><b>Por refeição</b><br>");
        for (int i = 0; i < porRef.length; i++)
            if (porRef[i] > 0) sb.append(AlimentacaoDao.REFEICOES[i]).append(": ").append(porRef[i]).append(" kcal<br>");
        if (total == 0) sb.append("<span style='color:#8899AA'>Nada registrado neste dia.</span><br>");
        java.util.List<Database.DiaConsumo> agua = db.consumoPorDia(usuario, dia, dia);
        int ml = agua.isEmpty() ? 0 : agua.get(0).ml;
        sb.append("<br><b>Água neste dia:</b> ").append(Usuario.formatarMl(ml)).append(" de ")
                .append(Usuario.formatarMl(usuario.getMetaMl())).append("</div></html>");
        lblResumo.setText(sb.toString());

        // últimos 7 dias
        LocalDate de = dia.minusDays(6);
        Map<LocalDate, Integer> semana = dao.kcalPorDia(usuario.getId(), de, dia);
        String[] r = new String[7];
        double[] v = new double[7];
        String[] dicas = new String[7];
        int i = 0;
        for (Map.Entry<LocalDate, Integer> e : semana.entrySet()) {
            r[i] = e.getKey().format(DateTimeFormatter.ofPattern("dd/MM"));
            v[i] = e.getValue();
            dicas[i] = e.getKey().format(DM) + ": " + e.getValue() + " kcal";
            i++;
        }
        grafico.definir("Calorias nos últimos 7 dias (aprox.)", r, v, dicas, meta > 0 ? meta : -1, "Meta",
                new Graficos.Formato() { public String formatar(double x) { return String.valueOf((int) Math.round(x)); } }, false);
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Alimentação", JOptionPane.WARNING_MESSAGE);
    }
}
