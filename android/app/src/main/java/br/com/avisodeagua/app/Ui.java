package br.com.avisodeagua.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Ajudantes para montar as telas por código (sem arquivos de layout). */
final class Ui {

    static final int AZUL = 0xFF1E90DC;
    static final int AZUL_ESCURO = 0xFF0B4F75;
    static final int TEXTO = 0xFF33414E;
    static final int SUAVE = 0xFF7A8896;
    static final int VERDE = 0xFF2E9E5B;
    static final int LARANJA = 0xFFD9822B;
    static final int FUNDO = 0xFFF2F8FC;
    static final Locale BR = new Locale("pt", "BR");

    private Ui() { }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static LinearLayout coluna(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout linha(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static TextView texto(Context c, String s, float sp, int cor, boolean negrito) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(cor);
        if (negrito) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static Button botao(Context c, String s, boolean principal) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTextColor(principal ? 0xFFFFFFFF : AZUL_ESCURO);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 24));
        g.setColor(principal ? AZUL : 0xFFE4F1FA);
        b.setBackground(g);
        b.setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10));
        return b;
    }

    /** Cartão branco com cantos arredondados. */
    static LinearLayout cartao(Context c) {
        LinearLayout l = coluna(c);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, 14));
        g.setColor(0xFFFFFFFF);
        g.setStroke(dp(c, 1), 0xFFDCE6EE);
        l.setBackground(g);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(c, 12));
        l.setLayoutParams(lp);
        return l;
    }

    static ScrollView rolagem(Context c, View conteudo) {
        ScrollView s = new ScrollView(c);
        s.setBackgroundColor(FUNDO);
        s.setFillViewport(true);
        s.addView(conteudo);
        return s;
    }

    static LinearLayout pagina(Context c) {
        LinearLayout l = coluna(c);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    static EditText campo(Context c, String dica, boolean numero) {
        EditText e = new EditText(c);
        e.setHint(dica);
        e.setSingleLine(true);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        if (numero) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    static ProgressBar barra(Context c) {
        ProgressBar p = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(100);
        p.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 14)));
        return p;
    }

    static LinearLayout.LayoutParams largura(Context c, int topo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topo);
        return lp;
    }

    static LinearLayout.LayoutParams peso(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    static void aviso(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_LONG).show();
    }

    static String fmt(double v) {
        return String.format(BR, "%.1f", v);
    }

    static double ler(String s) {
        if (s == null) return 0;
        s = s.trim().replace(',', '.');
        if (s.isEmpty()) return 0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return -1; }
    }

    /** 1500 -> "1,5 L"; 750 -> "750 ml". */
    static String ml(int ml) {
        if (ml >= 1000) {
            String v = String.format(BR, "%.2f", ml / 1000.0);
            if (v.endsWith("0")) v = v.substring(0, v.length() - 1);
            return v + " L";
        }
        return ml + " ml";
    }
}
