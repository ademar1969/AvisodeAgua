package br.com.avisodeagua.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Registro aproximado do que foi comido no dia. */
public class AlimentacaoActivity extends Activity {

    private static final String[] REFEICOES = {
            "Café da manhã", "Lanche da manhã", "Almoço", "Lanche da tarde", "Jantar", "Ceia"};

    private LocalDate dia = LocalDate.now();
    private TextView lblDia, lblTotal;
    private ProgressBar barra;
    private Spinner spRefeicao;
    private AutoCompleteTextView campoAlimento;
    private EditText campoQtd, campoKcal;
    private TextView lblPorcao;
    private LinearLayout lista;
    private final Map<String, JSONObject> alimentos = new HashMap<>();
    private int kcalPorcao = 0;
    private String porcao = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout p = Ui.pagina(this);

        LinearLayout nav = Ui.linha(this);
        Button ant = Ui.botao(this, "‹", false);
        Button prox = Ui.botao(this, "›", false);
        lblDia = Ui.texto(this, "", 17, Ui.AZUL_ESCURO, true);
        lblDia.setGravity(android.view.Gravity.CENTER);
        ant.setOnClickListener(v -> { dia = dia.minusDays(1); carregar(); });
        prox.setOnClickListener(v -> { if (dia.isBefore(LocalDate.now())) { dia = dia.plusDays(1); carregar(); } });
        nav.addView(ant);
        nav.addView(lblDia, Ui.peso(1));
        nav.addView(prox);
        p.addView(nav);

        LinearLayout tot = Ui.cartao(this);
        ((LinearLayout.LayoutParams) tot.getLayoutParams()).topMargin = Ui.dp(this, 10);
        lblTotal = Ui.texto(this, "—", 22, Ui.AZUL_ESCURO, true);
        tot.addView(lblTotal);
        barra = Ui.barra(this);
        tot.addView(barra, Ui.largura(this, 8));
        tot.addView(Ui.texto(this, "Calorias aproximadas, por porção caseira.", 12, Ui.SUAVE, false), Ui.largura(this, 6));
        p.addView(tot);

        LinearLayout add = Ui.cartao(this);
        add.addView(Ui.texto(this, "Adicionar", 16, Ui.TEXTO, true));
        spRefeicao = new Spinner(this);
        spRefeicao.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, REFEICOES));
        int h = LocalTime.now().getHour();
        spRefeicao.setSelection(h < 10 ? 0 : h < 12 ? 1 : h < 15 ? 2 : h < 18 ? 3 : h < 22 ? 4 : 5);
        add.addView(spRefeicao, Ui.largura(this, 6));
        campoAlimento = new AutoCompleteTextView(this);
        campoAlimento.setHint("Alimento (digite para buscar)");
        campoAlimento.setThreshold(1);
        campoAlimento.setSingleLine(true);
        campoAlimento.setOnItemClickListener((parent, view, pos, id) -> escolher((String) parent.getItemAtPosition(pos)));
        add.addView(campoAlimento, Ui.largura(this, 6));
        lblPorcao = Ui.texto(this, "", 13, Ui.SUAVE, false);
        add.addView(lblPorcao);
        LinearLayout l2 = Ui.linha(this);
        campoQtd = Ui.campo(this, "Qtd. porções", true);
        campoQtd.setText("1");
        campoKcal = Ui.campo(this, "kcal total", true);
        LinearLayout.LayoutParams lq = Ui.peso(1);
        lq.rightMargin = Ui.dp(this, 8);
        l2.addView(campoQtd, lq);
        l2.addView(campoKcal, Ui.peso(1));
        add.addView(l2, Ui.largura(this, 6));
        campoQtd.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            public void onTextChanged(CharSequence s, int a, int b2, int c) { }
            public void afterTextChanged(Editable s) { recalcular(); }
        });
        Button btAdd = Ui.botao(this, "Adicionar", true);
        btAdd.setOnClickListener(v -> adicionar());
        add.addView(btAdd, Ui.largura(this, 10));
        p.addView(add);

        p.addView(Ui.texto(this, "Comido neste dia (toque e segure para remover)", 14, Ui.SUAVE, true), Ui.largura(this, 4));
        lista = Ui.coluna(this);
        p.addView(lista, Ui.largura(this, 6));
        setContentView(Ui.rolagem(this, p));

        carregarAlimentos();
        carregar();
    }

    private void carregarAlimentos() {
        Api.get(this, "/api/alimentos", new Api.Resposta() {
            @Override public void ok(String corpo) {
                try {
                    JSONArray a = new JSONArray(corpo);
                    List<String> nomes = new ArrayList<>();
                    for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.getJSONObject(i);
                        alimentos.put(o.getString("nome"), o);
                        nomes.add(o.getString("nome"));
                    }
                    campoAlimento.setAdapter(new ArrayAdapter<>(AlimentacaoActivity.this,
                            android.R.layout.simple_dropdown_item_1line, nomes));
                } catch (Exception ignored) { }
            }
            @Override public void erro(String m) { Ui.aviso(AlimentacaoActivity.this, m); }
        });
    }

    private void escolher(String nome) {
        JSONObject o = alimentos.get(nome);
        if (o == null) return;
        kcalPorcao = o.optInt("kcal");
        porcao = o.optString("porcao");
        lblPorcao.setText("Porção: " + porcao + " — " + kcalPorcao + " kcal");
        recalcular();
    }

    private void recalcular() {
        double q = Ui.ler(campoQtd.getText().toString());
        if (kcalPorcao > 0 && q > 0) campoKcal.setText(String.valueOf(Math.round(kcalPorcao * q)));
    }

    private void adicionar() {
        String nome = campoAlimento.getText().toString().trim();
        if (nome.isEmpty()) { Ui.aviso(this, "Escolha ou digite um alimento."); return; }
        double q = Ui.ler(campoQtd.getText().toString());
        if (q <= 0) q = 1;
        double kcal = Ui.ler(campoKcal.getText().toString());
        if (kcal < 0) kcal = 0;
        if (!alimentos.containsKey(nome)) porcao = "";
        try {
            JSONObject o = new JSONObject();
            o.put("data", dia.toString());
            o.put("refeicao", REFEICOES[spRefeicao.getSelectedItemPosition()]);
            o.put("descricao", nome);
            o.put("porcao", porcao);
            o.put("quantidade", q);
            o.put("kcal", Math.round(kcal));
            Api.post(this, "/api/refeicoes", o, new Api.Resposta() {
                @Override public void ok(String c) {
                    campoAlimento.setText("");
                    campoQtd.setText("1");
                    campoKcal.setText("");
                    lblPorcao.setText("");
                    kcalPorcao = 0;
                    porcao = "";
                    carregar();
                }
                @Override public void erro(String m) { Ui.aviso(AlimentacaoActivity.this, m); }
            });
        } catch (Exception e) {
            Ui.aviso(this, e.getMessage());
        }
    }

    private void carregar() {
        lblDia.setText((dia.equals(LocalDate.now()) ? "Hoje, " : "") + dia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Api.get(this, "/api/refeicoes?data=" + dia, new Api.Resposta() {
            @Override public void ok(String corpo) {
                try {
                    JSONObject r = new JSONObject(corpo);
                    int total = r.optInt("total"), meta = r.optInt("meta");
                    lblTotal.setText(meta > 0 ? total + " de " + meta + " kcal" : total + " kcal");
                    barra.setProgress(meta > 0 ? Math.min(100, total * 100 / meta) : 0);
                    lista.removeAllViews();
                    JSONArray itens = r.getJSONArray("itens");
                    if (itens.length() == 0) lista.addView(Ui.texto(AlimentacaoActivity.this, "Nada registrado.", 14, Ui.SUAVE, false));
                    String ultimaRef = "";
                    for (int i = 0; i < itens.length(); i++) {
                        final JSONObject it = itens.getJSONObject(i);
                        String ref = it.optString("refeicao");
                        if (!ref.equals(ultimaRef)) {
                            lista.addView(Ui.texto(AlimentacaoActivity.this, ref, 14, Ui.AZUL, true), Ui.largura(AlimentacaoActivity.this, 10));
                            ultimaRef = ref;
                        }
                        LinearLayout linha = Ui.linha(AlimentacaoActivity.this);
                        int pd = Ui.dp(AlimentacaoActivity.this, 6);
                        linha.setPadding(0, pd, 0, pd);
                        String q = Ui.fmt(it.optDouble("quantidade", 1)).replace(",0", "");
                        TextView desc = Ui.texto(AlimentacaoActivity.this, q + "× " + it.optString("descricao")
                                + (it.optString("porcao").isEmpty() ? "" : "\n" + it.optString("porcao")), 15, Ui.TEXTO, false);
                        linha.addView(desc, Ui.peso(1));
                        linha.addView(Ui.texto(AlimentacaoActivity.this, it.optInt("kcal") + " kcal", 15, Ui.AZUL_ESCURO, true));
                        linha.setOnLongClickListener(v -> { remover(it); return true; });
                        lista.addView(linha);
                    }
                } catch (Exception e) {
                    Ui.aviso(AlimentacaoActivity.this, "Erro: " + e.getMessage());
                }
            }
            @Override public void erro(String m) {
                lblTotal.setText("Sem conexão");
                Ui.aviso(AlimentacaoActivity.this, m);
            }
        });
    }

    private void remover(final JSONObject it) {
        new AlertDialog.Builder(this)
                .setMessage("Remover \"" + it.optString("descricao") + "\"?")
                .setPositiveButton("Remover", (d, w) -> {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("id", it.optInt("id"));
                        Api.post(this, "/api/refeicoes/remover", o, new Api.Resposta() {
                            @Override public void ok(String c) { carregar(); }
                            @Override public void erro(String m) { Ui.aviso(AlimentacaoActivity.this, m); }
                        });
                    } catch (Exception ignored) { }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
