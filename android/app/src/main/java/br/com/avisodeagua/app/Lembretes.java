package br.com.avisodeagua.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/** Agenda os lembretes de água no próprio celular (funcionam sem conexão com o PC). */
final class Lembretes {

    static final String CANAL = "lembretes_agua";
    static final int ID_NOTIFICACAO = 1001;

    private Lembretes() { }

    static void criarCanal(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CANAL) == null) {
            NotificationChannel ch = new NotificationChannel(CANAL, "Hora de tomar água", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Lembretes para beber água");
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 300, 200, 300});
            nm.createNotificationChannel(ch);
        }
    }

    private static PendingIntent intentAlarme(Context c) {
        Intent i = new Intent(c, AlarmeReceiver.class);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Agenda o próximo lembrete conforme o intervalo e o horário (os mesmos do cadastro no PC). */
    static void agendar(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(intentAlarme(c));
        if (!Prefs.lembretesCelular(c) || !Prefs.lembretesPc(c)) {
            Prefs.get(c).edit().putLong("proximo", 0).apply();
            return;
        }
        LocalDateTime prox = calcularProximo(c, LocalDateTime.now());
        agendarEm(c, prox);
    }

    /** Lembrar de novo daqui a alguns minutos (botão "+5 min"). */
    static void adiar(Context c, int minutos) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(intentAlarme(c));
        agendarEm(c, LocalDateTime.now().plusMinutes(minutos));
    }

    private static void agendarEm(Context c, LocalDateTime quando) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long ms = quando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        PendingIntent pi = intentAlarme(c);
        boolean exato = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exato) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi);
        Prefs.get(c).edit().putLong("proximo", ms).apply();
    }

    static LocalDateTime calcularProximo(Context c, LocalDateTime depoisDe) {
        int intervalo = Math.max(5, Prefs.intervalo(c));
        LocalTime ini = hora(Prefs.inicio(c), LocalTime.of(8, 0));
        LocalTime fim = hora(Prefs.fim(c), LocalTime.of(22, 0));
        for (int d = -1; d <= 1; d++) {
            LocalDate dia = depoisDe.toLocalDate().plusDays(d);
            LocalDateTime inicio = LocalDateTime.of(dia, ini);
            LocalDateTime termino = LocalDateTime.of(dia, fim);
            if (!termino.isAfter(inicio)) termino = termino.plusDays(1);
            if (depoisDe.isBefore(inicio)) return inicio;
            if (depoisDe.isBefore(termino)) {
                long min = Duration.between(inicio, depoisDe).toMinutes();
                LocalDateTime cand = inicio.plusMinutes((min / intervalo + 1) * intervalo);
                if (!cand.isAfter(depoisDe)) cand = cand.plusMinutes(intervalo);
                if (!cand.isAfter(termino)) return cand;
            }
        }
        return LocalDateTime.of(depoisDe.toLocalDate().plusDays(2), ini);
    }

    private static LocalTime hora(String s, LocalTime padrao) {
        try { return LocalTime.parse(s.length() == 4 ? "0" + s : s); } catch (Exception e) { return padrao; }
    }
}
