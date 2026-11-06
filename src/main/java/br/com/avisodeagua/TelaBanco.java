package br.com.avisodeagua;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Properties;

/** Escolha do banco de dados: local (MariaDB/MySQL no computador) ou em nuvem. */
public class TelaBanco extends JDialog {

    private final Database db;
    private final Runnable aoTrocar;
    private final JRadioButton rbLocal = new JRadioButton("Banco local (MariaDB / MySQL neste computador)");
    private final JRadioButton rbNuvem = new JRadioButton("Banco em nuvem (servidor MySQL / MariaDB na internet)");
    private final JTextField campoHost = new JTextField(26);
    private final JSpinner campoPorta = new JSpinner(new SpinnerNumberModel(3306, 1, 65535, 1));
    private final JTextField campoBanco = new JTextField(16);
    private final JTextField campoUsuario = new JTextField(16);
    private final JPasswordField campoSenha = new JPasswordField(16);
    private final JCheckBox chkSsl = new JCheckBox("Conexão segura (SSL) — normalmente obrigatório na nuvem");
    private final JLabel lblAtual = new JLabel();

    public TelaBanco(Window dono, Database db, Runnable aoTrocar) {
        super(dono, "Aviso de Água - Banco de dados", ModalityType.APPLICATION_MODAL);
        this.db = db;
        this.aoTrocar = aoTrocar;
        setIconImage(Icones.gota(64));
        campoPorta.setEditor(new JSpinner.NumberEditor(campoPorta, "#"));
        ButtonGroup g = new ButtonGroup();
        g.add(rbLocal);
        g.add(rbNuvem);

        JPanel f = new JPanel(new GridBagLayout());
        f.setBorder(new EmptyBorder(16, 18, 8, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 6);
        int y = 0;
        lblAtual.setForeground(new Color(0x557085));
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; f.add(lblAtual, c);
        f.add(rbLocal, gbc(c, 0, y++));
        f.add(rbNuvem, gbc(c, 0, y++));
        JLabel dica = new JLabel("<html><div style='width:420px'><small>Na nuvem use um serviço com MySQL ou MariaDB "
                + "(ex.: Aiven, Railway, AWS RDS, Azure, Google Cloud SQL, ou o MySQL da sua hospedagem). "
                + "Copie do painel do serviço o endereço (host), porta, nome do banco, usuário e senha. "
                + "Com a nuvem, você acessa os mesmos dados de qualquer computador.</small></div></html>");
        f.add(dica, gbc(c, 0, y++));
        c.gridwidth = 1;
        linha(f, c, y++, "Servidor (host):", campoHost);
        linha(f, c, y++, "Porta:", campoPorta);
        linha(f, c, y++, "Nome do banco:", campoBanco);
        linha(f, c, y++, "Usuário:", campoUsuario);
        linha(f, c, y++, "Senha:", campoSenha);
        c.gridx = 1; c.gridy = y; f.add(chkSsl, c);

        rbLocal.addActionListener(e -> preencherLocal());
        rbNuvem.addActionListener(e -> { if (campoHost.getText().equals("localhost")) { campoHost.setText(""); chkSsl.setSelected(true); } });

        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton testar = new JButton("Testar conexão");
        JButton salvar = new JButton("Salvar e conectar");
        salvar.setFont(salvar.getFont().deriveFont(Font.BOLD));
        JButton fechar = new JButton("Fechar");
        testar.addActionListener(e -> testar());
        salvar.addActionListener(e -> salvar());
        fechar.addActionListener(e -> dispose());
        b.add(testar);
        b.add(fechar);
        b.add(salvar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(f, BorderLayout.CENTER);
        raiz.add(b, BorderLayout.SOUTH);
        setContentPane(raiz);
        carregar();
        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private static GridBagConstraints gbc(GridBagConstraints c, int x, int y) {
        c.gridx = x; c.gridy = y; c.gridwidth = 2;
        return c;
    }

    private static void linha(JPanel f, GridBagConstraints c, int y, String r, JComponent campo) {
        c.gridx = 0; c.gridy = y; c.gridwidth = 1; f.add(new JLabel(r), c);
        c.gridx = 1; f.add(campo, c);
    }

    private void carregar() {
        Properties p = Database.carregarConfig();
        boolean nuvem = "nuvem".equalsIgnoreCase(p.getProperty("db.tipo", "local"));
        (nuvem ? rbNuvem : rbLocal).setSelected(true);
        campoHost.setText(p.getProperty("db.host", "localhost"));
        try { campoPorta.setValue(Integer.parseInt(p.getProperty("db.porta", "3306").trim())); } catch (Exception ignored) { }
        campoBanco.setText(p.getProperty("db.nome", "avisodeagua"));
        campoUsuario.setText(p.getProperty("db.usuario", "root"));
        campoSenha.setText(p.getProperty("db.senha", "100219"));
        chkSsl.setSelected("true".equalsIgnoreCase(p.getProperty("db.ssl", "false")));
        lblAtual.setText("Conectado agora em: " + db.descricaoConexao());
    }

    private void preencherLocal() {
        Properties p = Database.configLocal();
        campoHost.setText(p.getProperty("db.host"));
        campoPorta.setValue(3306);
        campoBanco.setText(p.getProperty("db.nome"));
        campoUsuario.setText(p.getProperty("db.usuario"));
        campoSenha.setText(p.getProperty("db.senha"));
        chkSsl.setSelected(false);
    }

    private Properties lerTela() {
        Properties p = new Properties();
        p.setProperty("db.tipo", rbNuvem.isSelected() ? "nuvem" : "local");
        p.setProperty("db.host", campoHost.getText().trim());
        p.setProperty("db.porta", String.valueOf(campoPorta.getValue()));
        p.setProperty("db.nome", campoBanco.getText().trim().isEmpty() ? "avisodeagua" : campoBanco.getText().trim());
        p.setProperty("db.usuario", campoUsuario.getText().trim());
        p.setProperty("db.senha", new String(campoSenha.getPassword()));
        p.setProperty("db.ssl", String.valueOf(chkSsl.isSelected()));
        return p;
    }

    private boolean validar(Properties p) {
        if (p.getProperty("db.host").isEmpty() || p.getProperty("db.usuario").isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o servidor e o usuário.", "Banco de dados", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void testar() {
        final Properties p = lerTela();
        if (!validar(p)) return;
        TelaEmail.executar(this, "Testando a conexão...", () -> {
            Database teste = new Database(p);
            String versao = teste.testar();
            return "Conexão OK!\nServidor: " + versao + "\n" + teste.descricaoConexao();
        });
    }

    private void salvar() {
        final Properties p = lerTela();
        if (!validar(p)) return;
        final Database novo = new Database(p);
        final boolean[] vazio = {false};
        final String[] erro = {null};
        TelaEmail.executar(this, "Conectando e preparando as tabelas...", () -> {
            try {
                novo.testar();
                vazio[0] = CopiadorDados.destinoVazio(novo);
                return "Conectado: " + novo.descricaoConexao();
            } catch (Exception e) {
                erro[0] = e.getMessage();
                throw e;
            }
        });
        if (erro[0] != null) return;

        // banco novo vazio: oferece copiar os dados do banco atual
        if (vazio[0]) {
            int op = JOptionPane.showConfirmDialog(this,
                    "O banco escolhido ainda não tem dados.\n\nCopiar todos os seus dados do banco atual\n("
                            + db.descricaoConexao() + ")\npara o novo banco?",
                    "Copiar dados", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                TelaEmail.executar(this, "Copiando os dados (pode demorar se houver muitas fotos)...",
                        () -> "Dados copiados:\n\n" + CopiadorDados.copiar(db, novo));
            }
        }
        Database.salvarConfig(p);
        db.aplicar(p);
        aoTrocar.run();
        dispose();
    }
}
