package br.com.avisodeagua;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Medidas do corpo: cadastro, desenho, fotos, régua de progresso, gráficos, histórico e perfil. */
public class TelaCorpo extends JFrame {

    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color AZUL = new Color(0x1E90DC);
    private static final Color AZUL_ESCURO = new Color(0x0B4F75);

    private final Database db;
    private final CorpoDao dao;
    private final Runnable aoMudarPerfil;
    private Usuario usuario;
    private List<Medida> medidas = new ArrayList<Medida>();

    private final JTabbedPane abas = new JTabbedPane();
    private final JLabel lblStatus = new JLabel(" ");

    // aba medição
    private final JLabel lblModo = new JLabel("Nova medição");
    private JFormattedTextField campoData;
    private final JTextField campoPeso = new JTextField(6);
    private final JTextField[] campos = new JTextField[Medida.COLUNAS.length];
    private final JLabel lblPesoAnt = new JLabel();
    private final JLabel[] lblAnt = new JLabel[Medida.COLUNAS.length];
    private final JTextArea campoObs = new JTextArea(2, 14);
    private final DesenhoCorpo desenho = new DesenhoCorpo();
    private final JComboBox<String> comboComparar = new JComboBox<String>(
            new String[]{"Comparar com a medição anterior", "Comparar com a primeira medição", "Sem comparação"});
    private int editandoId = 0;

    // abas de progresso
    private final ReguaProgresso regua = new ReguaProgresso();
    private final Graficos.Linha grafico = new Graficos.Linha();
    private final JComboBox<String> comboIndicador = new JComboBox<String>();
    private final int[] indicadores;

    // fotos
    private final JComboBox<String> comboMedFoto = new JComboBox<String>();
    private final JComboBox<String> comboTipoFoto = new JComboBox<String>(CorpoDao.NOMES_FOTO);
    private final JPanel painelMiniaturas = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
    private final JComboBox<String> comboAntes = new JComboBox<String>();
    private final JComboBox<String> comboDepois = new JComboBox<String>();
    private final JComboBox<String> comboTipoComp = new JComboBox<String>(CorpoDao.NOMES_FOTO);
    private final JLabel imgAntes = new JLabel("", SwingConstants.CENTER);
    private final JLabel imgDepois = new JLabel("", SwingConstants.CENTER);
    private CorpoDao.Foto fotoSelecionada;

    // histórico
    private final DefaultTableModel modeloHist;
    private final JTable tabelaHist;

    // perfil
    private final JComboBox<String> comboSexo = new JComboBox<String>(new String[]{"Masculino", "Feminino"});
    private JFormattedTextField campoNasc;
    private final JTextField campoAltura = new JTextField(6);
    private final JComboBox<String> comboAtividade = new JComboBox<String>(Usuario.NIVEIS_ATIVIDADE);
    private final JTextField campoPesoMeta = new JTextField(6);
    private final JCheckBox chkKcalAuto = new JCheckBox("Calcular automaticamente");
    private final JSpinner campoKcal = new JSpinner(new SpinnerNumberModel(2000, 800, 6000, 50));
    private final JComboBox<String> comboLembrete = new JComboBox<String>(
            new String[]{"A cada 7 dias", "A cada 15 dias", "A cada 30 dias", "Não lembrar"});
    private static final int[] DIAS_LEMBRETE = {7, 15, 30, 0};
    private final JLabel lblCalculos = new JLabel(" ");

    public TelaCorpo(Database db, Runnable aoMudarPerfil) {
        this(db, new CorpoDao(db), aoMudarPerfil);
    }

    TelaCorpo(Database db, CorpoDao dao, Runnable aoMudarPerfil) {
        super("Aviso de Água - Medidas do corpo");
        this.db = db;
        this.dao = dao;
        this.aoMudarPerfil = aoMudarPerfil;
        setIconImage(Icones.gota(64));
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        indicadores = new int[Medida.COLUNAS.length + 3];
        indicadores[0] = -1; comboIndicador.addItem("Peso (kg)");
        indicadores[1] = 100; comboIndicador.addItem("IMC");
        indicadores[2] = 101; comboIndicador.addItem("% de gordura (estimada)");
        for (int i = 0; i < Medida.COLUNAS.length; i++) {
            indicadores[i + 3] = i;
            comboIndicador.addItem(Medida.NOMES[i] + " (cm)");
        }

        String[] cols = new String[Medida.COLUNAS.length + 4];
        cols[0] = "Data"; cols[1] = "Peso";
        for (int i = 0; i < Medida.COLUNAS.length; i++) cols[i + 2] = Medida.NOMES[i];
        cols[cols.length - 2] = "IMC"; cols[cols.length - 1] = "% gord.";
        modeloHist = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabelaHist = new JTable(modeloHist);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(topo(), BorderLayout.NORTH);
        abas.addTab("Medição", abaMedicao());
        abas.addTab("Régua de progresso", new JScrollPane(regua));
        abas.addTab("Gráficos", abaGraficos());
        abas.addTab("Fotos", abaFotos());
        abas.addTab("Histórico", abaHistorico());
        abas.addTab("Perfil e metas", abaPerfil());
        abas.setBorder(new EmptyBorder(6, 8, 0, 8));
        raiz.add(abas, BorderLayout.CENTER);
        lblStatus.setBorder(new EmptyBorder(6, 14, 8, 14));
        lblStatus.setForeground(new Color(0x557085));
        raiz.add(lblStatus, BorderLayout.SOUTH);
        setContentPane(raiz);
        setSize(1000, 720);
        setMinimumSize(new Dimension(900, 600));
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
        JLabel t = new JLabel("Medidas do corpo e acompanhamento");
        t.setFont(TelaAlerta.fonte(Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        p.add(t, BorderLayout.WEST);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acoes.setOpaque(false);
        JButton enviar = new JButton("Enviar relatório por e-mail");
        JButton cfg = new JButton("Configurar e-mail");
        enviar.addActionListener(e -> TelaEmail.enviarRelatorio(this, db, usuario));
        cfg.addActionListener(e -> new TelaEmail(this, db, usuario).setVisible(true));
        acoes.add(cfg);
        acoes.add(enviar);
        p.add(acoes, BorderLayout.EAST);
        return p;
    }

    // ================================================================== aba Medição

    private JComponent abaMedicao() {
        try {
            MaskFormatter mf = new MaskFormatter("##/##/####");
            mf.setPlaceholderCharacter('_');
            campoData = new JFormattedTextField(mf);
        } catch (Exception e) {
            campoData = new JFormattedTextField();
        }
        campoData.setColumns(8);

        JPanel f = new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        f.setBorder(new EmptyBorder(12, 14, 12, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 3, 3, 6);
        int y = 0;
        lblModo.setFont(TelaAlerta.fonte(Font.BOLD, 16));
        lblModo.setForeground(AZUL_ESCURO);
        c.gridx = 0; c.gridy = y++; c.gridwidth = 3; f.add(lblModo, c); c.gridwidth = 1;
        JLabel dica = new JLabel("<html><div style='width:300px'><small>Medidas em centímetros, com fita métrica. "
                + "Passe o mouse no nome para ver como medir. Pode deixar em branco o que não quiser medir.</small></div></html>");
        dica.setForeground(new Color(0x667788));
        c.gridy = y++; c.gridwidth = 3; f.add(dica, c); c.gridwidth = 1;

        linhaForm(f, c, y++, new JLabel("Data:"), campoData, new JLabel(""));
        linhaForm(f, c, y++, new JLabel("Peso (kg):"), campoPeso, lblPesoAnt);
        for (int i = 0; i < Medida.COLUNAS.length; i++) {
            campos[i] = new JTextField(6);
            lblAnt[i] = new JLabel();
            JLabel nome = new JLabel(Medida.NOMES[i] + ":");
            nome.setToolTipText(Medida.COMO_MEDIR[i]);
            campos[i].setToolTipText(Medida.COMO_MEDIR[i]);
            linhaForm(f, c, y++, nome, campos[i], lblAnt[i]);
        }
        campoObs.setLineWrap(true);
        campoObs.setWrapStyleWord(true);
        c.gridx = 0; c.gridy = y; f.add(new JLabel("Observação:"), c);
        c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        f.add(new JScrollPane(campoObs), c);
        c.gridwidth = 1; c.fill = GridBagConstraints.NONE; y++;

        JPanel bt = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bt.setOpaque(false);
        JButton salvar = new JButton("Salvar medição");
        salvar.setFont(salvar.getFont().deriveFont(Font.BOLD));
        JButton nova = new JButton("Nova");
        JButton ajuda = new JButton("Como medir?");
        salvar.addActionListener(e -> salvarMedicao());
        nova.addActionListener(e -> novaMedicao());
        ajuda.addActionListener(e -> comoMedir());
        bt.add(salvar);
        bt.add(nova);
        bt.add(ajuda);
        c.gridx = 0; c.gridy = y; c.gridwidth = 3; c.insets = new Insets(12, 3, 3, 6);
        f.add(bt, c);
        c.gridy = y + 1; c.weightx = 1; c.weighty = 1; f.add(Box.createGlue(), c);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { atualizarDesenho(); }
            public void removeUpdate(DocumentEvent e) { atualizarDesenho(); }
            public void changedUpdate(DocumentEvent e) { atualizarDesenho(); }
        };
        campoPeso.getDocument().addDocumentListener(dl);
        for (JTextField t : campos) t.getDocument().addDocumentListener(dl);
        comboComparar.addActionListener(e -> atualizarDesenho());

        JPanel dir = new JPanel(new BorderLayout());
        dir.setBackground(Color.WHITE);
        JPanel comp = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        comp.setBackground(Color.WHITE);
        comp.add(comboComparar);
        JButton salvarImg = new JButton("Salvar desenho (PNG)");
        salvarImg.addActionListener(e -> salvarDesenhoPng());
        comp.add(salvarImg);
        dir.add(comp, BorderLayout.NORTH);
        dir.add(desenho, BorderLayout.CENTER);

        JScrollPane sf = new JScrollPane(f);
        sf.setBorder(BorderFactory.createEmptyBorder());
        sf.getVerticalScrollBar().setUnitIncrement(16);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sf, dir);
        split.setDividerLocation(420);
        split.setResizeWeight(0);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    private static void linhaForm(JPanel f, GridBagConstraints c, int y, JComponent a, JComponent b, JComponent d) {
        c.gridx = 0; c.gridy = y; f.add(a, c);
        c.gridx = 1; f.add(b, c);
        if (d instanceof JLabel) {
            d.setForeground(new Color(0x8899AA));
            d.setFont(TelaAlerta.fonte(Font.PLAIN, 11));
        }
        c.gridx = 2; f.add(d, c);
    }

    private Medida lerFormulario(boolean validar) {
        Medida m = new Medida();
        m.setId(editandoId);
        String txt = campoData.getText();
        try {
            m.setData(LocalDate.parse(txt, DM));
        } catch (Exception e) {
            if (validar) { erro("Data inválida. Use dd/mm/aaaa."); return null; }
            m.setData(LocalDate.now());
        }
        double p = Medida.ler(campoPeso.getText());
        if (p < 0 || p > 400) { if (validar) { erro("Peso inválido."); return null; } p = 0; }
        m.setPeso(p);
        for (int i = 0; i < campos.length; i++) {
            double v = Medida.ler(campos[i].getText());
            if (v < 0 || v > 300) { if (validar) { erro("Valor inválido em \"" + Medida.NOMES[i] + "\"."); return null; } v = 0; }
            m.set(i, v);
        }
        m.setObservacao(campoObs.getText().trim());
        return m;
    }

    private void atualizarDesenho() {
        if (usuario == null) return;
        Medida m = lerFormulario(false);
        boolean vazio = m.getPeso() <= 0;
        for (int i = 0; i < campos.length; i++) vazio &= m.get(i) <= 0;
        if (vazio && editandoId == 0 && !medidas.isEmpty()) {
            // formulário novo em branco: mostra a última medição gravada
            Medida ult = medidas.get(medidas.size() - 1);
            Medida comp = null;
            String rot = "";
            int op = comboComparar.getSelectedIndex();
            if (op == 0 && medidas.size() >= 2) {
                comp = medidas.get(medidas.size() - 2);
                rot = "medição anterior (" + comp.getData().format(DM) + ")";
            } else if (op == 1 && medidas.size() >= 2) {
                comp = medidas.get(0);
                rot = "primeira medição (" + comp.getData().format(DM) + ")";
            }
            desenho.definir(usuario, ult, comp, rot);
            return;
        }
        Medida comp = null;
        String rot = "";
        int op = comboComparar.getSelectedIndex();
        List<Medida> outras = new ArrayList<Medida>();
        for (Medida x : medidas) if (x.getId() != editandoId && !x.getData().isAfter(m.getData())) outras.add(x);
        if (op == 0 && !outras.isEmpty()) {
            comp = outras.get(outras.size() - 1);
            rot = "medição anterior (" + comp.getData().format(DM) + ")";
        } else if (op == 1 && !outras.isEmpty()) {
            comp = outras.get(0);
            rot = "primeira medição (" + comp.getData().format(DM) + ")";
        }
        desenho.definir(usuario, m, comp, rot);
    }

    private void preencherAnteriores() {
        Medida ult = null;
        for (Medida x : medidas) if (x.getId() != editandoId) ult = x;
        lblPesoAnt.setText(ult != null && ult.getPeso() > 0 ? "anterior: " + Medida.fmt(ult.getPeso()) : "");
        for (int i = 0; i < campos.length; i++) {
            Medida u = null;
            for (Medida x : medidas) if (x.getId() != editandoId && x.get(i) > 0) u = x;
            lblAnt[i].setText(u != null ? "anterior: " + Medida.fmt(u.get(i)) : "");
        }
    }

    private void novaMedicao() {
        editandoId = 0;
        lblModo.setText("Nova medição");
        campoData.setText(LocalDate.now().format(DM));
        campoPeso.setText("");
        for (JTextField t : campos) t.setText("");
        campoObs.setText("");
        preencherAnteriores();
        atualizarDesenho();
    }

    private void editar(Medida m) {
        editandoId = m.getId();
        lblModo.setText("Editando a medição de " + m.getData().format(DM));
        campoData.setText(m.getData().format(DM));
        campoPeso.setText(m.getPeso() > 0 ? Medida.fmt(m.getPeso()) : "");
        for (int i = 0; i < campos.length; i++) campos[i].setText(m.get(i) > 0 ? Medida.fmt(m.get(i)) : "");
        campoObs.setText(m.getObservacao());
        preencherAnteriores();
        atualizarDesenho();
        abas.setSelectedIndex(0);
    }

    private void salvarMedicao() {
        Medida m = lerFormulario(true);
        if (m == null) return;
        boolean algum = m.getPeso() > 0;
        for (int i = 0; i < campos.length; i++) algum |= m.get(i) > 0;
        if (!algum) { erro("Informe pelo menos o peso ou uma medida."); return; }
        try {
            dao.salvar(usuario.getId(), m);
            if (m.getPeso() > 0 && !m.getData().isBefore(LocalDate.now().minusDays(1))) {
                usuario.setPesoKg(m.getPeso()); // atualiza o peso usado na meta de água
                db.salvarUsuario(usuario);
                aoMudarPerfil.run();
            }
            recarregar();
            editar(acharPorId(m.getId()));
            int op = JOptionPane.showOptionDialog(this,
                    "Medição de " + m.getData().format(DM) + " salva no banco de dados.\n\nDeseja adicionar fotos do corpo desta medição?",
                    "Medidas do corpo", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                    new Object[]{"Adicionar fotos", "Ver progresso", "OK"}, "OK");
            if (op == 0) { abas.setSelectedIndex(3); selecionarMedFoto(m.getId()); adicionarFoto(); }
            else if (op == 1) abas.setSelectedIndex(1);
        } catch (Exception e) {
            erro("Erro ao salvar no MySQL:\n" + e.getMessage());
        }
    }

    private Medida acharPorId(int id) {
        for (Medida x : medidas) if (x.getId() == id) return x;
        return medidas.isEmpty() ? new Medida() : medidas.get(medidas.size() - 1);
    }

    private void comoMedir() {
        StringBuilder sb = new StringBuilder("<html><body style='width:420px'><h3>Como tirar as medidas</h3>"
                + "<p>Use fita métrica de costura, sem apertar a pele. Meça sempre no mesmo horário "
                + "(de preferência de manhã, em jejum) e do mesmo lado do corpo.</p><ul>");
        for (int i = 0; i < Medida.COLUNAS.length; i++)
            sb.append("<li><b>").append(Medida.NOMES[i]).append(":</b> ").append(Medida.COMO_MEDIR[i]).append("</li>");
        sb.append("</ul><p><b>Peso:</b> mesma balança, sem sapatos, logo após acordar.</p></body></html>");
        JOptionPane.showMessageDialog(this, sb.toString(), "Como medir", JOptionPane.INFORMATION_MESSAGE);
    }

    private void salvarDesenhoPng() {
        Medida m = lerFormulario(false);
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("desenho_corpo_" + m.getData() + ".png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            BufferedImage img = new BufferedImage(Math.max(560, desenho.getWidth()), Math.max(640, desenho.getHeight()),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            desenho.desenhar(g, img.getWidth(), img.getHeight());
            g.dispose();
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getParentFile(), f.getName() + ".png");
            ImageIO.write(img, "png", f);
            JOptionPane.showMessageDialog(this, "Desenho salvo em:\n" + f.getAbsolutePath());
        } catch (Exception e) {
            erro("Erro ao salvar: " + e.getMessage());
        }
    }

    // ================================================================== aba Gráficos

    private JComponent abaGraficos() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JPanel t = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        t.setBackground(Color.WHITE);
        t.add(new JLabel("Mostrar:"));
        t.add(comboIndicador);
        p.add(t, BorderLayout.NORTH);
        p.add(grafico, BorderLayout.CENTER);
        comboIndicador.addActionListener(e -> atualizarGrafico());
        return p;
    }

    private void atualizarGrafico() {
        if (usuario == null) return;
        int ind = indicadores[Math.max(0, comboIndicador.getSelectedIndex())];
        int n = medidas.size();
        String[] r = new String[n];
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            r[i] = medidas.get(i).getData().format(DateTimeFormatter.ofPattern("dd/MM/yy"));
            v[i] = medidas.get(i).valorIndicador(ind, usuario);
        }
        final String un = Medida.unidadeIndicador(ind);
        double meta = ind == -1 && usuario.getPesoMetaKg() > 0 ? usuario.getPesoMetaKg() : -1;
        grafico.definir("Evolução: " + Medida.nomeIndicador(ind), r, v, meta, new Graficos.Formato() {
            public String formatar(double x) { return Medida.fmt(x) + (un.isEmpty() ? "" : " " + un); }
        });
    }

    // ================================================================== aba Fotos

    private JComponent abaFotos() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(Color.WHITE);
        JPanel t = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        t.setBackground(Color.WHITE);
        t.add(new JLabel("Medição:"));
        t.add(comboMedFoto);
        t.add(new JLabel("  Tipo:"));
        t.add(comboTipoFoto);
        JButton add = new JButton("Adicionar foto...");
        JButton girar = new JButton("Girar 90°");
        JButton rem = new JButton("Remover");
        JButton ver = new JButton("Ampliar");
        add.addActionListener(e -> adicionarFoto());
        girar.addActionListener(e -> girarFoto());
        rem.addActionListener(e -> removerFoto());
        ver.addActionListener(e -> { if (fotoSelecionada != null) ampliar(fotoSelecionada); });
        t.add(add); t.add(girar); t.add(rem); t.add(ver);
        p.add(t, BorderLayout.NORTH);
        comboMedFoto.addActionListener(e -> carregarMiniaturas());

        painelMiniaturas.setBackground(new Color(0xF4F9FD));
        JScrollPane sm = new JScrollPane(painelMiniaturas);
        sm.setPreferredSize(new Dimension(100, 200));
        sm.setBorder(BorderFactory.createTitledBorder("Fotos da medição (clique para selecionar, duplo clique para ampliar)"));

        JPanel comp = new JPanel(new BorderLayout(0, 4));
        comp.setBackground(Color.WHITE);
        comp.setBorder(BorderFactory.createTitledBorder("Comparar antes e depois"));
        JPanel ct = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ct.setBackground(Color.WHITE);
        ct.add(new JLabel("Antes:"));
        ct.add(comboAntes);
        ct.add(new JLabel("  Depois:"));
        ct.add(comboDepois);
        ct.add(new JLabel("  Tipo:"));
        ct.add(comboTipoComp);
        comp.add(ct, BorderLayout.NORTH);
        JPanel imgs = new JPanel(new GridLayout(1, 2, 10, 0));
        imgs.setBackground(Color.WHITE);
        imgAntes.setVerticalTextPosition(SwingConstants.BOTTOM);
        imgAntes.setHorizontalTextPosition(SwingConstants.CENTER);
        imgDepois.setVerticalTextPosition(SwingConstants.BOTTOM);
        imgDepois.setHorizontalTextPosition(SwingConstants.CENTER);
        imgs.add(imgAntes);
        imgs.add(imgDepois);
        comp.add(imgs, BorderLayout.CENTER);
        java.awt.event.ActionListener al = e -> compararFotos();
        comboAntes.addActionListener(al);
        comboDepois.addActionListener(al);
        comboTipoComp.addActionListener(al);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sm, comp);
        split.setDividerLocation(220);
        split.setBorder(BorderFactory.createEmptyBorder());
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private Medida medidaFoto() {
        int i = comboMedFoto.getSelectedIndex();
        return i >= 0 && i < medidas.size() ? medidas.get(medidas.size() - 1 - i) : null;
    }

    private void selecionarMedFoto(int id) {
        for (int i = 0; i < medidas.size(); i++)
            if (medidas.get(medidas.size() - 1 - i).getId() == id) comboMedFoto.setSelectedIndex(i);
    }

    private void carregarMiniaturas() {
        painelMiniaturas.removeAll();
        fotoSelecionada = null;
        Medida m = medidaFoto();
        if (m == null) {
            painelMiniaturas.add(new JLabel("Salve uma medição para poder adicionar fotos."));
        } else {
            try {
                List<CorpoDao.Foto> fotos = dao.fotos(m.getId());
                if (fotos.isEmpty()) painelMiniaturas.add(new JLabel("Nenhuma foto nesta medição. Clique em \"Adicionar foto...\"."));
                for (final CorpoDao.Foto f : fotos) {
                    final JLabel l = new JLabel(CorpoDao.nomeTipo(f.tipo), icone(f.imagem, 150, 170), SwingConstants.CENTER);
                    l.setVerticalTextPosition(SwingConstants.BOTTOM);
                    l.setHorizontalTextPosition(SwingConstants.CENTER);
                    l.setBorder(BorderFactory.createLineBorder(new Color(0xDCE6EE), 2));
                    l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    l.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent e) {
                            for (Component c : painelMiniaturas.getComponents())
                                if (c instanceof JLabel) ((JLabel) c).setBorder(BorderFactory.createLineBorder(new Color(0xDCE6EE), 2));
                            l.setBorder(BorderFactory.createLineBorder(AZUL, 2));
                            fotoSelecionada = f;
                            if (e.getClickCount() == 2) ampliar(f);
                        }
                    });
                    painelMiniaturas.add(l);
                }
            } catch (Exception e) {
                painelMiniaturas.add(new JLabel("Erro ao carregar fotos: " + e.getMessage()));
            }
        }
        painelMiniaturas.revalidate();
        painelMiniaturas.repaint();
    }

    private void adicionarFoto() {
        Medida m = medidaFoto();
        if (m == null) { erro("Salve uma medição antes de adicionar fotos."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Imagens (JPG, PNG)", "jpg", "jpeg", "png", "bmp", "gif"));
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String tipo = CorpoDao.TIPOS_FOTO[comboTipoFoto.getSelectedIndex()];
        int ok = 0;
        for (File f : fc.getSelectedFiles()) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) { erro("Formato de imagem não suportado: " + f.getName()); continue; }
                dao.adicionarFoto(m.getId(), tipo, jpeg(reduzir(img, 1280)));
                ok++;
            } catch (Exception e) {
                erro("Erro ao gravar " + f.getName() + ":\n" + e.getMessage());
            }
        }
        carregarMiniaturas();
        atualizarCombosComparacao();
        if (ok > 0) lblStatus.setText(ok + " foto(s) gravada(s) no banco de dados.");
    }

    private void girarFoto() {
        if (fotoSelecionada == null) { erro("Clique numa foto para selecionar."); return; }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(fotoSelecionada.imagem));
            BufferedImage r = new BufferedImage(img.getHeight(), img.getWidth(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = r.createGraphics();
            AffineTransform at = new AffineTransform();
            at.translate(img.getHeight(), 0);
            at.rotate(Math.PI / 2);
            g.drawImage(img, at, null);
            g.dispose();
            int medidaId = fotoSelecionada.medidaId;
            String tipo = fotoSelecionada.tipo;
            dao.removerFoto(fotoSelecionada.id);
            dao.adicionarFoto(medidaId, tipo, jpeg(r));
            carregarMiniaturas();
            compararFotos();
        } catch (Exception e) {
            erro("Erro ao girar: " + e.getMessage());
        }
    }

    private void removerFoto() {
        if (fotoSelecionada == null) { erro("Clique numa foto para selecionar."); return; }
        if (JOptionPane.showConfirmDialog(this, "Remover esta foto?", "Fotos", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            dao.removerFoto(fotoSelecionada.id);
            carregarMiniaturas();
            compararFotos();
        } catch (Exception e) {
            erro("Erro ao remover: " + e.getMessage());
        }
    }

    private void ampliar(CorpoDao.Foto f) {
        Rectangle tela = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        JLabel l = new JLabel(icone(f.imagem, tela.width - 120, tela.height - 160));
        JOptionPane.showMessageDialog(this, l, CorpoDao.nomeTipo(f.tipo) + " — " + f.data.format(DM), JOptionPane.PLAIN_MESSAGE);
    }

    private void atualizarCombosComparacao() {
        int a = comboAntes.getSelectedIndex(), d = comboDepois.getSelectedIndex();
        java.awt.event.ActionListener[] la = comboAntes.getActionListeners(), ld = comboDepois.getActionListeners();
        for (java.awt.event.ActionListener l : la) comboAntes.removeActionListener(l);
        for (java.awt.event.ActionListener l : ld) comboDepois.removeActionListener(l);
        comboAntes.removeAllItems();
        comboDepois.removeAllItems();
        for (Medida m : medidas) {
            comboAntes.addItem(m.getData().format(DM));
            comboDepois.addItem(m.getData().format(DM));
        }
        if (!medidas.isEmpty()) {
            comboAntes.setSelectedIndex(a >= 0 && a < medidas.size() ? a : 0);
            comboDepois.setSelectedIndex(d >= 0 && d < medidas.size() ? d : medidas.size() - 1);
        }
        for (java.awt.event.ActionListener l : la) comboAntes.addActionListener(l);
        for (java.awt.event.ActionListener l : ld) comboDepois.addActionListener(l);
        compararFotos();
    }

    private void compararFotos() {
        String tipo = CorpoDao.TIPOS_FOTO[Math.max(0, comboTipoComp.getSelectedIndex())];
        mostrarComparacao(imgAntes, comboAntes.getSelectedIndex(), tipo, "Antes");
        mostrarComparacao(imgDepois, comboDepois.getSelectedIndex(), tipo, "Depois");
    }

    private void mostrarComparacao(JLabel alvo, int idx, String tipo, String rotulo) {
        alvo.setIcon(null);
        if (idx < 0 || idx >= medidas.size()) { alvo.setText(rotulo + ": sem medição"); return; }
        Medida m = medidas.get(idx);
        try {
            CorpoDao.Foto f = dao.foto(m.getId(), tipo);
            String info = rotulo + " — " + m.getData().format(DM) + (m.getPeso() > 0 ? " — " + Medida.fmt(m.getPeso()) + " kg" : "");
            if (f == null) { alvo.setText(info + " (sem foto " + CorpoDao.nomeTipo(tipo).toLowerCase() + ")"); return; }
            alvo.setIcon(icone(f.imagem, 340, 300));
            alvo.setText(info);
        } catch (Exception e) {
            alvo.setText("Erro: " + e.getMessage());
        }
    }

    static BufferedImage reduzir(BufferedImage img, int max) {
        int w = img.getWidth(), h = img.getHeight();
        double esc = Math.min(1.0, max / (double) Math.max(w, h));
        int nw = Math.max(1, (int) (w * esc)), nh = Math.max(1, (int) (h * esc));
        BufferedImage r = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = r.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return r;
    }

    static byte[] jpeg(BufferedImage img) throws IOException {
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            w.setOutput(ios);
            ImageWriteParam p = w.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(0.85f);
            w.write(null, new IIOImage(img, null, null), p);
        } finally {
            w.dispose();
        }
        return out.toByteArray();
    }

    static ImageIcon icone(byte[] dados, int maxW, int maxH) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(dados));
            double esc = Math.min(maxW / (double) img.getWidth(), maxH / (double) img.getHeight());
            esc = Math.min(esc, 1.0);
            return new ImageIcon(reduzir(img, (int) (Math.max(img.getWidth(), img.getHeight()) * esc)));
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================== aba Histórico

    private JComponent abaHistorico() {
        JPanel p = new JPanel(new BorderLayout());
        tabelaHist.setRowHeight(24);
        tabelaHist.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabelaHist.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < modeloHist.getColumnCount(); i++)
            tabelaHist.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 90 : 78);
        tabelaHist.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) editarSelecionada(); }
        });
        p.add(new JScrollPane(tabelaHist), BorderLayout.CENTER);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton ed = new JButton("Editar");
        JButton ex = new JButton("Excluir");
        JButton csv = new JButton("Exportar CSV (Excel)");
        ed.addActionListener(e -> editarSelecionada());
        ex.addActionListener(e -> excluirSelecionada());
        csv.addActionListener(e -> exportarCsv());
        b.add(ed); b.add(ex); b.add(csv);
        b.add(new JLabel("   Duplo clique numa linha para editar."));
        p.add(b, BorderLayout.SOUTH);
        return p;
    }

    private Medida selecionadaHist() {
        int r = tabelaHist.getSelectedRow();
        if (r < 0) { erro("Selecione uma medição na tabela."); return null; }
        return medidas.get(medidas.size() - 1 - r);
    }

    private void editarSelecionada() {
        Medida m = selecionadaHist();
        if (m != null) editar(m);
    }

    private void excluirSelecionada() {
        Medida m = selecionadaHist();
        if (m == null) return;
        if (JOptionPane.showConfirmDialog(this, "Excluir a medição de " + m.getData().format(DM) + " e as fotos dela?",
                "Excluir", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        try {
            dao.excluir(usuario.getId(), m.getId());
            if (editandoId == m.getId()) novaMedicao();
            recarregar();
        } catch (Exception e) {
            erro("Erro ao excluir: " + e.getMessage());
        }
    }

    private void exportarCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("medidas_corpo_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write('﻿');
            w.write("Medidas do corpo;" + usuario.getNome() + "\r\n");
            StringBuilder cab = new StringBuilder("Data;Peso (kg)");
            for (String n : Medida.NOMES) cab.append(';').append(n).append(" (cm)");
            cab.append(";IMC;% gordura estimada;Observação\r\n");
            w.write(cab.toString());
            for (Medida m : medidas) {
                StringBuilder l = new StringBuilder(m.getData().format(DM)).append(';').append(num(m.getPeso()));
                for (int i = 0; i < Medida.COLUNAS.length; i++) l.append(';').append(num(m.get(i)));
                l.append(';').append(num(m.imc(usuario.getAlturaCm()))).append(';').append(num(m.gorduraEstimada(usuario)))
                        .append(';').append(m.getObservacao().replace(';', ',').replace('\n', ' ')).append("\r\n");
                w.write(l.toString());
            }
            JOptionPane.showMessageDialog(this, "Arquivo salvo em:\n" + f.getAbsolutePath());
        } catch (Exception e) {
            erro("Erro ao salvar: " + e.getMessage());
        }
    }

    private static String num(double v) { return v > 0 ? Medida.fmt(v) : ""; }

    // ================================================================== aba Perfil e metas

    private JComponent abaPerfil() {
        try {
            MaskFormatter mf = new MaskFormatter("##/##/####");
            mf.setPlaceholderCharacter('_');
            campoNasc = new JFormattedTextField(mf);
        } catch (Exception e) {
            campoNasc = new JFormattedTextField();
        }
        campoNasc.setColumns(8);
        JPanel f = new JPanel(new GridBagLayout());
        f.setBackground(Color.WHITE);
        f.setBorder(new EmptyBorder(16, 18, 16, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 4, 5, 8);
        int y = 0;
        JLabel t = new JLabel("Dados para os cálculos (IMC, gordura estimada, calorias e régua de progresso)");
        t.setFont(TelaAlerta.fonte(Font.BOLD, 14));
        t.setForeground(AZUL_ESCURO);
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; f.add(t, c); c.gridwidth = 1;
        linha2(f, c, y++, "Sexo:", comboSexo);
        linha2(f, c, y++, "Data de nascimento:", campoNasc);
        linha2(f, c, y++, "Altura (cm):", campoAltura);
        linha2(f, c, y++, "Nível de atividade:", comboAtividade);
        JPanel pm = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pm.setOpaque(false);
        pm.add(campoPesoMeta);
        pm.add(new JLabel("  kg   (o peso que você quer alcançar)"));
        linha2(f, c, y++, "Peso meta:", pm);
        JPanel pk = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pk.setOpaque(false);
        pk.add(chkKcalAuto);
        pk.add(new JLabel("   ou informe:  "));
        pk.add(campoKcal);
        pk.add(new JLabel("  kcal por dia"));
        linha2(f, c, y++, "Meta de calorias:", pk);
        chkKcalAuto.setOpaque(false);
        chkKcalAuto.addActionListener(e -> campoKcal.setEnabled(!chkKcalAuto.isSelected()));
        linha2(f, c, y++, "Sugerir atualizar medidas:", comboLembrete);
        JButton salvar = new JButton("Salvar perfil");
        salvar.setFont(salvar.getFont().deriveFont(Font.BOLD));
        salvar.addActionListener(e -> salvarPerfil());
        c.gridx = 1; c.gridy = y++; f.add(salvar, c);
        lblCalculos.setVerticalAlignment(SwingConstants.TOP);
        c.gridx = 0; c.gridy = y; c.gridwidth = 2; c.insets = new Insets(18, 4, 4, 8);
        f.add(lblCalculos, c);
        c.gridy = y + 1; c.weighty = 1; c.weightx = 1; f.add(Box.createGlue(), c);
        JScrollPane sp = new JScrollPane(f);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }

    private static void linha2(JPanel f, GridBagConstraints c, int y, String r, JComponent campo) {
        c.gridx = 0; c.gridy = y; f.add(new JLabel(r), c);
        c.gridx = 1; f.add(campo, c);
    }

    private void carregarPerfil() {
        comboSexo.setSelectedIndex(usuario.isFeminino() ? 1 : 0);
        campoNasc.setText(usuario.getDataNascimento() != null ? usuario.getDataNascimento().format(DM) : "");
        campoAltura.setText(usuario.getAlturaCm() > 0 ? Medida.fmt(usuario.getAlturaCm()) : "");
        comboAtividade.setSelectedIndex(usuario.getNivelAtividade());
        campoPesoMeta.setText(usuario.getPesoMetaKg() > 0 ? Medida.fmt(usuario.getPesoMetaKg()) : "");
        chkKcalAuto.setSelected(usuario.getMetaKcal() <= 0);
        campoKcal.setEnabled(usuario.getMetaKcal() > 0);
        if (usuario.getMetaKcal() > 0) campoKcal.setValue(usuario.getMetaKcal());
        int idx = 3;
        for (int i = 0; i < DIAS_LEMBRETE.length; i++) if (DIAS_LEMBRETE[i] == usuario.getLembreteMedidasDias()) idx = i;
        comboLembrete.setSelectedIndex(idx);
        atualizarCalculos();
    }

    private void salvarPerfil() {
        LocalDate nasc = null;
        String tn = campoNasc.getText().replace("_", "").replace("/", "").trim();
        if (!tn.isEmpty()) {
            try {
                nasc = LocalDate.parse(campoNasc.getText(), DM);
                if (nasc.isAfter(LocalDate.now()) || nasc.isBefore(LocalDate.now().minusYears(120))) throw new Exception();
            } catch (Exception e) { erro("Data de nascimento inválida."); return; }
        }
        double alt = Medida.ler(campoAltura.getText());
        if (alt < 0 || (alt > 0 && (alt < 90 || alt > 250))) { erro("Altura inválida (em centímetros, ex.: 172)."); return; }
        double pm = Medida.ler(campoPesoMeta.getText());
        if (pm < 0 || pm > 400) { erro("Peso meta inválido."); return; }
        usuario.setSexo(comboSexo.getSelectedIndex() == 1 ? "F" : "M");
        usuario.setDataNascimento(nasc);
        usuario.setAlturaCm(alt);
        usuario.setNivelAtividade(comboAtividade.getSelectedIndex());
        usuario.setPesoMetaKg(pm);
        usuario.setMetaKcal(chkKcalAuto.isSelected() ? 0 : (Integer) campoKcal.getValue());
        usuario.setLembreteMedidasDias(DIAS_LEMBRETE[comboLembrete.getSelectedIndex()]);
        try {
            db.salvarUsuario(usuario);
            aoMudarPerfil.run();
            atualizarTudo();
            JOptionPane.showMessageDialog(this, "Perfil salvo no banco de dados.", "Perfil", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            erro("Erro ao salvar: " + e.getMessage());
        }
    }

    private void atualizarCalculos() {
        Medida ult = medidas.isEmpty() ? null : ReguaProgresso.ultimaComPeso(medidas);
        double peso = usuario.pesoReferencia(ult);
        StringBuilder sb = new StringBuilder("<html><div style='width:560px'><b>Cálculos aproximados</b><br><br>");
        if (usuario.getIdade() > 0) sb.append("Idade: ").append(usuario.getIdade()).append(" anos<br>");
        if (peso > 0) sb.append("Peso de referência: ").append(Medida.fmt(peso)).append(" kg<br>");
        if (ult != null && usuario.getAlturaCm() > 0) {
            double imc = ult.imc(usuario.getAlturaCm());
            sb.append("IMC: <b>").append(Medida.fmt(imc)).append("</b> (").append(Medida.classificarImc(imc)).append(")<br>");
            double h = usuario.getAlturaCm() / 100;
            sb.append("Faixa de peso saudável (IMC 18,5 a 24,9): ").append(Medida.fmt(18.5 * h * h)).append(" a ")
                    .append(Medida.fmt(24.9 * h * h)).append(" kg<br>");
        }
        int tmb = usuario.getTmb(peso);
        if (tmb > 0) {
            sb.append("Metabolismo basal (Mifflin-St Jeor): ").append(tmb).append(" kcal/dia<br>");
            sb.append("Gasto diário estimado: ").append(usuario.getGastoDiario(peso)).append(" kcal/dia<br>");
            sb.append("Meta de calorias sugerida: <b>").append(usuario.getMetaKcalSugerida(peso)).append(" kcal/dia</b>");
            if (usuario.getPesoMetaKg() > 0 && usuario.getPesoMetaKg() < peso)
                sb.append(" (déficit de ~500 kcal para perder cerca de 0,5 kg por semana)");
            sb.append("<br>");
        } else {
            sb.append("<span style='color:#8899AA'>Preencha data de nascimento, altura e peso para calcular as calorias.</span><br>");
        }
        sb.append("<br><span style='color:#8899AA'>São estimativas para acompanhamento pessoal; não substituem a orientação de "
                + "um nutricionista ou médico.</span></div></html>");
        lblCalculos.setText(sb.toString());
    }

    // ================================================================== geral

    public void abrir(Usuario u) {
        this.usuario = u;
        recarregar();
        carregarPerfil();
        novaMedicao();
        if (medidas.isEmpty() && u.getAlturaCm() <= 0) abas.setSelectedIndex(5); // começa pelo perfil
        setVisible(true);
        setExtendedState(NORMAL);
        toFront();
    }

    private void recarregar() {
        try {
            medidas = dao.listar(usuario.getId());
        } catch (Exception e) {
            erro("Erro ao ler medições do MySQL:\n" + e.getMessage());
            medidas = new ArrayList<Medida>();
        }
        atualizarTudo();
    }

    private void atualizarTudo() {
        // histórico (mais recente primeiro)
        modeloHist.setRowCount(0);
        for (int k = medidas.size() - 1; k >= 0; k--) {
            Medida m = medidas.get(k);
            Object[] row = new Object[modeloHist.getColumnCount()];
            row[0] = m.getData().format(DM);
            row[1] = num(m.getPeso());
            for (int i = 0; i < Medida.COLUNAS.length; i++) row[i + 2] = num(m.get(i));
            row[row.length - 2] = num(m.imc(usuario.getAlturaCm()));
            row[row.length - 1] = num(m.gorduraEstimada(usuario));
            modeloHist.addRow(row);
        }
        // combo de fotos
        int sel = comboMedFoto.getSelectedIndex();
        java.awt.event.ActionListener[] ls = comboMedFoto.getActionListeners();
        for (java.awt.event.ActionListener l : ls) comboMedFoto.removeActionListener(l);
        comboMedFoto.removeAllItems();
        for (int k = medidas.size() - 1; k >= 0; k--) {
            Medida m = medidas.get(k);
            comboMedFoto.addItem(m.getData().format(DM) + (m.getPeso() > 0 ? " — " + Medida.fmt(m.getPeso()) + " kg" : ""));
        }
        if (comboMedFoto.getItemCount() > 0) comboMedFoto.setSelectedIndex(sel >= 0 && sel < comboMedFoto.getItemCount() ? sel : 0);
        for (java.awt.event.ActionListener l : ls) comboMedFoto.addActionListener(l);
        carregarMiniaturas();
        atualizarCombosComparacao();

        regua.definir(usuario, medidas);
        regua.setPreferredSize(new Dimension(760, 760));
        atualizarGrafico();
        preencherAnteriores();
        atualizarDesenho();
        atualizarCalculos();

        LocalDate ult = medidas.isEmpty() ? null : medidas.get(medidas.size() - 1).getData();
        String st = medidas.size() + " medição(ões) gravada(s) no MySQL";
        if (ult != null) {
            st += "   •   Última: " + ult.format(DM);
            if (usuario.getLembreteMedidasDias() > 0) {
                LocalDate prox = ult.plusDays(usuario.getLembreteMedidasDias());
                st += "   •   Próxima atualização sugerida: " + prox.format(DM)
                        + (prox.isAfter(LocalDate.now()) ? "" : "  (já está na hora de medir de novo!)");
            }
        }
        lblStatus.setText(st);
    }

    private void erro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Medidas do corpo", JOptionPane.WARNING_MESSAGE);
    }
}
