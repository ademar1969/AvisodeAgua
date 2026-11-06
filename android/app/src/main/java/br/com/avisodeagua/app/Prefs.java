package br.com.avisodeagua.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Configurações guardadas no celular. */
final class Prefs {

    private Prefs() { }

    static SharedPreferences get(Context c) {
        return c.getApplicationContext().getSharedPreferences("aviso_de_agua", Context.MODE_PRIVATE);
    }

    static String servidor(Context c) { return get(c).getString("servidor", ""); }
    static String token(Context c) { return get(c).getString("token", ""); }
    static boolean configurado(Context c) { return !servidor(c).isEmpty() && !token(c).isEmpty(); }

    static String nome(Context c) { return get(c).getString("nome", ""); }
    static int copoMl(Context c) { return get(c).getInt("copoMl", 250); }
    static int metaMl(Context c) { return get(c).getInt("metaMl", 2000); }
    static int intervalo(Context c) { return get(c).getInt("intervalo", 60); }
    static String inicio(Context c) { return get(c).getString("inicio", "08:00"); }
    static String fim(Context c) { return get(c).getString("fim", "22:00"); }
    /** Lembretes neste celular (o usuário pode desligar só no celular). */
    static boolean lembretesCelular(Context c) { return get(c).getBoolean("lembretesCelular", true); }
    /** Lembretes ativos no cadastro do PC. */
    static boolean lembretesPc(Context c) { return get(c).getBoolean("lembretesPc", true); }
    static long proximoAlarme(Context c) { return get(c).getLong("proximo", 0); }
}
