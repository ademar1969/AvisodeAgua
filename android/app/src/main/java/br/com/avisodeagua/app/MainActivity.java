package br.com.avisodeagua.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Tela inicial: água de hoje, próximo lembrete, calorias e progresso do corpo. */
public class MainActivity extends Activity {

    private TextView lblOla, lblConexao, lblAgua, lblCopos, lblProximo, lblKcal, lblCorpo;
    private ProgressBar barraAgua, barraKcal;
    private Switch swLembretes;
    private Button btBebi;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Lembretes.criarCanal(this);
        LinearLayout p = Ui.pagina(this);

        lblOla = Ui.texto(this, "Olá!", 22, Ui.AZUL_ESCURO, true);
        p.addView(lblOla);
        lblConexao = Ui.texto(this, "", 13, Ui.SUAVE, false);
        p.addView(lblConexao, Ui.largura(this, 2));

        // água
        LinearLayout agua = Ui.cartao(this);
        ((LinearLayout.LayoutParams) agua.getLayoutParams()).topMargin = Ui.dp(this, 12);
        agua.addView(Ui.texto(this, "💧 Água hoje", 15, Ui.SUAVE, true));
        lblAgua = Ui.texto(this, "—", 30, Ui.AZUL_ESCURO, true);
        agua.addView(lblAgua, Ui.largura(this, 4));
        barraAgua = Ui.barra(this);
        agua.addView(barraAgua, Ui.largura(this, 8));
        lblCopos = Ui.texto(this, "", 14, Ui.TEXTO, false);
        agua.addView(lblCopos, Ui.largura(this, 6));
        btBebi = Ui.botao(this, "+ Bebi um copo", true);
        btBebi.setOnClickListener(v -> bebi());
        agua.addView(btBebi, Ui.largura(this, 12));
        p.addView(agua);

        // lembretes
        LinearLayout lem = Ui.cartao(this);
        LinearLayout l1 = Ui.linha(this);
        TextView t = Ui.texto(this, "⏰ Lembretes neste celular", 15, Ui.TEXTO, true);
        l1.addView(t, Ui.peso(1));
        swLembretes = new Switch(this);
        swLembretes.setOnCheckedChangeListener((v, on) -> {
            Prefs.get(this).edit().putBoolean("lembretesCelular", on).apply();
            Lembretes.agendar(this);
            atualizarProximo();
        });
        l1.addView(swLembretes);
        lem.addView(l1);
        lblProximo = Ui.texto(this, "", 14, Ui.SUAVE, false);
        lem.addView(lblProximo, Ui.largura(this, 6));
        Button teste = Ui.botao(this, "Ver o alerta da cachoeira", false);
        teste.setOnClickListener(v -> startActivity(new Intent(this, AlertaActivity.class)));
        lem.addView(teste, Ui.largura(this, 10));
        p.addView(lem);

        // alimentação
        LinearLayout ali = Ui.cartao(this);
        ali.addView(Ui.texto(this, "🍽️ Alimentação de hoje", 15, Ui.SUAVE, true));
        lblKcal = Ui.texto(this, "—", 20, Ui.AZUL_ESCURO, true);
        ali.addView(lblKcal, Ui.largura(this, 4));
        barraKcal = Ui.barra(this);
        ali.addView(barraKcal, Ui.largura(this, 8));
        Button btAli = Ui.botao(this, "Registrar refeição", false);
        btAli.setOnClickListener(v -> abrir(AlimentacaoActivity.class));
        ali.addView(btAli, Ui.largura(this, 12));
        p.addView(ali);

        // corpo
        LinearLayout corpo = Ui.cartao(this);
        corpo.addView(Ui.texto(this, "📏 Medidas e progresso", 15, Ui.SUAVE, true));
        lblCorpo = Ui.texto(this, "", 14, Ui.TEXTO, false);
        corpo.addView(lblCorpo, Ui.largura(this, 6));
        LinearLayout bts = Ui.linha(this);
        Button btMed = Ui.botao(this, "Nova medição", false);
        Button btProg = Ui.botao(this, "Ver progresso", false);
        btMed.setOnClickListener(v -> abrir(MedidasActivity.class));
        btProg.setOnClickListener(v -> abrir(ProgressoActivity.class));
        LinearLayout.LayoutParams lp1 = Ui.peso(1);
        lp1.rightMargin = Ui.dp(this, 6);
        bts.addView(btMed, lp1);
        bts.addView(btProg, Ui.peso(1));
        corpo.addView(bts, Ui.largura(this, 12));
        p.addView(corpo);

        Button cfg = Ui.botao(this, "⚙️ Configurar conexão com o PC", false);
        cfg.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)));
        p.addView(cfg, Ui.largura(this, 4));
        TextView rod = Ui.texto(this, "Os dados ficam no banco do programa Aviso de Água do computador.", 12, Ui.SUAVE, false);
        rod.setGravity(Gravity.CENTER);
        p.addView(rod, Ui.largura(this, 14));

        setContentView(Ui.rolagem(this, p));
        pedirPermissoes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizar();
    }

    private void atualizar() {
        swLembretes.setChecked(Prefs.lembretesCelular(this));
        if (!Prefs.configurado(this)) {
            lblConexao.setText("Ainda não conectado ao computador. Toque em Configurar.");
            mostrarAgua(-1, -1);
            Lembretes.agendar(this);
            atualizarProximo();
            return;
        }
        lblConexao.setText("Atualizando...");
        new Thread(() -> {
            Registro.sincronizar(this);
            runOnUiThread(this::carregar);
        }).start();
    }

    private void carregar() {
        Api.get(this, "/api/resumo", new Api.Resposta() {
            @Override public void ok(String corpo) {
                try {
                    JSONObject r = new JSONObject(corpo);
                    // os horários e o intervalo vêm do cadastro do PC
                    Prefs.get(MainActivity.this).edit()
                            .putString("nome", r.optString("nome"))
                            .putInt("copoMl", r.optInt("copoMl", 250))
                            .putInt("metaMl", r.optInt("metaMl", 2000))
                            .putInt("intervalo", r.optInt("intervaloMinutos", 60))
                            .putString("inicio", r.optString("horaInicio", "08:00"))
                            .putString("fim", r.optString("horaFim", "22:00"))
                            .putBoolean("lembretesPc", r.optBoolean("lembretesAtivos", true))
                            .apply();
                    Lembretes.agendar(MainActivity.this);
                    String nome = r.optString("nome", "");
                    lblOla.setText("Olá, " + (nome.isEmpty() ? "" : nome.split("\\s+")[0]) + "!");
                    int pend = Registro.pendentes(MainActivity.this);
                    lblConexao.setText("Conectado ao computador" + (pend > 0 ? " • " + pend + " registro(s) para enviar" : ""));
                    mostrarAgua(r.optInt("mlHoje") + Registro.mlPendenteHoje(MainActivity.this), r.optInt("metaMl"));
                    int kcal = r.optInt("kcalHoje"), meta = r.optInt("metaKcal");
                    lblKcal.setText(meta > 0 ? kcal + " de " + meta + " kcal" : kcal + " kcal");
                    barraKcal.setProgress(meta > 0 ? Math.min(100, kcal * 100 / meta) : 0);
                    StringBuilder sb = new StringBuilder();
                    String resumo = r.optString("resumoPeso", "");
                    sb.append(resumo.isEmpty() ? "Nenhuma medição ainda. Toque em Nova medição." : resumo);
                    if (r.has("imc")) sb.append("\nIMC: ").append(Ui.fmt(r.optDouble("imc"))).append(" (").append(r.optString("imcClasse")).append(")");
                    String prox = r.isNull("proximaMedida") ? "" : r.optString("proximaMedida", "");
                    if (!prox.isEmpty()) {
                        LocalDate d = LocalDate.parse(prox);
                        sb.append("\nPróxima medição sugerida: ").append(d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        if (!d.isAfter(LocalDate.now())) sb.append(" — já está na hora!");
                    }
                    lblCorpo.setText(sb.toString());
                } catch (Exception e) {
                    lblConexao.setText("Resposta inesperada do computador.");
                }
                atualizarProximo();
            }

            @Override public void erro(String m) {
                lblConexao.setText("Sem conexão: " + m);
                mostrarAgua(-1, Prefs.metaMl(MainActivity.this));
                Lembretes.agendar(MainActivity.this);
                atualizarProximo();
            }
        });
    }

    private void mostrarAgua(int mlHoje, int meta) {
        int copo = Prefs.copoMl(this);
        btBebi.setText("+ Bebi um copo (" + copo + " ml)");
        if (mlHoje < 0) {
            int pend = Registro.mlPendenteHoje(this);
            lblAgua.setText(pend > 0 ? Ui.ml(pend) + " (não enviados)" : "—");
            lblCopos.setText(meta > 0 ? "Meta: " + Ui.ml(meta) + " por dia" : "");
            barraAgua.setProgress(meta > 0 ? Math.min(100, pend * 100 / meta) : 0);
            return;
        }
        lblAgua.setText(Ui.ml(mlHoje) + " de " + Ui.ml(meta));
        barraAgua.setProgress(meta > 0 ? Math.min(100, mlHoje * 100 / meta) : 0);
        int falta = Math.max(0, meta - mlHoje);
        lblCopos.setText(falta > 0 ? "Faltam " + Ui.ml(falta) + " (" + (int) Math.ceil(falta / (double) copo) + " copos)"
                : "Meta de hoje atingida! 🎉");
    }

    private void atualizarProximo() {
        long ms = Prefs.proximoAlarme(this);
        if (!Prefs.lembretesCelular(this)) { lblProximo.setText("Desligados neste celular."); return; }
        if (!Prefs.lembretesPc(this)) { lblProximo.setText("Pausados no cadastro do computador."); return; }
        if (ms <= 0) { lblProximo.setText(""); return; }
        LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        String dia = t.toLocalDate().equals(LocalDate.now()) ? "hoje" : "amanhã";
        lblProximo.setText("Próximo: " + dia + " às " + t.format(DateTimeFormatter.ofPattern("HH:mm"))
                + "  •  a cada " + Prefs.intervalo(this) + " min, das " + Prefs.inicio(this) + " às " + Prefs.fim(this));
    }

    private void bebi() {
        Registro.registrar(this, "MANUAL", Prefs.copoMl(this));
        Ui.aviso(this, "Copo registrado! 💧");
        lblAgua.postDelayed(this::atualizar, 1200);
    }

    private void abrir(Class<?> tela) {
        if (!Prefs.configurado(this)) {
            Ui.aviso(this, "Configure primeiro a conexão com o computador.");
            startActivity(new Intent(this, ConfigActivity.class));
            return;
        }
        startActivity(new Intent(this, tela));
    }

    private void pedirPermissoes() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms() && !Prefs.get(this).getBoolean("perguntouAlarme", false)) {
                Prefs.get(this).edit().putBoolean("perguntouAlarme", true).apply();
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Lembretes no horário certo")
                        .setMessage("Para o lembrete tocar exatamente na hora, permita \"Alarmes e lembretes\" para o Aviso de Água.")
                        .setPositiveButton("Permitir", (d, w) -> {
                            try {
                                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:" + getPackageName())));
                            } catch (Exception ignored) { }
                        })
                        .setNegativeButton("Agora não", null)
                        .show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Configurar");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) { startActivity(new Intent(this, ConfigActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
