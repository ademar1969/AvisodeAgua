package br.com.avisodeagua;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Calcula os horários dos lembretes: a partir da hora inicial, a cada N minutos,
 * até a hora final. Verifica o relógio a cada 15 segundos em segundo plano.
 */
public class Agendador {

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "agendador-agua");
        t.setDaemon(true);
        return t;
    });

    private volatile Usuario usuario;
    private volatile LocalDateTime proximo;
    private volatile boolean alertaAberto;
    private final Consumer<Usuario> aoDisparar;
    private Runnable aoMudarProximo = () -> { };

    public Agendador(Consumer<Usuario> aoDisparar) {
        this.aoDisparar = aoDisparar;
    }

    public void setAoMudarProximo(Runnable r) { this.aoMudarProximo = r; }

    public void iniciar() {
        exec.scheduleAtFixedRate(this::verificar, 2, 15, TimeUnit.SECONDS);
    }

    public synchronized void atualizarUsuario(Usuario u) {
        this.usuario = u;
        this.proximo = calcularProximo(u, LocalDateTime.now());
        aoMudarProximo.run();
    }

    /** Adia o próximo lembrete em alguns minutos (botão "Lembrar depois"). */
    public synchronized void adiar(int minutos) {
        this.proximo = LocalDateTime.now().plusMinutes(minutos);
        aoMudarProximo.run();
    }

    public synchronized void setAlertaAberto(boolean aberto) {
        this.alertaAberto = aberto;
        // se o alerta ficou aberto mais que o intervalo, não dispara de novo imediatamente
        if (!aberto && proximo != null && proximo.isBefore(LocalDateTime.now())) {
            proximo = calcularProximo(usuario, LocalDateTime.now());
            aoMudarProximo.run();
        }
    }

    public LocalDateTime getProximo() { return proximo; }

    private void verificar() {
        try {
            Usuario u = usuario;
            LocalDateTime p = proximo;
            if (u == null || !u.isAtivo() || !u.isCadastrado() || p == null || alertaAberto) return;
            LocalDateTime agora = LocalDateTime.now();
            if (!agora.isBefore(p)) {
                synchronized (this) {
                    proximo = calcularProximo(u, agora.plusSeconds(1));
                }
                aoMudarProximo.run();
                aoDisparar.accept(u);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** Primeiro horário de lembrete estritamente depois de "depoisDe". */
    static LocalDateTime calcularProximo(Usuario u, LocalDateTime depoisDe) {
        if (u == null || !u.isAtivo() || !u.isCadastrado()) return null;
        int intervalo = Math.max(1, u.getIntervaloMinutos());
        LocalTime ini = u.getHoraInicio();
        LocalTime fim = u.getHoraFim();
        for (int d = -1; d <= 1; d++) {
            LocalDate dia = depoisDe.toLocalDate().plusDays(d);
            LocalDateTime inicio = LocalDateTime.of(dia, ini);
            LocalDateTime termino = LocalDateTime.of(dia, fim);
            if (!termino.isAfter(inicio)) termino = termino.plusDays(1); // janela que passa da meia-noite
            if (depoisDe.isBefore(inicio)) return inicio;
            if (depoisDe.isBefore(termino)) {
                long min = java.time.Duration.between(inicio, depoisDe).toMinutes();
                long passos = min / intervalo + 1;
                LocalDateTime cand = inicio.plusMinutes(passos * intervalo);
                if (!cand.isAfter(depoisDe)) cand = cand.plusMinutes(intervalo);
                if (!cand.isAfter(termino)) return cand;
            }
        }
        return LocalDateTime.of(depoisDe.toLocalDate().plusDays(2), ini);
    }

    public void parar() { exec.shutdownNow(); }
}
