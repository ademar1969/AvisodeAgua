package br.com.avisodeagua.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;

/** Chega na hora do lembrete: mostra a notificação "Hora de tomar água" e agenda o próximo. */
public class AlarmeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        mostrar(c);
        Lembretes.agendar(c);
    }

    static void mostrar(Context c) {
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Lembretes.criarCanal(c);
        Intent abrir = new Intent(c, AlertaActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent piAbrir = PendingIntent.getActivity(c, 10, abrir, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piBebi = PendingIntent.getBroadcast(c, 11,
                new Intent(c, AcaoReceiver.class).setAction(AcaoReceiver.BEBI),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piAdiar = PendingIntent.getBroadcast(c, 12,
                new Intent(c, AcaoReceiver.class).setAction(AcaoReceiver.ADIAR),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String nome = Prefs.nome(c);
        String primeiro = nome.trim().isEmpty() ? "" : nome.trim().split("\\s+")[0] + ", ";
        Icon icone = Icon.createWithResource(c, R.drawable.ic_notificacao);
        Notification n = new Notification.Builder(c, Lembretes.CANAL)
                .setSmallIcon(icone)
                .setContentTitle("Hora de tomar água! 💧")
                .setContentText(primeiro + "beba um copo de água (" + Prefs.copoMl(c) + " ml).")
                .setColor(Ui.AZUL)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(piAbrir)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(icone, "Já bebi", piBebi).build())
                .addAction(new Notification.Action.Builder(icone, "Lembrar em 5 min", piAdiar).build())
                .build();
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(Lembretes.ID_NOTIFICACAO, n);
    }
}
