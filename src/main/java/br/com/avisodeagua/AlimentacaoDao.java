package br.com.avisodeagua;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gravação do que foi comido no dia (valores aproximados). */
public class AlimentacaoDao {

    public static final String[] REFEICOES = {
            "Café da manhã", "Lanche da manhã", "Almoço", "Lanche da tarde", "Jantar", "Ceia"};

    public static class Item {
        public int id;
        public LocalDate data;
        public String refeicao;
        public String descricao;
        public String porcao;
        public double quantidade = 1;
        public int kcal;
    }

    private final Database db;

    public AlimentacaoDao(Database db) { this.db = db; }

    public List<Item> itens(int usuarioId, LocalDate dia) throws SQLException {
        List<Item> l = new ArrayList<Item>();
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM refeicao_item WHERE usuario_id=? AND data=? ORDER BY id")) {
            ps.setInt(1, usuarioId);
            ps.setDate(2, Date.valueOf(dia));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Item it = new Item();
                    it.id = rs.getInt("id");
                    it.data = rs.getDate("data").toLocalDate();
                    it.refeicao = rs.getString("refeicao");
                    it.descricao = rs.getString("descricao");
                    it.porcao = rs.getString("porcao");
                    it.quantidade = rs.getDouble("quantidade");
                    it.kcal = rs.getInt("kcal");
                    l.add(it);
                }
            }
        }
        // ordena pela ordem das refeições do dia
        List<Item> ordenada = new ArrayList<Item>();
        for (String r : REFEICOES) for (Item it : l) if (r.equals(it.refeicao)) ordenada.add(it);
        for (Item it : l) if (!ordenada.contains(it)) ordenada.add(it);
        return ordenada;
    }

    public void adicionar(int usuarioId, Item it) throws SQLException {
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO refeicao_item (usuario_id, data, refeicao, descricao, porcao, quantidade, kcal) "
                             + "VALUES (?,?,?,?,?,?,?)")) {
            ps.setInt(1, usuarioId);
            ps.setDate(2, Date.valueOf(it.data));
            ps.setString(3, it.refeicao);
            ps.setString(4, it.descricao);
            ps.setString(5, it.porcao);
            ps.setDouble(6, it.quantidade);
            ps.setInt(7, it.kcal);
            ps.executeUpdate();
        }
    }

    public void remover(int usuarioId, int id) throws SQLException {
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM refeicao_item WHERE id=? AND usuario_id=?")) {
            ps.setInt(1, id);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        }
    }

    /** Copia todos os itens de um dia para outro (ex.: repetir o cardápio de ontem). */
    public int copiarDia(int usuarioId, LocalDate de, LocalDate para) throws SQLException {
        int n = 0;
        for (Item it : itens(usuarioId, de)) {
            it.data = para;
            adicionar(usuarioId, it);
            n++;
        }
        return n;
    }

    /** kcal por dia no período (todos os dias preenchidos, 0 quando não houve registro). */
    public Map<LocalDate, Integer> kcalPorDia(int usuarioId, LocalDate de, LocalDate ate) {
        Map<LocalDate, Integer> m = new LinkedHashMap<LocalDate, Integer>();
        for (LocalDate d = de; !d.isAfter(ate); d = d.plusDays(1)) m.put(d, 0);
        try (Connection c = db.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT data, SUM(kcal) FROM refeicao_item WHERE usuario_id=? AND data BETWEEN ? AND ? GROUP BY data")) {
            ps.setInt(1, usuarioId);
            ps.setDate(2, Date.valueOf(de));
            ps.setDate(3, Date.valueOf(ate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(rs.getDate(1).toLocalDate(), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("Falha no resumo de calorias: " + e.getMessage());
        }
        return m;
    }
}
