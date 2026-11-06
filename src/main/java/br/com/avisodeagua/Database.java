package br.com.avisodeagua;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;

/**
 * Acesso ao MySQL. A conexão padrão é localhost:3306, usuário root, senha 100219.
 * Pode ser alterada em %APPDATA%\AvisodeAgua\config.properties.
 */
public class Database {

    public static final String ACAO_BEBEU = "BEBEU";
    public static final String ACAO_ADIOU = "ADIOU";
    public static final String ACAO_IGNOROU = "IGNOROU";
    /** Copo registrado pelo menu da bandeja, sem alerta. */
    public static final String ACAO_MANUAL = "MANUAL";

    private volatile String url;
    private volatile String usuario;
    private volatile String senha;
    private volatile boolean nuvem;

    /** Usa a configuração salva em config.properties (padrão: MySQL/MariaDB local, root / 100219). */
    public Database() {
        aplicar(carregarConfig());
    }

    /** Cria uma conexão com uma configuração específica (usado para testar e copiar dados). */
    public Database(Properties p) {
        aplicar(p);
    }

    /** Troca a conexão em uso (local ou nuvem) sem reiniciar o programa. */
    public final synchronized void aplicar(Properties p) {
        String host = p.getProperty("db.host", "localhost").trim();
        String porta = p.getProperty("db.porta", "3306").trim();
        String banco = p.getProperty("db.nome", "avisodeagua").trim();
        boolean ssl = "true".equalsIgnoreCase(p.getProperty("db.ssl", "false"));
        this.nuvem = "nuvem".equalsIgnoreCase(p.getProperty("db.tipo", "local"));
        this.usuario = p.getProperty("db.usuario", "root");
        this.senha = p.getProperty("db.senha", "100219");
        this.url = "jdbc:mysql://" + host + ":" + porta + "/" + banco
                + "?createDatabaseIfNotExist=true"
                + (ssl ? "&sslMode=REQUIRED" : "&sslMode=DISABLED")
                + "&allowPublicKeyRetrieval=true&connectTimeout=15000"
                + "&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8";
    }

    public boolean isNuvem() { return nuvem; }

    /** Pasta de dados do aplicativo (config e imagem personalizada). */
    public static Path pastaDados() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Paths.get(appData) : Paths.get(System.getProperty("user.home"));
        Path pasta = base.resolve("AvisodeAgua");
        try { Files.createDirectories(pasta); } catch (IOException ignored) { }
        return pasta;
    }

    /** Configuração padrão do banco local (MariaDB/MySQL instalado no computador). */
    public static Properties configLocal() {
        Properties p = new Properties();
        p.setProperty("db.tipo", "local");
        p.setProperty("db.host", "localhost");
        p.setProperty("db.porta", "3306");
        p.setProperty("db.nome", "avisodeagua");
        p.setProperty("db.usuario", "root");
        p.setProperty("db.senha", "100219");
        p.setProperty("db.ssl", "false");
        return p;
    }

    /** Lê %APPDATA%\AvisodeAgua\config.properties (cria com o padrão local se não existir). */
    public static synchronized Properties carregarConfig() {
        Properties p = new Properties();
        Path arq = pastaDados().resolve("config.properties");
        if (Files.exists(arq)) {
            try (InputStream in = Files.newInputStream(arq)) { p.load(in); } catch (IOException ignored) { }
        } else {
            p = configLocal();
            salvarConfig(p);
        }
        return p;
    }

    /** Grava as configurações (mantém as chaves que não forem do banco, ex.: celular.*). */
    public static synchronized void salvarConfig(Properties novas) {
        Path arq = pastaDados().resolve("config.properties");
        Properties p = new Properties();
        if (Files.exists(arq)) {
            try (InputStream in = Files.newInputStream(arq)) { p.load(in); } catch (IOException ignored) { }
        }
        p.putAll(novas);
        try (OutputStream out = Files.newOutputStream(arq)) {
            p.store(out, "Configuracao - Aviso de Agua");
        } catch (IOException e) {
            System.err.println("Não foi possível gravar config.properties: " + e.getMessage());
        }
    }

    /** Testa a conexão; devolve a versão do servidor. */
    public String testar() throws SQLException {
        try (Connection c = conectar(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {
            return rs.next() ? rs.getString(1) : "?";
        }
    }

    Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** Cria as tabelas se ainda não existirem. */
    public void inicializar() throws SQLException {
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS usuario ( " + "id INT PRIMARY KEY AUTO_INCREMENT, " + "nome VARCHAR(100) NOT NULL, " + "celular VARCHAR(20) NOT NULL, " + "intervalo_minutos INT NOT NULL DEFAULT 60, " + "hora_inicio TIME NOT NULL DEFAULT '08:00:00', " + "hora_fim TIME NOT NULL DEFAULT '22:00:00', " + "meta_copos INT NOT NULL DEFAULT 8, " + "ativo BOOLEAN NOT NULL DEFAULT TRUE, " + "iniciar_windows BOOLEAN NOT NULL DEFAULT TRUE, " + "criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + "atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP " + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS registro_consumo ( " + "id INT PRIMARY KEY AUTO_INCREMENT, " + "usuario_id INT NOT NULL, " + "data_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " + "acao VARCHAR(10) NOT NULL, " + "INDEX idx_usuario_data (usuario_id, data_hora), " + "FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE " + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ");
            // colunas novas (versão 1.1): peso, tamanho do copo e volume de cada registro
            adicionarColuna(c, "usuario", "peso_kg", "DECIMAL(5,1) NULL");
            adicionarColuna(c, "usuario", "copo_ml", "INT NOT NULL DEFAULT 250");
            adicionarColuna(c, "registro_consumo", "quantidade_ml", "INT NULL");
            // versão 1.2: perfil corporal, medidas, fotos, alimentação e e-mail
            adicionarColuna(c, "usuario", "email", "VARCHAR(150) NULL");
            adicionarColuna(c, "usuario", "sexo", "CHAR(1) NOT NULL DEFAULT 'M'");
            adicionarColuna(c, "usuario", "data_nascimento", "DATE NULL");
            adicionarColuna(c, "usuario", "altura_cm", "DECIMAL(5,1) NULL");
            adicionarColuna(c, "usuario", "nivel_atividade", "INT NOT NULL DEFAULT 1");
            adicionarColuna(c, "usuario", "peso_meta_kg", "DECIMAL(5,1) NULL");
            adicionarColuna(c, "usuario", "meta_kcal", "INT NOT NULL DEFAULT 0");
            adicionarColuna(c, "usuario", "lembrete_medidas_dias", "INT NOT NULL DEFAULT 15");
            StringBuilder med = new StringBuilder("CREATE TABLE IF NOT EXISTS medida_corpo ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, usuario_id INT NOT NULL, data DATE NOT NULL, "
                    + "peso_kg DECIMAL(5,1) NULL, ");
            for (String col : Medida.COLUNAS) med.append(col).append(" DECIMAL(5,1) NULL, ");
            med.append("observacao VARCHAR(500) NULL, criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX idx_medida_usuario (usuario_id, data), "
                    + "FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate(med.toString());
            st.executeUpdate("CREATE TABLE IF NOT EXISTS foto_corpo ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, medida_id INT NOT NULL, "
                    + "tipo VARCHAR(10) NOT NULL, imagem MEDIUMBLOB NOT NULL, "
                    + "criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (medida_id) REFERENCES medida_corpo(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS refeicao_item ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, usuario_id INT NOT NULL, data DATE NOT NULL, "
                    + "refeicao VARCHAR(30) NOT NULL, descricao VARCHAR(150) NOT NULL, "
                    + "porcao VARCHAR(60) NULL, quantidade DECIMAL(6,2) NOT NULL DEFAULT 1, kcal INT NOT NULL DEFAULT 0, "
                    + "criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX idx_refeicao_usuario (usuario_id, data), "
                    + "FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS config_email ("
                    + "id INT PRIMARY KEY, servidor VARCHAR(120), porta INT, seguranca VARCHAR(10), "
                    + "usuario VARCHAR(150), senha VARCHAR(200), remetente VARCHAR(150), "
                    + "destinatarios VARCHAR(500), enviar_fotos BOOLEAN NOT NULL DEFAULT TRUE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private static void adicionarColuna(Connection c, String tabela, String coluna, String ddl) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, tabela);
            ps.setString(2, coluna);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }
        try (Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + ddl);
        }
    }

    /** Retorna o usuário cadastrado (o primeiro) ou um usuário vazio. */
    public Usuario carregarUsuario() throws SQLException {
        Usuario u = new Usuario();
        try (Connection c = conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM usuario ORDER BY id LIMIT 1")) {
            if (rs.next()) {
                u.setId(rs.getInt("id"));
                u.setNome(rs.getString("nome"));
                u.setCelular(rs.getString("celular"));
                u.setIntervaloMinutos(rs.getInt("intervalo_minutos"));
                u.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
                u.setHoraFim(rs.getTime("hora_fim").toLocalTime());
                u.setMetaCopos(rs.getInt("meta_copos"));
                u.setAtivo(rs.getBoolean("ativo"));
                u.setIniciarComWindows(rs.getBoolean("iniciar_windows"));
                double peso = rs.getDouble("peso_kg");
                u.setPesoKg(rs.wasNull() ? 0 : peso);
                u.setCopoMl(Math.max(50, rs.getInt("copo_ml")));
                u.setEmail(rs.getString("email"));
                u.setSexo(rs.getString("sexo"));
                Date nasc = rs.getDate("data_nascimento");
                u.setDataNascimento(nasc != null ? nasc.toLocalDate() : null);
                u.setAlturaCm(rs.getDouble("altura_cm"));
                u.setNivelAtividade(rs.getInt("nivel_atividade"));
                u.setPesoMetaKg(rs.getDouble("peso_meta_kg"));
                u.setMetaKcal(rs.getInt("meta_kcal"));
                u.setLembreteMedidasDias(rs.getInt("lembrete_medidas_dias"));
            } else {
                u.setId(0);
            }
        }
        return u;
    }

    public void salvarUsuario(Usuario u) throws SQLException {
        try (Connection c = conectar()) {
            if (u.getId() > 0 && existe(c, u.getId())) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET nome=?, celular=?, intervalo_minutos=?, hora_inicio=?, hora_fim=?, " + "meta_copos=?, ativo=?, iniciar_windows=?, peso_kg=?, copo_ml=?, email=?, sexo=?, data_nascimento=?, "
                        + "altura_cm=?, nivel_atividade=?, peso_meta_kg=?, meta_kcal=?, lembrete_medidas_dias=? WHERE id=? ")) {
                    preencher(ps, u);
                    ps.setInt(19, u.getId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO usuario (nome, celular, intervalo_minutos, hora_inicio, hora_fim, " + "meta_copos, ativo, iniciar_windows, peso_kg, copo_ml, email, sexo, data_nascimento, altura_cm, "
                        + "nivel_atividade, peso_meta_kg, meta_kcal, lembrete_medidas_dias) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ",
                        Statement.RETURN_GENERATED_KEYS)) {
                    preencher(ps, u);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        if (k.next()) u.setId(k.getInt(1));
                    }
                }
            }
        }
    }

    private boolean existe(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM usuario WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void preencher(PreparedStatement ps, Usuario u) throws SQLException {
        ps.setString(1, u.getNome());
        ps.setString(2, u.getCelular());
        ps.setInt(3, u.getIntervaloMinutos());
        ps.setTime(4, Time.valueOf(u.getHoraInicio()));
        ps.setTime(5, Time.valueOf(u.getHoraFim()));
        ps.setInt(6, u.getMetaCopos());
        ps.setBoolean(7, u.isAtivo());
        ps.setBoolean(8, u.isIniciarComWindows());
        if (u.getPesoKg() > 0) ps.setDouble(9, u.getPesoKg()); else ps.setNull(9, Types.DECIMAL);
        ps.setInt(10, u.getCopoMl());
        ps.setString(11, u.getEmail());
        ps.setString(12, u.getSexo());
        if (u.getDataNascimento() != null) ps.setDate(13, Date.valueOf(u.getDataNascimento()));
        else ps.setNull(13, Types.DATE);
        if (u.getAlturaCm() > 0) ps.setDouble(14, u.getAlturaCm()); else ps.setNull(14, Types.DECIMAL);
        ps.setInt(15, u.getNivelAtividade());
        if (u.getPesoMetaKg() > 0) ps.setDouble(16, u.getPesoMetaKg()); else ps.setNull(16, Types.DECIMAL);
        ps.setInt(17, u.getMetaKcal());
        ps.setInt(18, u.getLembreteMedidasDias());
    }

    /** Registra a resposta ao alerta. ml = quantidade bebida (0 para ADIOU / IGNOROU). */
    public void registrar(int usuarioId, String acao, int ml) {
        registrar(usuarioId, acao, ml, null);
    }

    /** Igual ao anterior, com data/hora informada (ex.: copos registrados no celular sem internet). */
    public void registrar(int usuarioId, String acao, int ml, java.time.LocalDateTime quando) {
        if (usuarioId <= 0) return;
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO registro_consumo (usuario_id, acao, quantidade_ml, data_hora) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, usuarioId);
            ps.setString(2, acao);
            if (ml > 0) ps.setInt(3, ml); else ps.setNull(3, Types.INTEGER);
            ps.setTimestamp(4, Timestamp.valueOf(quando != null ? quando : java.time.LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Falha ao registrar consumo: " + e.getMessage());
        }
    }

    /** Quantos copos o usuário bebeu hoje. */
    public int coposHoje(int usuarioId) {
        if (usuarioId <= 0) return 0;
        try (Connection c = conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM registro_consumo WHERE usuario_id=? AND acao IN (?, 'MANUAL') AND DATE(data_hora)=?")) {
            ps.setInt(1, usuarioId);
            ps.setString(2, ACAO_BEBEU);
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ relatórios

    /** Consumo de um dia, usado nos relatórios e gráficos. */
    public static class DiaConsumo {
        public final LocalDate dia;
        public int copos, ml, adiados, ignorados, manuais;
        DiaConsumo(LocalDate dia) { this.dia = dia; }
    }

    /** ml bebidos hoje. */
    public int mlHoje(Usuario u) {
        java.util.List<DiaConsumo> l = consumoPorDia(u, LocalDate.now(), LocalDate.now());
        return l.isEmpty() ? 0 : l.get(0).ml;
    }

    /** Consumo dia a dia entre as datas (inclusive), com todos os dias preenchidos (mesmo sem registro). */
    public java.util.List<DiaConsumo> consumoPorDia(Usuario u, LocalDate de, LocalDate ate) {
        java.util.Map<LocalDate, DiaConsumo> mapa = new java.util.LinkedHashMap<LocalDate, DiaConsumo>();
        for (LocalDate d = de; !d.isAfter(ate); d = d.plusDays(1)) mapa.put(d, new DiaConsumo(d));
        if (u.getId() <= 0) return new java.util.ArrayList<DiaConsumo>(mapa.values());
        String sql = "SELECT DATE(data_hora) dia, "
                + "SUM(acao IN ('BEBEU','MANUAL')) copos, SUM(acao = 'MANUAL') manuais, "
                + "SUM(CASE WHEN acao IN ('BEBEU','MANUAL') THEN COALESCE(quantidade_ml, ?) ELSE 0 END) ml, "
                + "SUM(acao = 'ADIOU') adiados, SUM(acao = 'IGNOROU') ignorados "
                + "FROM registro_consumo WHERE usuario_id = ? AND data_hora >= ? AND data_hora < ? "
                + "GROUP BY DATE(data_hora)";
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, u.getCopoMl());
            ps.setInt(2, u.getId());
            ps.setTimestamp(3, Timestamp.valueOf(de.atStartOfDay()));
            ps.setTimestamp(4, Timestamp.valueOf(ate.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DiaConsumo dc = mapa.get(rs.getDate("dia").toLocalDate());
                    if (dc == null) continue;
                    dc.copos = rs.getInt("copos");
                    dc.manuais = rs.getInt("manuais");
                    dc.ml = rs.getInt("ml");
                    dc.adiados = rs.getInt("adiados");
                    dc.ignorados = rs.getInt("ignorados");
                }
            }
        } catch (SQLException e) {
            System.err.println("Falha no relatório diário: " + e.getMessage());
        }
        return new java.util.ArrayList<DiaConsumo>(mapa.values());
    }

    /** Copos bebidos em cada hora do dia (0 a 23) no período. */
    public int[] coposPorHora(Usuario u, LocalDate de, LocalDate ate) {
        int[] horas = new int[24];
        if (u.getId() <= 0) return horas;
        String sql = "SELECT HOUR(data_hora) h, COUNT(*) n FROM registro_consumo "
                + "WHERE usuario_id = ? AND acao IN ('BEBEU','MANUAL') AND data_hora >= ? AND data_hora < ? GROUP BY HOUR(data_hora)";
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, u.getId());
            ps.setTimestamp(2, Timestamp.valueOf(de.atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(ate.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) horas[rs.getInt("h")] = rs.getInt("n");
            }
        } catch (SQLException e) {
            System.err.println("Falha no relatório por hora: " + e.getMessage());
        }
        return horas;
    }

    public String descricaoConexao() {
        return url.substring(0, url.indexOf('?')).replace("jdbc:mysql://", "") + " (usuário " + usuario + ")"
                + (nuvem ? " - nuvem" : " - local");
    }
}
