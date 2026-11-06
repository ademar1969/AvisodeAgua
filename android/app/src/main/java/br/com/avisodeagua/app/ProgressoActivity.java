package br.com.avisodeagua.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Progresso: resumo do peso, desenho do corpo, régua de progresso e água da semana. */
public class ProgressoActivity extends Activity {

    private TextView lblResumo;
    private ImageView imgCorpo, imgRegua;
    private BarrasView barras;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout p = Ui.pagina(this);

        LinearLayout r = Ui.cartao(this);
        r.addView(Ui.texto(this, "Resumo", 16, Ui.TEXTO, true));
        lblResumo = Ui.texto(this, "Carregando...", 14, Ui.TEXTO, false);
        r.addView(lblResumo, Ui.largura(this, 6));
        p.addView(r);

        LinearLayout a = Ui.cartao(this);
        a.addView(Ui.texto(this, "💧 Água nos últimos 7 dias", 16, Ui.TEXTO, true));
        barras = new BarrasView(this);
        a.addView(barras, Ui.largura(this, 8));
        p.addView(a);

        LinearLayout c = Ui.cartao(this);
        c.addView(Ui.texto(this, "Desenho aproximado do corpo", 16, Ui.TEXTO, true));
        imgCorpo = new ImageView(this);
        imgCorpo.setAdjustViewBounds(true);
        c.addView(imgCorpo, Ui.largura(this, 8));
        p.addView(c);

        LinearLayout g = Ui.cartao(this);
        g.addView(Ui.texto(this, "Régua de progresso", 16, Ui.TEXTO, true));
        imgRegua = new ImageView(this);
        imgRegua.setAdjustViewBounds(true);
        g.addView(imgRegua, Ui.largura(this, 8));
        p.addView(g);
        setContentView(Ui.rolagem(this, p));
        carregar();
    }

    private void carregar() {
        Api.get(this, "/api/resumo", new Api.Resposta() {
            @Override public void ok(String corpo) {
                try {
                    JSONObject o = new JSONObject(corpo);
                    StringBuilder sb = new StringBuilder();
                    String rp = o.optString("resumoPeso", "");
                    sb.append(rp.isEmpty() ? "Cadastre medições para acompanhar o peso." : rp);
                    if (o.has("imc")) sb.append("\n\nIMC: ").append(Ui.fmt(o.optDouble("imc"))).append(" (").append(o.optString("imcClasse")).append(")");
                    if (o.optDouble("pesoMeta") > 0) sb.append("\nPeso meta: ").append(Ui.fmt(o.optDouble("pesoMeta"))).append(" kg");
                    sb.append("\nÁgua hoje: ").append(Ui.ml(o.optInt("mlHoje"))).append(" de ").append(Ui.ml(o.optInt("metaMl")));
                    int meta = o.optInt("metaKcal");
                    sb.append("\nCalorias hoje: ").append(o.optInt("kcalHoje")).append(meta > 0 ? " de " + meta + " kcal" : " kcal");
                    lblResumo.setText(sb.toString());
                    final int metaMl = o.optInt("metaMl");
                    Api.get(ProgressoActivity.this, "/api/agua/semana", new Api.Resposta() {
                        @Override public void ok(String s) {
                            try {
                                JSONArray arr = new JSONArray(s);
                                String[] r = new String[arr.length()];
                                int[] v = new int[arr.length()];
                                for (int i = 0; i < arr.length(); i++) {
                                    r[i] = LocalDate.parse(arr.getJSONObject(i).getString("dia")).format(DateTimeFormatter.ofPattern("dd/MM"));
                                    v[i] = arr.getJSONObject(i).optInt("ml");
                                }
                                barras.definir(r, v, metaMl);
                            } catch (Exception ignored) { }
                        }
                        @Override public void erro(String m) { }
                    });
                } catch (Exception e) {
                    lblResumo.setText("Erro: " + e.getMessage());
                }
            }
            @Override public void erro(String m) { lblResumo.setText(m); }
        });
        int w = getResources().getDisplayMetrics().widthPixels;
        int larg = Math.max(400, Math.min(1000, w));
        Api.imagem(this, "/api/imagem/corpo.png?w=" + larg + "&h=" + (int) (larg * 1.2), new Api.RespostaImagem() {
            @Override public void ok(Bitmap img) { imgCorpo.setImageBitmap(img); }
            @Override public void erro(String m) { }
        });
        Api.imagem(this, "/api/imagem/regua.png?w=" + Math.max(560, larg), new Api.RespostaImagem() {
            @Override public void ok(Bitmap img) { imgRegua.setImageBitmap(img); }
            @Override public void erro(String m) { }
        });
    }
}
