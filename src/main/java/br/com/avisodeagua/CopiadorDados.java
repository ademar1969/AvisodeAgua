package br.com.avisodeagua;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Copia todos os dados de um banco para outro (ex.: do banco local para o banco em nuvem). */
public final class CopiadorDados {

    /** Ordem respeita as chaves estrangeiras. */
    private static final String[] TABELAS = {
            "usuario", "registro_consumo", "medida_corpo", "foto_corpo", "refeicao_item", "config_email"};

    private CopiadorDados() { }

    /** true se o banco de destino ainda não tem usuário cadastrado. */
    public static boolean destinoVazio(Database destino) throws SQLException {
        destino.inicializar();
        try (Connection c = destino.conectar(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuario")) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    /** Copia as tabelas; devolve um resumo do que foi copiado. */
    public static String copiar(Database origem, Database destino) throws SQLException {
        origem.inicializar();
        destino.inicializar();
        StringBuilder resumo = new StringBuilder();
        try (Connection co = origem.conectar(); Connection cd = destino.conectar()) {
            cd.setAutoCommit(false);
            try {
                for (String t : TABELAS) {
                    int n = copiarTabela(co, cd, t);
                    resumo.append(t).append(": ").append(n).append(" registro(s)\n");
                }
                cd.commit();
            } catch (SQLException e) {
                cd.rollback();
                throw e;
            }
        }
        return resumo.toString();
    }

    private static int copiarTabela(Connection co, Connection cd, String tabela) throws SQLException {
        Set<String> colsDestino = new HashSet<String>();
        try (Statement st = cd.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + tabela + " WHERE 1=0")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) colsDestino.add(md.getColumnName(i).toLowerCase());
        }
        int n = 0;
        try (Statement st = co.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + tabela)) {
            ResultSetMetaData md = rs.getMetaData();
            List<Integer> idx = new ArrayList<Integer>();
            StringBuilder cols = new StringBuilder(), qs = new StringBuilder();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String nome = md.getColumnName(i).toLowerCase();
                if (!colsDestino.contains(nome)) continue;
                idx.add(i);
                if (cols.length() > 0) { cols.append(", "); qs.append(", "); }
                cols.append(nome);
                qs.append("?");
            }
            String sql = "REPLACE INTO " + tabela + " (" + cols + ") VALUES (" + qs + ")";
            try (PreparedStatement ps = cd.prepareStatement(sql)) {
                while (rs.next()) {
                    for (int k = 0; k < idx.size(); k++) ps.setObject(k + 1, rs.getObject(idx.get(k)));
                    ps.addBatch();
                    if (++n % 200 == 0) ps.executeBatch();
                }
                ps.executeBatch();
            }
        }
        return n;
    }
}
