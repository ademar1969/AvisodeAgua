package br.com.avisodeagua.app;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

/** Endereço do computador e código de acesso (aparecem no programa do PC, botão "Celular"). */
public class ConfigActivity extends Activity {

    private EditText campoServidor, campoToken;
    private TextView lblResultado;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout p = Ui.pagina(this);
        LinearLayout c = Ui.cartao(this);
        c.addView(Ui.texto(this, "Conectar ao computador", 20, Ui.AZUL_ESCURO, true));
        c.addView(Ui.texto(this, "No programa Aviso de Água do PC, clique em \"Celular\". Lá aparecem o endereço "
                + "e o código de acesso. O celular precisa estar no mesmo Wi-Fi do computador.", 14, Ui.TEXTO, false),
                Ui.largura(this, 8));
        c.addView(Ui.texto(this, "Endereço do computador", 13, Ui.SUAVE, true), Ui.largura(this, 16));
        campoServidor = Ui.campo(this, "ex.: 192.168.0.10:8085", false);
        campoServidor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campoServidor.setText(Prefs.servidor(this));
        c.addView(campoServidor);
        c.addView(Ui.texto(this, "Código de acesso", 13, Ui.SUAVE, true), Ui.largura(this, 12));
        campoToken = Ui.campo(this, "8 letras/números", false);
        campoToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        campoToken.setText(Prefs.token(this));
        c.addView(campoToken);

        Button testar = Ui.botao(this, "Testar conexão e salvar", true);
        testar.setOnClickListener(v -> testar());
        c.addView(testar, Ui.largura(this, 16));
        lblResultado = Ui.texto(this, "", 14, Ui.TEXTO, false);
        c.addView(lblResultado, Ui.largura(this, 10));
        p.addView(c);

        LinearLayout ajuda = Ui.cartao(this);
        ajuda.addView(Ui.texto(this, "Se não conectar", 15, Ui.TEXTO, true));
        ajuda.addView(Ui.texto(this, "• O programa precisa estar aberto no PC (ícone de gota perto do relógio).\n"
                + "• Confira se o celular e o PC estão na mesma rede Wi-Fi.\n"
                + "• No PC, permita o Java no Firewall do Windows (o instalador já libera a porta 8085).\n"
                + "• Os lembretes funcionam mesmo sem conexão; os copos são enviados depois.", 13, Ui.TEXTO, false),
                Ui.largura(this, 6));
        p.addView(ajuda);
        setContentView(Ui.rolagem(this, p));
    }

    private void testar() {
        String s = campoServidor.getText().toString().trim();
        String t = campoToken.getText().toString().trim().toUpperCase();
        if (s.isEmpty() || t.isEmpty()) { Ui.aviso(this, "Preencha o endereço e o código."); return; }
        Prefs.get(this).edit().putString("servidor", s).putString("token", t).apply();
        lblResultado.setTextColor(Ui.SUAVE);
        lblResultado.setText("Testando " + Api.base(this) + " ...");
        Api.get(this, "/api/ping", new Api.Resposta() {
            @Override public void ok(String corpo) {
                try {
                    JSONObject o = new JSONObject(corpo);
                    lblResultado.setTextColor(Ui.VERDE);
                    lblResultado.setText("Conectado! Olá, " + o.optString("nome") + ". Banco " + o.optString("banco") + ".");
                    Prefs.get(ConfigActivity.this).edit().putString("nome", o.optString("nome")).apply();
                    Registro.sincronizarEmSegundoPlano(ConfigActivity.this);
                } catch (Exception e) {
                    lblResultado.setText("Resposta inesperada: " + e.getMessage());
                }
            }

            @Override public void erro(String m) {
                lblResultado.setTextColor(Ui.LARANJA);
                lblResultado.setText(m);
            }
        });
    }
}
