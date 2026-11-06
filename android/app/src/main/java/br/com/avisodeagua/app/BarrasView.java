package br.com.avisodeagua.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Gráfico de barras simples (água dos últimos 7 dias) com linha da meta. */
public class BarrasView extends View {

    private String[] rotulos = new String[0];
    private int[] valores = new int[0];
    private int meta;
    private final Paint barra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linhaMeta = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarrasView(Context c) {
        super(c);
        texto.setColor(Ui.TEXTO);
        texto.setTextSize(Ui.dp(c, 11));
        texto.setTextAlign(Paint.Align.CENTER);
        linhaMeta.setColor(0xFFF0A030);
        linhaMeta.setStrokeWidth(Ui.dp(c, 2));
        linhaMeta.setStyle(Paint.Style.STROKE);
        linhaMeta.setPathEffect(new DashPathEffect(new float[]{Ui.dp(c, 6), Ui.dp(c, 4)}, 0));
    }

    public void definir(String[] rotulos, int[] valores, int meta) {
        this.rotulos = rotulos;
        this.valores = valores;
        this.meta = meta;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(getContext(), 190));
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = valores.length;
        if (n == 0) return;
        float topo = Ui.dp(getContext(), 18), base = getHeight() - Ui.dp(getContext(), 22);
        float max = Math.max(meta, 1);
        for (int v : valores) max = Math.max(max, v);
        max *= 1.1f;
        float larg = getWidth() / (float) n;
        for (int i = 0; i < n; i++) {
            float h = (base - topo) * valores[i] / max;
            float x = i * larg + larg * 0.18f;
            barra.setColor(meta > 0 && valores[i] >= meta ? Ui.AZUL : 0xFFA9D6F3);
            c.drawRoundRect(new RectF(x, base - h, x + larg * 0.64f, base), 8, 8, barra);
            c.drawText(rotulos[i], i * larg + larg / 2, getHeight() - Ui.dp(getContext(), 6), texto);
            if (valores[i] > 0) c.drawText(Ui.ml(valores[i]), i * larg + larg / 2, base - h - Ui.dp(getContext(), 4), texto);
        }
        if (meta > 0) {
            float y = base - (base - topo) * meta / max;
            c.drawLine(0, y, getWidth(), y, linhaMeta);
        }
    }
}
