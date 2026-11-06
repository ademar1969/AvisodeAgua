package br.com.avisodeagua;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Aviso de Água - fica rodando em segundo plano (ícone na bandeja, perto do relógio)
 * e abre uma tela com uma cachoeira na hora de tomar água.
 */
public class App {

    private static final int PORTA_INSTANCIA = 47321;

    private final Database db = new Database();
    private final Agendador agendador = new Agendador(this::dispararAlerta);
    private TelaCadastro tela;
    private TelaRelatorios relatorios;
    private TelaCorpo telaCorpo;
    private TelaAlimentacao telaAlimentacao;
    private java.time.LocalDate ultimaSugestaoMedidas;
    private ServidorCelular servidorCelular;
    private TrayIcon trayIcon;
    private Usuario usuario = new Usuario();
    private boolean bancoOk;

    public static void main(String[] args) {
        boolean oculto = args.length > 0 && args[0].equalsIgnoreCase("--oculto");
        if (!garantirInstanciaUnica()) return; // já está rodando: a outra instância abre a tela
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new App().iniciar(oculto));
    }

    private void iniciar(boolean oculto) {
        tela = new TelaCadastro(this::salvar, () -> dispararAlerta(usuarioParaTeste()), this::abrirRelatorios);
        relatorios = new TelaRelatorios(db);
        telaCorpo = new TelaCorpo(db, this::atualizarStatus);
        telaAlimentacao = new TelaAlimentacao(db);
        tela.definirAtalhos(this::abrirCorpo, this::abrirAlimentacao, this::abrirEmail);
        tela.definirAtalhosExtras(this::abrirBanco, this::abrirCelular);
        instalarBandeja();
        conectarBanco();

        tela.carregar(usuario);
        servidorCelular = new ServidorCelular(db, new CorpoDao(db), new AlimentacaoDao(db), () -> usuario,
                () -> SwingUtilities.invokeLater(this::atualizarStatus));
        if (servidorCelular.ativoNaConfig()) {
            try { servidorCelular.iniciar(); } catch (Exception e) {
                System.err.println("API do celular não iniciou: " + e.getMessage());
            }
        }
        agendador.setAoMudarProximo(this::atualizarStatus);
        agendador.atualizarUsuario(usuario);
        agendador.iniciar();
        // sugestão de atualizar as medidas: 1 minuto após abrir e depois a cada hora
        javax.swing.Timer t = new javax.swing.Timer(60 * 60 * 1000, e -> sugerirAtualizarMedidas());
        t.setInitialDelay(60 * 1000);
        t.start();

        if (!usuario.isCadastrado() || !oculto || trayIcon == null) {
            tela.abrir();
        } else {
            notificar("Aviso de Água está ativo", "Vou te lembrar de beber água. 💧");
        }
    }

    private void conectarBanco() {
        while (true) {
            try {
                db.inicializar();
                usuario = db.carregarUsuario();
                bancoOk = true;
                return;
            } catch (SQLException e) {
                bancoOk = false;
                int op = JOptionPane.showOptionDialog(null,
                        "Não foi possível conectar ao banco MySQL:\n" + db.descricaoConexao()
                                + "\n\nErro: " + e.getMessage()
                                + "\n\nVerifique se o MySQL está instalado e em execução.\n"
                                + "As configurações ficam em: " + Database.pastaDados().resolve("config.properties"),
                        "Aviso de Água - Banco de dados", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                        null, new Object[]{"Tentar novamente", "Configurar banco (local / nuvem)", "Continuar sem banco", "Sair"},
                        "Tentar novamente");
                if (op == 0) continue;
                if (op == 1) { new TelaBanco(null, db, () -> { }).setVisible(true); continue; }
                if (op == 3 || op == JOptionPane.CLOSED_OPTION) System.exit(0);
                return;
            }
        }
    }

    private void salvar(Usuario u) {
        try {
            if (!bancoOk) { db.inicializar(); bancoOk = true; }
            db.salvarUsuario(u);
            usuario = u;
            InicializacaoWindows.configurar(u.isIniciarComWindows());
            agendador.atualizarUsuario(u);
            String prox = agendador.getProximo() == null ? "lembretes desativados"
                    : "próximo lembrete às " + agendador.getProximo().format(DateTimeFormatter.ofPattern("HH:mm"));
            JOptionPane.showMessageDialog(tela, "Dados salvos com sucesso!\n" + prox
                            + "\n\nO programa continua rodando perto do relógio do Windows.",
                    "Aviso de Água", JOptionPane.INFORMATION_MESSAGE);
            tela.setVisible(false);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(tela, "Erro ao salvar no MySQL:\n" + e.getMessage(),
                    "Aviso de Água", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirBanco() {
        new TelaBanco(tela.isVisible() ? tela : null, db, this::bancoTrocado).setVisible(true);
    }

    /** Depois de trocar entre banco local e nuvem: recarrega tudo do novo banco. */
    private void bancoTrocado() {
        try {
            db.inicializar();
            usuario = db.carregarUsuario();
            bancoOk = true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Erro ao ler o novo banco:\n" + e.getMessage(),
                    "Banco de dados", JOptionPane.ERROR_MESSAGE);
            return;
        }
        telaCorpo.setVisible(false);
        telaAlimentacao.setVisible(false);
        relatorios.setVisible(false);
        tela.carregar(usuario);
        agendador.atualizarUsuario(usuario);
        atualizarStatus();
        notificar("Banco de dados", "Conectado em " + db.descricaoConexao());
        if (!usuario.isCadastrado()) tela.abrir();
    }

    private void abrirCelular() {
        if (exigirCadastro()) new TelaCelular(tela.isVisible() ? tela : null, servidorCelular).setVisible(true);
    }

    private boolean exigirCadastro() {
        if (usuario.isCadastrado() && usuario.getId() > 0) return true;
        JOptionPane.showMessageDialog(tela, "Faça e salve o seu cadastro primeiro.",
                "Aviso de Água", JOptionPane.INFORMATION_MESSAGE);
        tela.abrir();
        return false;
    }

    private void abrirCorpo() {
        if (exigirCadastro()) telaCorpo.abrir(usuario);
    }

    private void abrirAlimentacao() {
        if (exigirCadastro()) telaAlimentacao.abrir(usuario);
    }

    private void abrirEmail() {
        if (exigirCadastro()) new TelaEmail(tela.isVisible() ? tela : null, db, usuario).setVisible(true);
    }

    /** Sugere atualizar as medidas quando passou o prazo escolhido (uma vez por dia). */
    private void sugerirAtualizarMedidas() {
        if (!usuario.isCadastrado() || usuario.getId() <= 0 || usuario.getLembreteMedidasDias() <= 0) return;
        java.time.LocalDate hoje = java.time.LocalDate.now();
        if (hoje.equals(ultimaSugestaoMedidas) || java.time.LocalTime.now().getHour() < 7) return;
        java.time.LocalDate ult = new CorpoDao(db).ultimaData(usuario.getId());
        String msg;
        if (ult == null) {
            msg = "Que tal registrar suas medidas do corpo para acompanhar a sua evolução?";
        } else {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(ult, hoje);
            if (dias < usuario.getLembreteMedidasDias()) return;
            msg = "Sua última medição foi há " + dias + " dias. Está na hora de atualizar peso e medidas para acompanhar o progresso.";
        }
        ultimaSugestaoMedidas = hoje;
        notificar("Hora de atualizar suas medidas", msg);
        int op = JOptionPane.showOptionDialog(null, msg, "Aviso de Água - Medidas do corpo",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{"Atualizar agora", "Depois"}, "Atualizar agora");
        if (op == 0) abrirCorpo();
    }

    private void abrirRelatorios() {
        if (!usuario.isCadastrado()) {
            JOptionPane.showMessageDialog(tela, "Faça e salve o seu cadastro primeiro.",
                    "Aviso de Água", JOptionPane.INFORMATION_MESSAGE);
            tela.abrir();
            return;
        }
        relatorios.abrir(usuario);
    }

    /** Registra um copo bebido (pelo alerta ou pelo menu da bandeja). */
    private void registrarCopo(Usuario u, String acao) {
        int antes = db.mlHoje(u);
        db.registrar(u.getId(), acao, u.getCopoMl());
        int depois = db.mlHoje(u);
        int meta = u.getMetaMl();
        if (meta > 0 && antes < meta && depois >= meta) {
            notificar("Parabéns, " + u.getNome().trim().split("\\s+")[0] + "!",
                    "Você atingiu a meta de " + Usuario.formatarMl(meta) + " de água hoje.");
        }
        atualizarStatus();
        if (relatorios != null && relatorios.isVisible()) relatorios.atualizar();
    }

    private Usuario usuarioParaTeste() {
        if (usuario.isCadastrado()) return usuario;
        Usuario t = new Usuario();
        t.setNome("Você");
        return t;
    }

    private void dispararAlerta(Usuario u) {
        SwingUtilities.invokeLater(() -> {
            agendador.setAlertaAberto(true);
            int copos = db.coposHoje(u.getId());
            int ml = db.mlHoje(u);
            TelaAlerta alerta = new TelaAlerta(u, copos, ml, new TelaAlerta.Resposta() {
                @Override public void bebeu() {
                    registrarCopo(u, Database.ACAO_BEBEU);
                }
                @Override public void adiar(int minutos) {
                    db.registrar(u.getId(), Database.ACAO_ADIOU, 0);
                    agendador.adiar(minutos);
                }
                @Override public void ignorou() {
                    db.registrar(u.getId(), Database.ACAO_IGNOROU, 0);
                }
            });
            alerta.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent e) {
                    agendador.setAlertaAberto(false);
                    atualizarStatus();
                }
            });
            alerta.mostrar();
        });
    }

    private void atualizarStatus() {
        int copos = db.coposHoje(usuario.getId());
        int ml = db.mlHoje(usuario);
        if (tela != null) tela.atualizarStatus(agendador.getProximo(), copos, ml, usuario.getMetaMl());
        if (trayIcon != null) {
            String dica = "Aviso de Água - hoje " + Usuario.formatarMl(ml) + " de " + Usuario.formatarMl(usuario.getMetaMl());
            if (agendador.getProximo() != null) {
                dica += " - próximo: " + agendador.getProximo().format(DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                dica += " - lembretes pausados";
            }
            trayIcon.setToolTip(dica);
        }
    }

    private void instalarBandeja() {
        if (!SystemTray.isSupported()) return;
        PopupMenu menu = new PopupMenu();
        MenuItem abrir = new MenuItem("Abrir cadastro");
        MenuItem agora = new MenuItem("Tomar água agora");
        MenuItem bebi = new MenuItem("Registrar copo bebido");
        MenuItem rel = new MenuItem("Relatórios da água");
        MenuItem corpo = new MenuItem("Medidas do corpo");
        MenuItem comida = new MenuItem("Alimentação do dia");
        MenuItem enviar = new MenuItem("Enviar relatório por e-mail");
        corpo.addActionListener(e -> abrirCorpo());
        comida.addActionListener(e -> abrirAlimentacao());
        enviar.addActionListener(e -> { if (exigirCadastro()) TelaEmail.enviarRelatorio(null, db, usuario); });
        bebi.addActionListener(e -> {
            if (!usuario.isCadastrado()) { tela.abrir(); return; }
            registrarCopo(usuario, Database.ACAO_MANUAL);
            notificar("Copo registrado", "Hoje: " + Usuario.formatarMl(db.mlHoje(usuario)) + " de "
                    + Usuario.formatarMl(usuario.getMetaMl()));
        });
        rel.addActionListener(e -> abrirRelatorios());
        CheckboxMenuItem pausar = new CheckboxMenuItem("Pausar lembretes");
        MenuItem sair = new MenuItem("Sair");
        abrir.addActionListener(e -> tela.abrir());
        agora.addActionListener(e -> dispararAlerta(usuarioParaTeste()));
        pausar.addItemListener(e -> {
            usuario.setAtivo(!pausar.getState());
            agendador.atualizarUsuario(usuario);
        });
        sair.addActionListener(e -> {
            agendador.parar();
            SystemTray.getSystemTray().remove(trayIcon);
            System.exit(0);
        });
        menu.add(abrir);
        menu.add(agora);
        menu.add(bebi);
        menu.add(rel);
        menu.addSeparator();
        menu.add(corpo);
        menu.add(comida);
        menu.add(enviar);
        menu.addSeparator();
        MenuItem cel = new MenuItem("Aplicativo do celular");
        MenuItem banco = new MenuItem("Banco de dados (local / nuvem)");
        cel.addActionListener(e -> abrirCelular());
        banco.addActionListener(e -> abrirBanco());
        menu.add(cel);
        menu.add(banco);
        menu.addSeparator();
        menu.add(pausar);
        menu.addSeparator();
        menu.add(sair);

        Dimension d = SystemTray.getSystemTray().getTrayIconSize();
        trayIcon = new TrayIcon(Icones.gota(Math.max(16, d.width)), "Aviso de Água", menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> tela.abrir()); // duplo clique
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            trayIcon = null;
        }
    }

    private void notificar(String titulo, String msg) {
        if (trayIcon != null) trayIcon.displayMessage(titulo, msg, TrayIcon.MessageType.INFO);
    }

    /**
     * Garante uma única instância. Se já houver uma rodando, pede para ela abrir a tela de cadastro.
     */
    private static boolean garantirInstanciaUnica() {
        try {
            ServerSocket ss = new ServerSocket(PORTA_INSTANCIA, 5, InetAddress.getLoopbackAddress());
            Thread t = new Thread(() -> {
                while (true) {
                    try (Socket s = ss.accept()) {
                        SwingUtilities.invokeLater(() -> {
                            for (Window w : Window.getWindows()) {
                                if (w instanceof TelaCadastro) ((TelaCadastro) w).abrir();
                            }
                        });
                    } catch (IOException ignored) { }
                }
            }, "instancia-unica");
            t.setDaemon(true);
            t.start();
            return true;
        } catch (IOException e) {
            try (Socket s = new Socket(InetAddress.getLoopbackAddress(), PORTA_INSTANCIA)) {
                return false;
            } catch (IOException ex) {
                return true;
            }
        }
    }
}
