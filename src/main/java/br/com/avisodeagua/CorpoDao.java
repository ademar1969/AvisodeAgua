package br.com.avisodeagua;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Gravação das medições do corpo e das fotos no MySQL. */
public class CorpoDao {

    public static final String[] TIPOS_FOTO = {"FRENTE", "LADO", "COSTAS", "OUTRA"};
    public static final String[] NOMES_FOTO = {"Frente", "Lado", "Costas", "Outra"};

    public static class Foto {
        public int id;
        public int medidaId;
        public String tipo;
        public byte[] imagem;
        public LocalDate data;
    }

    private final Database db;

    public CorpoDao(Database db) { this.db = db; }

    /** Todas as medições do usuário, da mais antiga para a mais recente. */
    public List<Medida> listar(int usuarioId) throws SQLException {
        List<Medida> lista = new ArrayList<Medida>();
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM medida_corpo WHERE usuario_id = ? ORDER BY data, id")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Medida m = new Medida();
                    m.setId(rs.getInt("id"));
                    m.setData(rs.getDate("data").toLocalDate());
                    m.setPeso(rs.getDouble("peso_kg"));
                    for (int i = 0; i < Medida.COLUNAS.length; i++) m.set(i, rs.getDouble(Medida.COLUNAS[i]));
                    m.setObservacao(rs.getString("observacao"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    public void salvar(int usuarioId, Medida m) throws SQLException {
        StringBuilder cols = new StringBuilder("data, peso_kg");
        for (String col : Medida.COLUNAS) cols.append(", ").append(col);
        cols.append(", observacao");
        try (Connection c = db.conectar()) {
            if (m.getId() > 0) {
                StringBuilder sql = new StringBuilder("UPDATE medida_corpo SET data=?, peso_kg=?");
                for (String col : Medida.COLUNAS) sql.append(", ").append(col).append("=?");
                sql.append(", observacao=? WHERE id=? AND usuario_id=?");
                try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                    int n = preencher(ps, m);
                    ps.setInt(n++, m.getId());
                    ps.setInt(n, usuarioId);
                    ps.executeUpdate();
                }
            } else {
                StringBuilder q = new StringBuilder("?, ?");
                for (int i = 0; i < Medida.COLUNAS.length; i++) q.append(", ?");
                q.append(", ?, ?");
                String sql = "INSERT INTO medida_corpo (" + cols + ", usuario_id) VALUES (" + q + ")";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    int n = preencher(ps, m);
                    ps.setInt(n, usuarioId);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        if (k.next()) m.setId(k.getInt(1));
                    }
                }
            }
        }
    }

    private static int preencher(PreparedStatement ps, Medida m) throws SQLException {
        int n = 1;
        ps.setDate(n++, Date.valueOf(m.getData()));
        decimal(ps, n++, m.getPeso());
        for (int i = 0; i < Medida.COLUNAS.length; i++) decimal(ps, n++, m.get(i));
        ps.setString(n++, m.getObservacao());
        return n;
    }

    private static void decimal(PreparedStatement ps, int i, double v) throws SQLException {
        if (v > 0) ps.setDouble(i, v); else ps.setNull(i, Types.DECIMAL);
    }

    public void excluir(int usuarioId, int medidaId) throws SQLException {
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM medida_corpo WHERE id=? AND usuario_id=?")) {
            ps.setInt(1, medidaId);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        }
    }

    /** Data da medição mais recente, ou null. */
    public LocalDate ultimaData(int usuarioId) {
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT MAX(data) FROM medida_corpo WHERE usuario_id=?")) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getDate(1) != null) return rs.getDate(1).toLocalDate();
            }
        } catch (SQLException e) {
            System.err.println("Falha ao consultar medições: " + e.getMessage());
        }
        return null;
    }

    // ------------------------------------------------------------------ fotos

    public List<Foto> fotos(int medidaId) throws SQLException {
        List<Foto> l = new ArrayList<Foto>();
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT f.id, f.medida_id, f.tipo, f.imagem, m.data FROM foto_corpo f "
                             + "JOIN medida_corpo m ON m.id = f.medida_id WHERE f.medida_id=? ORDER BY f.id")) {
            ps.setInt(1, medidaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Foto f = new Foto();
                    f.id = rs.getInt(1);
                    f.medidaId = rs.getInt(2);
                    f.tipo = rs.getString(3);
                    f.imagem = rs.getBytes(4);
                    f.data = rs.getDate(5).toLocalDate();
                    l.add(f);
                }
            }
        }
        return l;
    }

    /** Primeira foto do tipo numa medição (para comparar antes / depois), ou null. */
    public Foto foto(int medidaId, String tipo) throws SQLException {
        for (Foto f : fotos(medidaId)) if (f.tipo.equals(tipo)) return f;
        return null;
    }

    public void adicionarFoto(int medidaId, String tipo, byte[] jpeg) throws SQLException {
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO foto_corpo (medida_id, tipo, imagem) VALUES (?, ?, ?)")) {
            ps.setInt(1, medidaId);
            ps.setString(2, tipo);
            ps.setBytes(3, jpeg);
            ps.executeUpdate();
        }
    }

    public void removerFoto(int fotoId) throws SQLException {
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM foto_corpo WHERE id=?")) {
            ps.setInt(1, fotoId);
            ps.executeUpdate();
        }
    }

    public static String nomeTipo(String tipo) {
        for (int i = 0; i < TIPOS_FOTO.length; i++) if (TIPOS_FOTO[i].equals(tipo)) return NOMES_FOTO[i];
        return tipo;
    }
}
