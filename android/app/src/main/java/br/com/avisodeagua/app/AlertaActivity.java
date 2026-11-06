package br.com.avisodeagua.app;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Tela "Hora de tomar água" com a imagem da cachoeira (abre ao tocar na notificação). */
public class AlertaActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Lembretes.ID_NOTIFICACAO);

        FrameLayout raiz = new FrameLayout(this);
        ImageView img = new ImageView(this);
        img.setImageResource(R.drawable.cachoeira);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        raiz.addView(img, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View sombra = new View(this);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xCC001E3C, 0x33001E3C, 0x00001E3C, 0xCC001E3C});
        sombra.setBackground(g);
        raiz.addView(sombra, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topo = Ui.coluna(this);
        int p = Ui.dp(this, 28);
        topo.setPadding(p, Ui.dp(this, 56), p, p);
        topo.addView(Ui.texto(this, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), 30, 0xDDFFFFFF, false));
        TextView titulo = Ui.texto(this, "💧 Hora de tomar água!", 34, Color.WHITE, true);
        topo.addView(titulo, Ui.largura(this, 6));
        String nome = Prefs.nome(this).trim();
        String primeiro = nome.isEmpty() ? "" : nome.split("\\s+")[0] + ", ";
        topo.addView(Ui.texto(this, primeiro + "pare um minutinho e beba um copo de água (" + Prefs.copoMl(this) + " ml).",
                19, Color.WHITE, false), Ui.largura(this, 10));
        raiz.addView(topo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP));

        LinearLayout base = Ui.coluna(this);
        base.setPadding(p, p, p, Ui.dp(this, 40));
        Button bebi = Ui.botao(this, "Já bebi", true);
        Button adiar = Ui.botao(this, "Lembrar em 5 min", false);
        Button fechar = Ui.botao(this, "Fechar", false);
        bebi.setTextSize(20);
        bebi.setOnClickListener(v -> {
            Registro.registrar(this, "BEBEU", Prefs.copoMl(this));
            Ui.aviso(this, "Muito bem! Copo registrado 💧");
            finish();
        });
        adiar.setOnClickListener(v -> {
            Registro.registrar(this, "ADIOU", 0);
            Lembretes.adiar(this, 5);
            Ui.aviso(this, "Vou lembrar de novo em 5 minutos.");
            finish();
        });
        fechar.setOnClickListener(v -> {
            Registro.registrar(this, "IGNOROU", 0);
            finish();
        });
        base.addView(bebi, Ui.largura(this, 0));
        base.addView(adiar, Ui.largura(this, 10));
        base.addView(fechar, Ui.largura(this, 10));
        raiz.addView(base, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));
        setContentView(raiz);
    }
}
