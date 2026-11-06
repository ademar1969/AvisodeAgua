package br.com.avisodeagua.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Conversa com o programa do PC (API do Aviso de Água). */
final class Api {

    interface Resposta {
        void ok(String corpo);
        void erro(String mensagem);
    }

    interface RespostaImagem {
        void ok(Bitmap imagem);
        void erro(String mensagem);
    }

    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private Api() { }

    static String base(Context c) {
        String s = Prefs.servidor(c).trim();
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://" + s;
        if (!s.matches(".*:\\d+$") && !s.startsWith("https://")) s = s + ":8085";
        return s;
    }

    // ---------------------------------------------------------------- chamadas síncronas (usar fora da thread principal)

    static String getSinc(Context c, String caminho) throws IOException {
        return chamar(c, "GET", caminho, null);
    }

    static String postSinc(Context c, String caminho, JSONObject corpo) throws IOException {
        return chamar(c, "POST", caminho, corpo.toString());
    }

    private static String chamar(Context c, String metodo, String caminho, String corpo) throws IOException {
        if (!Prefs.configurado(c)) throw new IOException("Configure o endereço do computador e o código de acesso.");
        HttpURLConnection con = (HttpURLConnection) new URL(base(c) + caminho).openConnection();
        con.setConnectTimeout(6000);
        con.setReadTimeout(30000);
        con.setRequestMethod(metodo);
        con.setRequestProperty("X-Token", Prefs.token(c).trim());
        con.setRequestProperty("Accept", "application/json");
        try {
            if (corpo != null) {
                byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                con.setFixedLengthStreamingMode(b.length);
                try (OutputStream os = con.getOutputStream()) { os.write(b); }
            }
            int st = con.getResponseCode();
            InputStream in = st >= 400 ? con.getErrorStream() : con.getInputStream();
            String txt = in == null ? "" : new String(ler(in), StandardCharsets.UTF_8);
            if (st >= 400) {
                String msg = "Erro " + st;
                try { msg = new JSONObject(txt).optString("erro", msg); } catch (Exception ignored) { }
                throw new IOException(msg);
            }
            return txt;
        } catch (java.net.ConnectException | java.net.SocketTimeoutException | java.net.UnknownHostException e) {
            throw new IOException("Não consegui falar com o computador (" + base(c) + "). "
                    + "Verifique se o programa está aberto no PC e se o celular está no mesmo Wi-Fi.");
        } finally {
            con.disconnect();
        }
    }

    static byte[] ler(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    // ---------------------------------------------------------------- chamadas assíncronas (resposta na tela)

    static void get(final Context c, final String caminho, final Resposta r) {
        new Thread(() -> {
            try {
                final String s = getSinc(c, caminho);
                PRINCIPAL.post(() -> r.ok(s));
            } catch (final Exception e) {
                PRINCIPAL.post(() -> r.erro(e.getMessage()));
            }
        }).start();
    }

    static void post(final Context c, final String caminho, final JSONObject corpo, final Resposta r) {
        new Thread(() -> {
            try {
                final String s = postSinc(c, caminho, corpo);
                PRINCIPAL.post(() -> r.ok(s));
            } catch (final Exception e) {
                PRINCIPAL.post(() -> r.erro(e.getMessage()));
            }
        }).start();
    }

    static void imagem(final Context c, final String caminho, final RespostaImagem r) {
        new Thread(() -> {
            HttpURLConnection con = null;
            try {
                String sep = caminho.contains("?") ? "&" : "?";
                con = (HttpURLConnection) new URL(base(c) + caminho + sep + "token=" + Prefs.token(c).trim()).openConnection();
                con.setConnectTimeout(6000);
                con.setReadTimeout(30000);
                if (con.getResponseCode() != 200) throw new IOException("Imagem indisponível (" + con.getResponseCode() + ")");
                final Bitmap b = BitmapFactory.decodeStream(con.getInputStream());
                PRINCIPAL.post(() -> r.ok(b));
            } catch (final Exception e) {
                PRINCIPAL.post(() -> r.erro(e.getMessage()));
            } finally {
                if (con != null) con.disconnect();
            }
        }).start();
    }
}
