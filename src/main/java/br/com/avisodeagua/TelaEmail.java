package br.com.avisodeagua;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Configuração do e-mail de envio e envio do relatório de acompanhamento. */
public class TelaEmail extends JDialog {

    private static final String[][] PROVEDORES = {
            {"Gmail", "smtp.gmail.com", "465", "SSL"},
            {"Outlook / Hotmail / Microsoft 365", "smtp.office365.com", "587", "STARTTLS"},
            {"Yahoo", "smtp.mail.yahoo.com", "465", "SSL"},
            {"Outro (preencher manualmente)", "", "587", "STARTTLS"}};

    private final Database db;
    private final Usuario usuario;
    private final JComboBox<String> comboProvedor = new JComboBox<String>();
    private final JTextField campoServidor = new JTextField(22);
    private final JSpinner campoPorta = new JSpinner(new SpinnerNumberModel(465, 1, 65535, 1));
    private final JComboBox<String> comboSeg = new JComboBox<String>(new String[]{"SSL", "STARTTLS", "NENHUMA"});
    private final JTextField campoUsuario = new JTextField(22);
    private final JPasswordField campoSenha = new JPasswordField(22);
    private final JTextField campoDest = new JTextField(30);
    private final JCheckBox campoFotos = new JCheckBox("Anexar as fotos da última medição");

    public TelaEmail(Window dono, Database db, Usuario u) {
        super(dono, "Aviso de Água - E-mail", ModalityType.APPLICATION_MODAL);
        this.db = db;
        this.usuario = u;
        setIconImage(Icones.gota(64));
        campoPorta.setEditor(new JSpinner.NumberEditor(campoPorta, "#"));
        for (String[] p : PROVEDORES) comboProvedor.addItem(p[0]);

        JPanel f = new JPanel(new GridBagLayout());
        f.setBorder(new EmptyBorder(16, 18, 8, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 6);
        c.anchor = GridBagConstraints.WEST;
        int y = 0;
        JLabel info = new JLabel("<html><b>Conta que vai ENVIAR os e-mails</b><br>"
                + "<small>Gmail: ative a verificação em 2 etapas e crie uma <b>senha de app</b> em "
                + "myaccount.google.com &gt; Segurança &gt; Senhas de app. Use essa senha aqui.</small></html>");
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; f.add(info, c); c.gridwidth = 1;
        add(f, c, y++, "Provedor:", comboProvedor);
        add(f, c, y++, "Servidor SMTP:", campoServidor);
        JPanel ps = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ps.add(campoPorta);
        ps.add(new JLabel("   Segurança: "));
        ps.add(comboSeg);
        add(f, c, y++, "Porta:", ps);
        add(f, c, y++, "Seu e-mail:", campoUsuario);
        add(f, c, y++, "Senha (de app):", campoSenha);
        JLabel l2 = new JLabel("<html><b>Para quem enviar</b> <small>(separe vários com ; — ex.: você e seu nutricionista)</small></html>");
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.insets = new Insets(14, 4, 5, 6); f.add(l2, c);
        c.gridwidth = 1; c.insets = new Insets(5, 4, 5, 6);
        add(f, c, y++, "Destinatários:", campoDest);
        c.gridx = 1; c.gridy = y; f.add(campoFotos, c);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton teste = new JButton("Enviar teste");
        JButton relatorio = new JButton("Enviar relatório agora");
        JButton salvar = new JButton("Salvar");
        JButton fechar = new JButton("Fechar");
        botoes.add(teste);
        botoes.add(relatorio);
        botoes.add(fechar);
        botoes.add(salvar);
        teste.addActionListener(e -> { if (salvar(false)) enviarTeste(); });
        relatorio.addActionListener(e -> { if (salvar(false)) enviarRelatorio(this, db, usuario); });
        salvar.addActionListener(e -> { if (salvar(true)) dispose(); });
        fechar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(salvar);

        comboProvedor.addActionListener(e -> {
            String[] p = PROVEDORES[comboProvedor.getSelectedIndex()];
            if (!p[1].isEmpty()) campoServidor.setText(p[1]);
            campoPorta.setValue(Integer.parseInt(p[2]));
            comboSeg.setSelectedItem(p[3]);
        });

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(f, BorderLayout.CENTER);
        raiz.add(botoes, BorderLayout.SOUTH);
        setContentPane(raiz);
        carregar();
        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private static void add(JPanel f, GridBagConstraints c, int y, String r, JComponent campo) {
        c.gridx = 0; c.gridy = y; f.add(new JLabel(r), c);
        c.gridx = 1; f.add(campo, c);
    }

    private void carregar() {
        Email.Config cfg = Email.carregar(db);
        campoServidor.setText(cfg.servidor);
        campoPorta.setValue(cfg.porta > 0 ? cfg.porta : 465);
        comboSeg.setSelectedItem(cfg.seguranca != null ? cfg.seguranca : "SSL");
        campoUsuario.setText(cfg.usuario != null && !cfg.usuario.isEmpty() ? cfg.usuario : usuario.getEmail());
        campoSenha.setText(cfg.senha);
        String dest = cfg.destinatarios != null && !cfg.destinatarios.isEmpty() ? cfg.destinatarios : usuario.getEmail();
        campoDest.setText(dest);
        campoFotos.setSelected(cfg.enviarFotos);
        for (int i = 0; i < PROVEDORES.length; i++) {
            if (PROVEDORES[i][1].equalsIgnoreCase(cfg.servidor)) {
                ActionListenerSilencioso.semEvento(comboProvedor, i);
                return;
            }
        }
        ActionListenerSilencioso.semEvento(comboProvedor, PROVEDORES.length - 1);
    }

    private boolean salvar(boolean avisar) {
        Email.Config cfg = new Email.Config();
        cfg.servidor = campoServidor.getText().trim();
        cfg.porta = (Integer) campoPorta.getValue();
        cfg.seguranca = (String) comboSeg.getSelectedItem();
        cfg.usuario = campoUsuario.getText().trim();
        cfg.senha = new String(campoSenha.getPassword());
        cfg.remetente = cfg.usuario;
        cfg.destinatarios = campoDest.getText().trim();
        cfg.enviarFotos = campoFotos.isSelected();
        if (cfg.servidor.isEmpty() || !cfg.usuario.contains("@")) {
            JOptionPane.showMessageDialog(this, "Informe o servidor SMTP e o seu e-mail.", "E-mail", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (Email.separar(cfg.destinatarios).isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe pelo menos um destinatário.", "E-mail", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        try {
            Email.salvar(db, cfg);
            if (avisar) JOptionPane.showMessageDialog(this, "Configuração de e-mail salva.", "E-mail", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar no MySQL:\n" + e.getMessage(), "E-mail", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void enviarTeste() {
        final Email.Config cfg = Email.carregar(db);
        executar(this, "Enviando e-mail de teste...", () -> {
            Email.enviar(cfg, Email.separar(cfg.destinatarios), "Teste - Aviso de Água",
                    "<p style=\"font-family:Arial\">Olá, " + usuario.getNome()
                            + "!</p><p style=\"font-family:Arial\">O envio de e-mails do <b>Aviso de Água</b> está funcionando.</p>",
                    new java.util.ArrayList<Email.Anexo>());
            return "E-mail de teste enviado para:\n" + cfg.destinatarios;
        });
    }

    /** Monta e envia o relatório completo (usado também pelo menu e pela tela de medidas). */
    public static void enviarRelatorio(Component pai, Database db, Usuario u) {
        final Email.Config cfg = Email.carregar(db);
        if (!cfg.configurado() || Email.separar(cfg.destinatarios).isEmpty()) {
            JOptionPane.showMessageDialog(pai, "Configure o e-mail primeiro (botão \"E-mail\").",
                    "E-mail", JOptionPane.INFORMATION_MESSAGE);
            Window w = pai instanceof Window ? (Window) pai : (pai != null ? SwingUtilities.getWindowAncestor(pai) : null);
            new TelaEmail(w, db, u).setVisible(true);
            return;
        }
        executar(pai, "Montando e enviando o relatório...", () -> {
            List<Medida> medidas = new CorpoDao(db).listar(u.getId());
            RelatorioEmail.Conteudo c = RelatorioEmail.montar(db, u, medidas, cfg.enviarFotos);
            Email.enviar(cfg, Email.separar(cfg.destinatarios), c.assunto, c.html, c.anexos);
            return "Relatório enviado para:\n" + cfg.destinatarios;
        });
    }

    interface Tarefa { String rodar() throws Exception; }

    /** Roda em segundo plano mostrando uma janelinha de espera. */
    static void executar(Component pai, String msg, Tarefa t) {
        final Window dono = pai == null ? null : pai instanceof Window ? (Window) pai : SwingUtilities.getWindowAncestor(pai);
        final JDialog espera = new JDialog(dono, "Aguarde", ModalityType.APPLICATION_MODAL);
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.add(new JLabel(msg), BorderLayout.NORTH);
        p.add(pb, BorderLayout.CENTER);
        espera.setContentPane(p);
        espera.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        espera.pack();
        espera.setLocationRelativeTo(dono);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return t.rodar(); }
            @Override protected void done() {
                espera.dispose();
                try {
                    JOptionPane.showMessageDialog(dono, get(), "E-mail", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(dono, "Não foi possível enviar:\n" + causa.getMessage(),
                            "E-mail", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
        espera.setVisible(true);
    }

    /** Seleciona um item do combo sem disparar o preenchimento automático. */
    private static final class ActionListenerSilencioso {
        static void semEvento(JComboBox<String> combo, int i) {
            java.awt.event.ActionListener[] ls = combo.getActionListeners();
            for (java.awt.event.ActionListener l : ls) combo.removeActionListener(l);
            combo.setSelectedIndex(i);
            for (java.awt.event.ActionListener l : ls) combo.addActionListener(l);
        }
    }
}
