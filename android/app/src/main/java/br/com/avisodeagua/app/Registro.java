package br.com.avisodeagua.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Copos de água registrados no celular. Ficam numa fila e são enviados ao PC
 * assim que houver conexão (funciona sem internet).
 */
final class Registro {

    private Registro() { }

    /** acao: MANUAL (botão do app), BEBEU (no alerta), ADIOU ou IGNOROU. */
    static synchronized void registrar(Context c, String acao, int ml) {
        SharedPreferences p = Prefs.get(c);
        try {
            JSONArray fila = new JSONArray(p.getString("pendentes", "[]"));
            JSONObject o = new JSONObject();
            o.put("acao", acao);
            o.put("ml", ml);
            o.put("quando", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
            fila.put(o);
            SharedPreferences.Editor e = p.edit().putString("pendentes", fila.toString());
            if (ml > 0) {
                // contador local do dia, para mostrar mesmo sem conexão
                String hoje = LocalDate.now().toString();
                int atual = hoje.equals(p.getString("diaLocal", "")) ? p.getInt("mlPendenteHoje", 0) : 0;
                e.putString("diaLocal", hoje).putInt("mlPendenteHoje", atual + ml);
            }
            e.apply();
        } catch (Exception ignored) { }
        sincronizarEmSegundoPlano(c);
    }

    static int pendentes(Context c) {
        try { return new JSONArray(Prefs.get(c).getString("pendentes", "[]")).length(); } catch (Exception e) { return 0; }
    }

    /** ml bebidos hoje que ainda não chegaram no PC. */
    static int mlPendenteHoje(Context c) {
        SharedPreferences p = Prefs.get(c);
        if (pendentes(c) == 0) return 0;
        return LocalDate.now().toString().equals(p.getString("diaLocal", "")) ? p.getInt("mlPendenteHoje", 0) : 0;
    }

    static void sincronizarEmSegundoPlano(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(() -> sincronizar(app)).start();
    }

    /** Envia a fila para o PC. Devolve quantos foram enviados. */
    static synchronized int sincronizar(Context c) {
        if (!Prefs.configurado(c)) return 0;
        SharedPreferences p = Prefs.get(c);
        int enviados = 0;
        try {
            JSONArray fila = new JSONArray(p.getString("pendentes", "[]"));
            JSONArray resto = new JSONArray();
            boolean falhou = false;
            for (int i = 0; i < fila.length(); i++) {
                JSONObject o = fila.getJSONObject(i);
                if (!falhou) {
                    try {
                        Api.postSinc(c, "/api/agua", o);
                        enviados++;
                        continue;
                    } catch (Exception e) {
                        falhou = true;
                    }
                }
                resto.put(o);
            }
            SharedPreferences.Editor e = p.edit().putString("pendentes", resto.toString());
            if (resto.length() == 0) e.putInt("mlPendenteHoje", 0);
            e.apply();
        } catch (Exception ignored) { }
        return enviados;
    }
}
