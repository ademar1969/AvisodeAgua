package br.com.avisodeagua;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Envio de e-mail por SMTP (SSL ou STARTTLS) sem bibliotecas externas.
 * Funciona com Gmail (use uma "senha de app"), Outlook/Hotmail, Yahoo e servidores próprios.
 */
public final class Email {

    /** Configuração do servidor de envio (tabela config_email). */
    public static class Config {
        public String servidor = "smtp.gmail.com";
        public int porta = 465;
        public String seguranca = "SSL";   // SSL, STARTTLS ou NENHUMA
        public String usuario = "";
        public String senha = "";
        public String remetente = "";
        public String destinatarios = "";
        public boolean enviarFotos = true;

        public boolean configurado() {
            return servidor != null && !servidor.trim().isEmpty() && usuario != null && !usuario.trim().isEmpty();
        }
    }

    /** Arquivo anexado. Se contentId != null, a imagem aparece dentro do texto do e-mail. */
    public static class Anexo {
        public final String nome, tipo, contentId;
        public final byte[] dados;
        public Anexo(String nome, String tipo, byte[] dados, String contentId) {
            this.nome = nome; this.tipo = tipo; this.dados = dados; this.contentId = contentId;
        }
    }

    private Email() { }

    // ------------------------------------------------------------------ banco

    public static Config carregar(Database db) {
        Config c = new Config();
        try (Connection con = db.conectar();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM config_email WHERE id = 1")) {
            if (rs.next()) {
                c.servidor = rs.getString("servidor");
                c.porta = rs.getInt("porta");
                c.seguranca = rs.getString("seguranca");
                c.usuario = rs.getString("usuario");
                c.senha = rs.getString("senha");
                c.remetente = rs.getString("remetente");
                c.destinatarios = rs.getString("destinatarios");
                c.enviarFotos = rs.getBoolean("enviar_fotos");
            }
        } catch (SQLException e) {
            System.err.println("Falha ao ler configuração de e-mail: " + e.getMessage());
        }
        return c;
    }

    public static void salvar(Database db, Config c) throws SQLException {
        try (Connection con = db.conectar();
             PreparedStatement ps = con.prepareStatement(
                     "REPLACE INTO config_email (id, servidor, porta, seguranca, usuario, senha, remetente, destinatarios, enviar_fotos) "
                             + "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, c.servidor);
            ps.setInt(2, c.porta);
            ps.setString(3, c.seguranca);
            ps.setString(4, c.usuario);
            ps.setString(5, c.senha);
            ps.setString(6, c.remetente);
            ps.setString(7, c.destinatarios);
            ps.setBoolean(8, c.enviarFotos);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ SMTP

    public static void enviar(Config cfg, List<String> para, String assunto, String html, List<Anexo> anexos)
            throws IOException {
        if (para.isEmpty()) throw new IOException("Nenhum destinatário informado.");
        String remetente = cfg.remetente != null && !cfg.remetente.trim().isEmpty() ? cfg.remetente.trim() : cfg.usuario.trim();
        Socket sock;
        if ("SSL".equalsIgnoreCase(cfg.seguranca)) {
            sock = tls((SSLSocket) SSLSocketFactory.getDefault().createSocket(cfg.servidor, cfg.porta));
        } else {
            sock = new Socket();
            sock.connect(new InetSocketAddress(cfg.servidor, cfg.porta), 20000);
        }
        sock.setSoTimeout(60000);
        try {
            Conexao c = new Conexao(sock);
            c.esperar(220);
            c.comando("EHLO avisodeagua", 250);
            if ("STARTTLS".equalsIgnoreCase(cfg.seguranca)) {
                c.comando("STARTTLS", 220);
                sock = tls((SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                        .createSocket(sock, cfg.servidor, cfg.porta, true));
                sock.setSoTimeout(60000);
                c = new Conexao(sock);
                c.comando("EHLO avisodeagua", 250);
            }
            if (cfg.usuario != null && !cfg.usuario.trim().isEmpty()) {
                String plano = "\0" + cfg.usuario.trim() + "\0" + (cfg.senha == null ? "" : cfg.senha);
                String resp = c.comandoResposta("AUTH PLAIN " + b64(plano.getBytes(StandardCharsets.UTF_8)));
                if (!resp.startsWith("235")) {
                    // tenta AUTH LOGIN
                    c.comando("AUTH LOGIN", 334);
                    c.comando(b64(cfg.usuario.trim().getBytes(StandardCharsets.UTF_8)), 334);
                    String r2 = c.comandoResposta(b64((cfg.senha == null ? "" : cfg.senha).getBytes(StandardCharsets.UTF_8)));
                    if (!r2.startsWith("235"))
                        throw new IOException("Usuário ou senha do e-mail recusados pelo servidor.\n" + r2
                                + "\nNo Gmail use uma \"senha de app\" (Conta Google > Segurança > Senhas de app).");
                }
            }
            c.comando("MAIL FROM:<" + endereco(remetente) + ">", 250);
            for (String p : para) c.comando("RCPT TO:<" + endereco(p) + ">", 250, 251);
            c.comando("DATA", 354);
            c.escrever(montarMensagem(remetente, para, assunto, html, anexos));
            c.comando(".", 250);
            try { c.comandoResposta("QUIT"); } catch (IOException ignored) { }
        } finally {
            try { sock.close(); } catch (IOException ignored) { }
        }
    }

    private static SSLSocket tls(SSLSocket s) throws IOException {
        SSLParameters p = s.getSSLParameters();
        p.setEndpointIdentificationAlgorithm("HTTPS"); // confere o certificado do servidor
        s.setSSLParameters(p);
        s.startHandshake();
        return s;
    }

    private static String endereco(String s) {
        s = s.trim();
        int a = s.indexOf('<'), b = s.indexOf('>');
        return a >= 0 && b > a ? s.substring(a + 1, b).trim() : s;
    }

    private static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }

    private static String cabecalhoUtf8(String s) {
        return "=?UTF-8?B?" + b64(s.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    static String montarMensagem(String de, List<String> para, String assunto, String html, List<Anexo> anexos) {
        String misto = "misto_" + UUID.randomUUID().toString().replace("-", "");
        String rel = "rel_" + UUID.randomUUID().toString().replace("-", "");
        StringBuilder m = new StringBuilder();
        m.append("From: ").append(cabecalhoUtf8("Aviso de Água")).append(" <").append(endereco(de)).append(">\r\n");
        StringBuilder to = new StringBuilder();
        for (String p : para) { if (to.length() > 0) to.append(", "); to.append(endereco(p)); }
        m.append("To: ").append(to).append("\r\n");
        m.append("Subject: ").append(cabecalhoUtf8(assunto)).append("\r\n");
        m.append("Date: ").append(DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US).format(ZonedDateTime.now())).append("\r\n");
        m.append("Message-ID: <").append(UUID.randomUUID()).append("@avisodeagua>\r\n");
        m.append("MIME-Version: 1.0\r\n");
        m.append("Content-Type: multipart/mixed; boundary=\"").append(misto).append("\"\r\n\r\n");

        m.append("--").append(misto).append("\r\n");
        m.append("Content-Type: multipart/related; boundary=\"").append(rel).append("\"\r\n\r\n");
        m.append("--").append(rel).append("\r\n");
        m.append("Content-Type: text/html; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n");
        m.append(b64Linhas(html.getBytes(StandardCharsets.UTF_8)));
        List<Anexo> normais = new ArrayList<Anexo>();
        for (Anexo a : anexos) {
            if (a.contentId == null) { normais.add(a); continue; }
            m.append("--").append(rel).append("\r\n");
            m.append("Content-Type: ").append(a.tipo).append("; name=\"").append(a.nome).append("\"\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n");
            m.append("Content-ID: <").append(a.contentId).append(">\r\n");
            m.append("Content-Disposition: inline; filename=\"").append(a.nome).append("\"\r\n\r\n");
            m.append(b64Linhas(a.dados));
        }
        m.append("--").append(rel).append("--\r\n\r\n");
        for (Anexo a : normais) {
            m.append("--").append(misto).append("\r\n");
            m.append("Content-Type: ").append(a.tipo).append("; name=\"").append(a.nome).append("\"\r\n");
            m.append("Content-Transfer-Encoding: base64\r\n");
            m.append("Content-Disposition: attachment; filename=\"").append(a.nome).append("\"\r\n\r\n");
            m.append(b64Linhas(a.dados));
        }
        m.append("--").append(misto).append("--\r\n");
        return m.toString();
    }

    private static String b64Linhas(byte[] dados) {
        return Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(dados) + "\r\n";
    }

    /** Lê e escreve no servidor SMTP. */
    private static class Conexao {
        private final BufferedReader in;
        private final Writer out;

        Conexao(Socket s) throws IOException {
            in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII));
        }

        String ler() throws IOException {
            StringBuilder sb = new StringBuilder();
            String linha;
            while ((linha = in.readLine()) != null) {
                sb.append(linha).append('\n');
                if (linha.length() < 4 || linha.charAt(3) != '-') break;
            }
            if (sb.length() == 0) throw new IOException("O servidor de e-mail fechou a conexão.");
            return sb.toString().trim();
        }

        void esperar(int... codigos) throws IOException {
            String r = ler();
            for (int c : codigos) if (r.startsWith(String.valueOf(c))) return;
            throw new IOException("Resposta inesperada do servidor de e-mail:\n" + r);
        }

        String comandoResposta(String cmd) throws IOException {
            out.write(cmd + "\r\n");
            out.flush();
            return ler();
        }

        void comando(String cmd, int... codigos) throws IOException {
            String r = comandoResposta(cmd);
            for (int c : codigos) if (r.startsWith(String.valueOf(c))) return;
            String mostrado = cmd.startsWith("AUTH") || cmd.matches("^[A-Za-z0-9+/=]{8,}$") ? "(autenticação)" : cmd;
            throw new IOException("O servidor de e-mail recusou \"" + mostrado + "\":\n" + r);
        }

        void escrever(String mensagem) throws IOException {
            // "dot-stuffing": linhas que começam com ponto ganham outro ponto
            for (String linha : mensagem.split("\r\n", -1)) {
                if (linha.startsWith(".")) out.write(".");
                out.write(linha);
                out.write("\r\n");
            }
            out.flush();
        }
    }

    /** Separa uma lista "a@x.com; b@y.com, c@z.com" em endereços. */
    public static List<String> separar(String lista) {
        List<String> l = new ArrayList<String>();
        if (lista == null) return l;
        for (String p : lista.split("[;,\\s]+")) if (p.contains("@")) l.add(p.trim());
        return l;
    }
}
