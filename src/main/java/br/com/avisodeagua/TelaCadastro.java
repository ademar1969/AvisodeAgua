package br.com.avisodeagua;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

/** Janela de cadastro: nome, celular, intervalo e horários dos lembretes. */
public class TelaCadastro extends JFrame {

    private static final Color AZUL = new Color(0x1E90DC);
    private static final Color AZUL_ESCURO = new Color(0x0B4F75);

    private final JTextField campoNome = new JTextField(24);
    private final JFormattedTextField campoCelular;
    private final JSpinner campoIntervalo = new JSpinner(new SpinnerNumberModel(60, 5, 600, 5));
    private final JSpinner campoInicio = spinnerHora();
    private final JSpinner campoFim = spinnerHora();
    private final JSpinner campoMeta = new JSpinner(new SpinnerNumberModel(8, 1, 40, 1));
    private final JSpinner campoPeso = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 300.0, 0.5));
    private final JSpinner campoCopo = new JSpinner(new SpinnerNumberModel(250, 50, 1000, 50));
    private final JLabel lblMetaTotal = new JLabel(" ");
    private final JLabel lblRecomendado = new JLabel(" ");
    private final JButton btnRecomendada = new JButton("Usar recomendada");
    private final JTextField campoEmail = new JTextField(24);
    private Runnable aoCorpo = () -> { }, aoAlimentacao = () -> { }, aoEmail = () -> { };
    private final JCheckBox campoAtivo = new JCheckBox("Lembretes ativados");
    private final JCheckBox campoIniciarWindows = new JCheckBox("Iniciar junto com o Windows");
    private final JLabel lblProximo = new JLabel(" ");
    private final JLabel lblHoje = new JLabel(" ");

    private Usuario usuario;
    private final Consumer<Usuario> aoSalvar;
    private final Runnable aoTestar;
    private final Runnable aoRelatorios;

    public TelaCadastro(Consumer<Usuario> aoSalvar, Runnable aoTestar, Runnable aoRelatorios) {
        super("Aviso de Água - Cadastro");
        this.aoSalvar = aoSalvar;
        this.aoTestar = aoTestar;
        this.aoRelatorios = aoRelatorios;
        setIconImage(Icones.gota(64));
        setDefaultCloseOperation(HIDE_ON_CLOSE); // fechar só esconde: continua na bandeja

        JFormattedTextField cel;
        try {
            MaskFormatter mf = new MaskFormatter("(##) #####-####");
            mf.setPlaceholderCharacter('_');
            cel = new JFormattedTextField(mf);
        } catch (ParseException e) {
            cel = new JFormattedTextField();
        }
        campoCelular = cel;
        campoCelular.setColumns(14);
        campoNome.setMinimumSize(campoNome.getPreferredSize());
        campoCelular.setMinimumSize(campoCelular.getPreferredSize());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Color.WHITE);
        raiz.add(cabecalho(), BorderLayout.NORTH);
        raiz.add(formulario(), BorderLayout.CENTER);
        raiz.add(rodape(), BorderLayout.SOUTH);
        setContentPane(raiz);
        pack();
        setMinimumSize(getSize());
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JComponent cabecalho() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, AZUL, getWidth(), getHeight(), AZUL_ESCURO));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel t = new JLabel("Aviso de Água");
        t.setIcon(new ImageIcon(Icones.gota(40)));
        t.setIconTextGap(12);
        t.setFont(TelaAlerta.fonte(Font.BOLD, 24));
        t.setForeground(Color.WHITE);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nav.setOpaque(false);
        String[] nomes = {"Relatórios da água", "Medidas do corpo", "Alimentação", "E-mail"};
        for (int i = 0; i < nomes.length; i++) {
            final int k = i;
            JButton b = new JButton(nomes[i]);
            b.setMargin(new Insets(3, 8, 3, 8));
            b.addActionListener(e -> {
                if (k == 0) aoRelatorios.run();
                else if (k == 1) aoCorpo.run();
                else if (k == 2) aoAlimentacao.run();
                else aoEmail.run();
            });
            nav.add(b);
        }
        JPanel tx = new JPanel(new BorderLayout(0, 10));
        tx.setOpaque(false);
        tx.add(t, BorderLayout.NORTH);
        tx.add(nav, BorderLayout.SOUTH);
        p.add(tx, BorderLayout.CENTER);
        return p;
    }

    private JComponent formulario() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        f.setBorder(BorderFactory.createEmptyBorder(18, 22, 8, 22));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        int y = 0;

        secao(f, c, y++, "Seus dados");
        linha(f, c, y++, "Nome:", campoNome);
        linha(f, c, y++, "Celular:", campoCelular);
        campoEmail.setMinimumSize(campoEmail.getPreferredSize());
        linha(f, c, y++, "E-mail:", campoEmail);

        secao(f, c, y++, "Lembretes");
        JPanel intervalo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        intervalo.setOpaque(false);
        intervalo.add(campoIntervalo);
        intervalo.add(new JLabel("  minutos"));
        linha(f, c, y++, "Lembrar a cada:", intervalo);
        JPanel janela = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        janela.setOpaque(false);
        janela.add(campoInicio);
        janela.add(new JLabel("   até   "));
        janela.add(campoFim);
        linha(f, c, y++, "Horário:", janela);

        secao(f, c, y++, "Meta de água");
        campoPeso.setEditor(new JSpinner.NumberEditor(campoPeso, "0.0"));
        ((JSpinner.DefaultEditor) campoPeso.getEditor()).getTextField().setColumns(4);
        JPanel peso = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        peso.setOpaque(false);
        peso.add(campoPeso);
        peso.add(new JLabel("  kg   (opcional, para calcular a meta)"));
        linha(f, c, y++, "Seu peso:", peso);
        JPanel copo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        copo.setOpaque(false);
        copo.add(campoCopo);
        copo.add(new JLabel("  ml"));
        linha(f, c, y++, "Tamanho do copo:", copo);
        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        meta.setOpaque(false);
        meta.add(campoMeta);
        meta.add(new JLabel("  copos por dia  =  "));
        lblMetaTotal.setFont(TelaAlerta.fonte(Font.BOLD, 13));
        lblMetaTotal.setForeground(AZUL_ESCURO);
        meta.add(lblMetaTotal);
        linha(f, c, y++, "Meta diária:", meta);
        JPanel rec = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rec.setOpaque(false);
        lblRecomendado.setForeground(new Color(0x557085));
        rec.add(lblRecomendado);
        rec.add(Box.createHorizontalStrut(8));
        btnRecomendada.setMargin(new Insets(1, 6, 1, 6));
        rec.add(btnRecomendada);
        c.gridx = 1; c.gridy = y++; f.add(rec, c);
        javax.swing.event.ChangeListener ch = e -> atualizarMeta();
        campoPeso.addChangeListener(ch);
        campoCopo.addChangeListener(ch);
        campoMeta.addChangeListener(ch);
        btnRecomendada.addActionListener(e -> {
            int copos = coposRecomendados();
            if (copos > 0) campoMeta.setValue(Math.min(40, copos));
        });
        atualizarMeta();

        campoAtivo.setOpaque(false);
        campoIniciarWindows.setOpaque(false);
        JPanel chks = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chks.setOpaque(false);
        chks.add(campoAtivo);
        chks.add(Box.createHorizontalStrut(16));
        chks.add(campoIniciarWindows);
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; f.add(chks, c); c.gridwidth = 1;

        lblProximo.setForeground(AZUL_ESCURO);
        lblProximo.setFont(TelaAlerta.fonte(Font.BOLD, 13));
        lblHoje.setForeground(new Color(0x555555));
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.insets = new Insets(14, 4, 2, 8);
        f.add(lblProximo, c);
        c.gridy = y; c.insets = new Insets(2, 4, 6, 8);
        f.add(lblHoje, c);
        return f;
    }

    private JComponent rodape() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        p.setBackground(new Color(0xF2F8FC));
        JButton banco = new JButton("Banco");
        banco.setToolTipText("Banco de dados local ou em nuvem");
        banco.addActionListener(e -> aoBanco.run());
        JButton cel = new JButton("Celular");
        cel.setToolTipText("Conectar o aplicativo Android");
        cel.addActionListener(e -> aoCelular.run());
        p.add(banco);
        p.add(cel);
        JButton testar = new JButton("Testar alerta");
        JButton fechar = new JButton("Fechar");
        JButton salvar = new JButton("Salvar");
        salvar.setFont(salvar.getFont().deriveFont(Font.BOLD));
        testar.addActionListener(e -> aoTestar.run());
        fechar.addActionListener(e -> setVisible(false));
        salvar.addActionListener(e -> salvar());
        p.add(testar);
        p.add(fechar);
        p.add(salvar);
        getRootPane().setDefaultButton(salvar);
        return p;
    }

    private static void secao(JPanel f, GridBagConstraints c, int y, String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(TelaAlerta.fonte(Font.BOLD, 14));
        l.setForeground(AZUL);
        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        Insets antes = c.insets;
        c.insets = new Insets(y == 0 ? 0 : 12, 4, 2, 8);
        f.add(l, c);
        c.insets = antes;
        c.gridwidth = 1;
    }

    private static void linha(JPanel f, GridBagConstraints c, int y, String rotulo, JComponent campo) {
        c.gridx = 0; c.gridy = y; c.weightx = 0;
        f.add(new JLabel(rotulo), c);
        c.gridx = 1; c.weightx = 1;
        f.add(campo, c);
    }

    private static JSpinner spinnerHora() {
        JSpinner s = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(4);
        return s;
    }

    private static LocalTime hora(JSpinner s) {
        Date d = (Date) s.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }

    private static void definirHora(JSpinner s, LocalTime t) {
        Date d = Date.from(t.atDate(java.time.LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant());
        s.setValue(d);
    }

    public void carregar(Usuario u) {
        this.usuario = u;
        campoNome.setText(u.getNome());
        campoCelular.setValue(null);
        campoCelular.setText(u.getCelular());
        campoEmail.setText(u.getEmail());
        if (u.getCelular() != null && !u.getCelular().trim().isEmpty()) {
            try { campoCelular.commitEdit(); } catch (ParseException ignored) { }
        }
        campoIntervalo.setValue(u.getIntervaloMinutos());
        definirHora(campoInicio, u.getHoraInicio());
        definirHora(campoFim, u.getHoraFim());
        campoMeta.setValue(u.getMetaCopos());
        campoPeso.setValue(u.getPesoKg());
        campoCopo.setValue(u.getCopoMl());
        atualizarMeta();
        campoAtivo.setSelected(u.isAtivo());
        campoIniciarWindows.setSelected(u.isIniciarComWindows());
    }

    private int coposRecomendados() {
        double peso = ((Number) campoPeso.getValue()).doubleValue();
        int copo = ((Number) campoCopo.getValue()).intValue();
        if (peso <= 0) return 0;
        return (int) Math.ceil(peso * Usuario.ML_POR_KG / Math.max(1, copo));
    }

    private void atualizarMeta() {
        int copos = ((Number) campoMeta.getValue()).intValue();
        int copo = ((Number) campoCopo.getValue()).intValue();
        double peso = ((Number) campoPeso.getValue()).doubleValue();
        lblMetaTotal.setText(Usuario.formatarMl(copos * copo) + " por dia");
        if (peso > 0) {
            int recMl = (int) Math.round(peso * Usuario.ML_POR_KG);
            lblRecomendado.setText("Recomendado (35 ml/kg): " + Usuario.formatarMl(recMl)
                    + " = " + coposRecomendados() + " copos");
            btnRecomendada.setEnabled(coposRecomendados() != copos);
        } else {
            lblRecomendado.setText("Informe o peso para ver a meta recomendada.");
            btnRecomendada.setEnabled(false);
        }
    }

    public void atualizarStatus(LocalDateTime proximo, int coposHoje, int mlHoje, int metaMl) {
        SwingUtilities.invokeLater(() -> {
            if (proximo == null) {
                lblProximo.setText("Próximo lembrete: — (cadastre-se e ative os lembretes)");
            } else {
                String dia = proximo.toLocalDate().equals(java.time.LocalDate.now()) ? "hoje" : "amanhã";
                lblProximo.setText("Próximo lembrete: " + dia + " às "
                        + proximo.format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            lblHoje.setText("Hoje você bebeu " + Usuario.formatarMl(mlHoje) + " (" + coposHoje + " copos) de "
                    + Usuario.formatarMl(metaMl) + ".");
        });
    }

    private void salvar() {
        String nome = campoNome.getText().trim();
        String celular = campoCelular.getText().trim();
        String digitos = celular.replaceAll("\\D", "");
        if (nome.length() < 2) {
            erro("Informe o seu nome.", campoNome);
            return;
        }
        if (digitos.length() < 10) {
            erro("Informe um celular válido com DDD, ex.: (11) 98765-4321.", campoCelular);
            return;
        }
        String email = campoEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            erro("E-mail inválido. Exemplo: nome@gmail.com", campoEmail);
            return;
        }
        LocalTime ini = hora(campoInicio);
        LocalTime fim = hora(campoFim);
        if (ini.equals(fim)) {
            erro("O horário inicial e o final não podem ser iguais.", campoInicio);
            return;
        }
        Usuario u = usuario != null ? usuario : new Usuario();
        u.setNome(nome);
        u.setCelular(celular);
        u.setEmail(campoEmail.getText().trim());
        u.setIntervaloMinutos((Integer) campoIntervalo.getValue());
        u.setHoraInicio(ini);
        u.setHoraFim(fim);
        u.setMetaCopos((Integer) campoMeta.getValue());
        u.setPesoKg(((Number) campoPeso.getValue()).doubleValue());
        u.setCopoMl(((Number) campoCopo.getValue()).intValue());
        u.setAtivo(campoAtivo.isSelected());
        u.setIniciarComWindows(campoIniciarWindows.isSelected());
        aoSalvar.accept(u);
    }

    private void erro(String msg, JComponent foco) {
        JOptionPane.showMessageDialog(this, msg, "Aviso de Água", JOptionPane.WARNING_MESSAGE);
        foco.requestFocusInWindow();
    }

    private Runnable aoBanco = () -> { }, aoCelular = () -> { };

    public void definirAtalhosExtras(Runnable banco, Runnable celular) {
        this.aoBanco = banco;
        this.aoCelular = celular;
    }

    public void definirAtalhos(Runnable corpo, Runnable alimentacao, Runnable email) {
        this.aoCorpo = corpo;
        this.aoAlimentacao = alimentacao;
        this.aoEmail = email;
    }

    public void abrir() {
        setVisible(true);
        setExtendedState(NORMAL);
        toFront();
        requestFocus();
    }
}
