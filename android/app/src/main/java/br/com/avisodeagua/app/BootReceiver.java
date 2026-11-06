package br.com.avisodeagua.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Depois de reiniciar o celular (ou atualizar o app), agenda os lembretes de novo. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Lembretes.agendar(c);
    }
}
