package br.com.avisodeagua.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Botões da notificação: "Já bebi" e "Lembrar em 5 min". */
public class AcaoReceiver extends BroadcastReceiver {

    static final String BEBI = "br.com.avisodeagua.app.BEBI";
    static final String ADIAR = "br.com.avisodeagua.app.ADIAR";

    @Override
    public void onReceive(Context c, Intent intent) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(Lembretes.ID_NOTIFICACAO);
        final PendingResult pr = goAsync();
        if (BEBI.equals(intent.getAction())) {
            Registro.registrar(c, "BEBEU", Prefs.copoMl(c));
        } else if (ADIAR.equals(intent.getAction())) {
            Registro.registrar(c, "ADIOU", 0);
            Lembretes.adiar(c, 5);
        }
        pr.finish();
    }
}
