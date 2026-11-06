package br.com.avisodeagua;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Monta o e-mail de acompanhamento (HTML + desenho do corpo + régua + fotos). */
public final class RelatorioEmail {

    private static final DateTimeFormatter DM = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static class Conteudo {
        public String assunto;
        public String html;
        public List<Email.Anexo> anexos = new ArrayList<Email.Anexo>();
    }

    private RelatorioEmail() { }

    public static Conteudo montar(Database db, Usuario u, List<Medida> medidas, boolean comFotos) throws Exception {
        Conteudo c = new Conteudo();
        Medida atual = medidas.isEmpty() ? null : medidas.get(medidas.size() - 1);
        Medida anterior = medidas.size() >= 2 ? medidas.get(medidas.size() - 2) : null;
        Medida primeira = medidas.size() >= 2 ? medidas.get(0) : null;
        c.assunto = "Acompanhamento - " + u.getNome() + " - " + LocalDate.now().format(DM);

        StringBuilder h = new StringBuilder();
        h.append("<div style=\"font-family:Segoe UI,Arial,sans-serif;color:#33414E;max-width:720px\">");
        h.append("<div style=\"background:#1E90DC;color:#fff;padding:16px 20px;border-radius:8px 8px 0 0\">")
                .append("<div style=\"font-size:22px;font-weight:bold\">Relatório de acompanhamento</div>")
                .append("<div style=\"font-size:13px\">").append(esc(u.getNome()));
        if (!u.getCelular().isEmpty()) h.append(" &bull; ").append(esc(u.getCelular()));
        h.append(" &bull; gerado em ").append(LocalDate.now().format(DM)).append("</div></div>");
        h.append("<div style=\"border:1px solid #DCE6EE;border-top:0;padding:16px 20px;border-radius:0 0 8px 8px\">");

        // perfil
        h.append("<p style=\"margin:0 0 12px\">");
        if (u.getIdade() > 0) h.append("Idade: <b>").append(u.getIdade()).append(" anos</b> &nbsp; ");
        if (u.getAlturaCm() > 0) h.append("Altura: <b>").append(Medida.fmt(u.getAlturaCm())).append(" cm</b> &nbsp; ");
        h.append("Sexo: <b>").append(u.isFeminino() ? "feminino" : "masculino").append("</b>");
        if (u.getPesoMetaKg() > 0) h.append(" &nbsp; Peso meta: <b>").append(Medida.fmt(u.getPesoMetaKg())).append(" kg</b>");
        h.append("</p>");

        if (atual == null) {
            h.append("<p>Ainda não há medições cadastradas.</p>");
        } else {
            String resumo = ReguaProgresso.resumoPeso(u, medidas);
            if (!resumo.isEmpty())
                h.append("<p style=\"background:#EAF5FC;padding:10px 12px;border-radius:6px\"><b>Progresso:</b> ")
                        .append(esc(resumo)).append("</p>");

            // indicadores
            h.append("<table cellpadding=\"6\" style=\"border-collapse:collapse;margin:8px 0 14px\"><tr>");
            indicador(h, "Peso", atual.getPeso() > 0 ? Medida.fmt(atual.getPeso()) + " kg" : "—");
            double imc = atual.imc(u.getAlturaCm());
            indicador(h, "IMC", imc > 0 ? Medida.fmt(imc) + " (" + Medida.classificarImc(imc) + ")" : "—");
            double gord = atual.gorduraEstimada(u);
            indicador(h, "Gordura estimada", gord > 0 ? Medida.fmt(gord) + "%" : "—");
            double rcq = atual.rcq();
            indicador(h, "Cintura/Quadril", rcq > 0 ? String.format("%.2f", rcq).replace('.', ',') : "—");
            h.append("</tr></table>");

            // tabela de medidas
            h.append("<h3 style=\"margin:16px 0 6px;color:#0B4F75\">Medidas (cm)</h3>");
            h.append("<table cellpadding=\"6\" style=\"border-collapse:collapse;width:100%;font-size:13px\">");
            h.append("<tr style=\"background:#F2F8FC\"><th align=\"left\">Medida</th>");
            if (primeira != null) h.append("<th>Início<br><small>").append(primeira.getData().format(DM)).append("</small></th>");
            if (anterior != null && anterior != primeira) h.append("<th>Anterior<br><small>").append(anterior.getData().format(DM)).append("</small></th>");
            h.append("<th>Atual<br><small>").append(atual.getData().format(DM)).append("</small></th>");
            if (primeira != null) h.append("<th>Variação total</th>");
            h.append("</tr>");
            linha(h, "Peso (kg)", primeira, anterior, atual, -1);
            for (int i = 0; i < Medida.COLUNAS.length; i++) linha(h, Medida.NOMES[i], primeira, anterior, atual, i);
            h.append("</table>");
            if (!atual.getObservacao().isEmpty())
                h.append("<p><b>Observação:</b> ").append(esc(atual.getObservacao())).append("</p>");

            // imagens
            Medida comp = primeira != null ? primeira : null;
            BufferedImage corpo = DesenhoCorpo.imagem(u, atual, comp,
                    comp != null ? "primeira medição (" + comp.getData().format(DM) + ")" : "", 640, 740);
            c.anexos.add(new Email.Anexo("desenho_corpo.png", "image/png", png(corpo), "corpo"));
            h.append("<h3 style=\"margin:18px 0 6px;color:#0B4F75\">Desenho aproximado do corpo</h3>");
            h.append("<img src=\"cid:corpo\" width=\"640\" style=\"max-width:100%;border:1px solid #DCE6EE\">");
            if (medidas.size() >= 1) {
                BufferedImage regua = ReguaProgresso.imagem(u, medidas, 720, 760);
                c.anexos.add(new Email.Anexo("regua_progresso.png", "image/png", png(regua), "regua"));
                h.append("<h3 style=\"margin:18px 0 6px;color:#0B4F75\">Régua de progresso</h3>");
                h.append("<img src=\"cid:regua\" width=\"720\" style=\"max-width:100%;border:1px solid #DCE6EE\">");
            }
            if (comFotos) {
                CorpoDao dao = new CorpoDao(db);
                int n = 0;
                for (CorpoDao.Foto f : dao.fotos(atual.getId())) {
                    n++;
                    c.anexos.add(new Email.Anexo("foto_" + f.tipo.toLowerCase() + "_" + atual.getData() + "_" + n + ".jpg",
                            "image/jpeg", f.imagem, null));
                }
                if (n > 0) h.append("<p>").append(n).append(" foto(s) da última medição em anexo.</p>");
            }
        }

        // água e alimentação dos últimos 7 dias
        LocalDate ate = LocalDate.now(), de = ate.minusDays(6);
        List<Database.DiaConsumo> agua = db.consumoPorDia(u, de, ate);
        Map<LocalDate, Integer> kcal = new AlimentacaoDao(db).kcalPorDia(u.getId(), de, ate);
        int metaKcal = u.getMetaKcalEfetiva(u.pesoReferencia(atual));
        h.append("<h3 style=\"margin:18px 0 6px;color:#0B4F75\">Últimos 7 dias</h3>");
        h.append("<table cellpadding=\"6\" style=\"border-collapse:collapse;width:100%;font-size:13px\">");
        h.append("<tr style=\"background:#F2F8FC\"><th align=\"left\">Dia</th><th>Água</th><th>Meta de água</th><th>Calorias (aprox.)</th>");
        if (metaKcal > 0) h.append("<th>Meta kcal</th>");
        h.append("</tr>");
        int totalMl = 0, totalKcal = 0, diasKcal = 0;
        for (Database.DiaConsumo d : agua) {
            int k = kcal.containsKey(d.dia) ? kcal.get(d.dia) : 0;
            totalMl += d.ml;
            if (k > 0) { totalKcal += k; diasKcal++; }
            boolean ok = u.getMetaMl() > 0 && d.ml >= u.getMetaMl();
            h.append("<tr style=\"border-top:1px solid #E4EAF0\"><td>").append(d.dia.format(DM)).append("</td>")
                    .append("<td align=\"center\">").append(Usuario.formatarMl(d.ml)).append("</td>")
                    .append("<td align=\"center\" style=\"color:").append(ok ? "#2E9E5B" : "#B86E00").append("\">")
                    .append(ok ? "atingida" : "não atingida").append("</td>")
                    .append("<td align=\"center\">").append(k > 0 ? k + " kcal" : "—").append("</td>");
            if (metaKcal > 0) h.append("<td align=\"center\">").append(metaKcal).append("</td>");
            h.append("</tr>");
        }
        h.append("</table>");
        h.append("<p style=\"font-size:13px\">Média de água: <b>").append(Usuario.formatarMl(totalMl / 7))
                .append("</b> por dia (meta ").append(Usuario.formatarMl(u.getMetaMl())).append(")");
        if (diasKcal > 0) h.append(" &bull; Média de calorias nos dias registrados: <b>").append(totalKcal / diasKcal).append(" kcal</b>");
        h.append("</p>");

        if (u.getLembreteMedidasDias() > 0 && atual != null)
            h.append("<p style=\"font-size:13px\">Próxima atualização das medidas sugerida para <b>")
                    .append(atual.getData().plusDays(u.getLembreteMedidasDias()).format(DM)).append("</b>.</p>");
        h.append("<p style=\"font-size:11px;color:#8899AA;margin-top:18px\">Valores aproximados, apenas para acompanhamento pessoal. "
                + "Calorias e % de gordura são estimativas; não substituem a orientação de um nutricionista ou médico. "
                + "Enviado pelo programa Aviso de Água.</p>");
        h.append("</div></div>");
        c.html = h.toString();
        return c;
    }

    private static void indicador(StringBuilder h, String nome, String valor) {
        h.append("<td style=\"border:1px solid #DCE6EE;padding:8px 12px\"><div style=\"font-size:11px;color:#667788\">")
                .append(nome).append("</div><div style=\"font-size:16px;font-weight:bold;color:#0B4F75\">")
                .append(esc(valor)).append("</div></td>");
    }

    private static void linha(StringBuilder h, String nome, Medida pri, Medida ant, Medida at, int i) {
        double va = i < 0 ? at.getPeso() : at.get(i);
        double vp = pri == null ? 0 : (i < 0 ? pri.getPeso() : pri.get(i));
        double vn = ant == null ? 0 : (i < 0 ? ant.getPeso() : ant.get(i));
        if (va <= 0 && vp <= 0 && vn <= 0) return;
        h.append("<tr style=\"border-top:1px solid #E4EAF0\"><td>").append(nome).append("</td>");
        if (pri != null) h.append("<td align=\"center\">").append(vp > 0 ? Medida.fmt(vp) : "—").append("</td>");
        if (ant != null && ant != pri) h.append("<td align=\"center\">").append(vn > 0 ? Medida.fmt(vn) : "—").append("</td>");
        h.append("<td align=\"center\"><b>").append(va > 0 ? Medida.fmt(va) : "—").append("</b></td>");
        if (pri != null) {
            if (va > 0 && vp > 0) {
                double d = va - vp;
                String cor = Math.abs(d) < 0.05 ? "#667788" : d < 0 ? "#2E9E5B" : "#D9822B";
                h.append("<td align=\"center\" style=\"color:").append(cor).append(";font-weight:bold\">")
                        .append(d > 0 ? "+" : "").append(Medida.fmt(d)).append("</td>");
            } else h.append("<td align=\"center\">—</td>");
        }
        h.append("</tr>");
    }

    static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
