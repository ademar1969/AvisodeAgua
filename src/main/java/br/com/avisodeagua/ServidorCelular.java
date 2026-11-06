package br.com.avisodeagua;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * API HTTP para o aplicativo Android. Roda dentro do programa do PC e usa o mesmo banco
 * (local ou nuvem). Toda chamada precisa do código de acesso (header X-Token ou ?token=).
 */
public class ServidorCelular {

    public static final int PORTA_PADRAO = 8085;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final Database db;
    private final CorpoDao corpo;
    private final AlimentacaoDao alim;
    private final Supplier<Usuario> usuario;
    private final Runnable aoMudar;
    private HttpServer servidor;
    private int porta = PORTA_PADRAO;
    private String token;

    public ServidorCelular(Database db, CorpoDao corpo, AlimentacaoDao alim, Supplier<Usuario> usuario, Runnable aoMudar) {
        this.db = db;
        this.corpo = corpo;
        this.alim = alim;
        this.usuario = usuario;
        this.aoMudar = aoMudar;
        Properties p = Database.carregarConfig();
        try { porta = Integer.parseInt(p.getProperty("celular.porta", String.valueOf(PORTA_PADRAO))); } catch (Exception ignored) { }
        token = p.getProperty("celular.token", "");
        if (token.length() < 6) novoToken();
    }

    // ------------------------------------------------------------------ controle

    public boolean ativoNaConfig() {
        return !"false".equalsIgnoreCase(Database.carregarConfig().getProperty("celular.ativo", "true"));
    }

    public synchronized void iniciar() throws IOException {
        if (servidor != null) return;
        servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/", this::tratar);
        servidor.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "api-celular");
            t.setDaemon(true);
            return t;
        }));
        servidor.start();
    }

    public synchronized void parar() {
        if (servidor != null) { servidor.stop(0); servidor = null; }
    }

    public synchronized boolean rodando() { return servidor != null; }

    public int getPorta() { return porta; }
    public String getToken() { return token; }

    public void configurar(boolean ativo, int novaPorta) throws IOException {
        Properties p = new Properties();
        p.setProperty("celular.ativo", String.valueOf(ativo));
        p.setProperty("celular.porta", String.valueOf(novaPorta));
        Database.salvarConfig(p);
        boolean mudouPorta = novaPorta != porta;
        porta = novaPorta;
        if (!ativo) parar();
        else {
            if (mudouPorta) parar();
            iniciar();
        }
    }

    public final void novoToken() {
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(alfabeto.charAt(r.nextInt(alfabeto.length())));
        token = sb.toString();
        Properties p = new Properties();
        p.setProperty("celular.token", token);
        Database.salvarConfig(p);
    }

    /** Endereços IPv4 deste computador na rede local (para digitar no celular). */
    public static List<String> enderecosLocais() {
        List<String> l = new ArrayList<String>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                String nome = (ni.getDisplayName() + " " + ni.getName()).toLowerCase();
                if (nome.contains("virtual") || nome.contains("vmware") || nome.contains("hyper-v") || nome.contains("vbox")) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address && !a.isLoopbackAddress() && !a.isLinkLocalAddress()) l.add(a.getHostAddress());
            }
        } catch (SocketException ignored) { }
        return l;
    }

    // ------------------------------------------------------------------ requisições

    private void tratar(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            Map<String, String> q = parametros(ex.getRequestURI().getRawQuery());
            if (caminho.equals("/") || caminho.equals("/api")) {
                texto(ex, 200, "Aviso de Água - API do aplicativo Android. Use o app no celular.");
                return;
            }
            String tk = ex.getRequestHeaders().getFirst("X-Token");
            if (tk == null) tk = q.get("token");
            if (tk == null || !tk.trim().equalsIgnoreCase(token)) {
                json(ex, 401, Json.obj("erro", "Código de acesso inválido. Confira o código na tela Celular do programa do PC."));
                return;
            }
            Usuario u = usuario.get();
            if (u == null || !u.isCadastrado() || u.getId() <= 0) {
                json(ex, 409, Json.obj("erro", "Faça o cadastro no programa do PC primeiro."));
                return;
            }
            String m = ex.getRequestMethod().toUpperCase();
            Map<String, Object> corpoReq = m.equals("POST") ? Json.lerObjeto(lerCorpo(ex)) : new HashMap<String, Object>();

            if (caminho.equals("/api/ping")) json(ex, 200, Json.obj("ok", true, "nome", u.getNome(), "versao", "1.3",
                    "banco", db.isNuvem() ? "nuvem" : "local"));
            else if (caminho.equals("/api/resumo")) json(ex, 200, resumo(u));
            else if (caminho.equals("/api/agua") && m.equals("POST")) json(ex, 200, agua(u, corpoReq));
            else if (caminho.equals("/api/agua/semana")) json(ex, 200, aguaSemana(u));
            else if (caminho.equals("/api/alimentos")) json(ex, 200, alimentos());
            else if (caminho.equals("/api/refeicoes") && m.equals("GET")) json(ex, 200, refeicoes(u, q.get("data")));
            else if (caminho.equals("/api/refeicoes") && m.equals("POST")) json(ex, 200, novaRefeicao(u, corpoReq));
            else if (caminho.equals("/api/refeicoes/remover")) {
                alim.remover(u.getId(), (int) Json.num(corpoReq, "id", 0));
                json(ex, 200, Json.obj("ok", true));
            }
            else if (caminho.equals("/api/medidas") && m.equals("GET")) json(ex, 200, medidas(u));
            else if (caminho.equals("/api/medidas") && m.equals("POST")) json(ex, 200, novaMedida(u, corpoReq));
            else if (caminho.equals("/api/foto") && m.equals("POST")) json(ex, 200, foto(corpoReq));
            else if (caminho.equals("/api/imagem/corpo.png")) imagemCorpo(ex, u, q);
            else if (caminho.equals("/api/imagem/regua.png")) imagemRegua(ex, u, q);
            else json(ex, 404, Json.obj("erro", "Endereço não encontrado: " + caminho));
        } catch (IllegalArgumentException e) {
            json(ex, 400, Json.obj("erro", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            json(ex, 500, Json.obj("erro", String.valueOf(e.getMessage())));
        } finally {
            ex.close();
        }
    }

    private Map<String, Object> resumo(Usuario u) throws Exception {
        List<Medida> ms = corpo.listar(u.getId());
        Medida ult = ReguaProgresso.ultimaComPeso(ms);
        double peso = u.pesoReferencia(ult);
        LocalDate hoje = LocalDate.now();
        Map<LocalDate, Integer> k = alim.kcalPorDia(u.getId(), hoje, hoje);
        LocalDate ultData = ms.isEmpty() ? null : ms.get(ms.size() - 1).getData();
        Map<String, Object> r = Json.obj(
                "nome", u.getNome(),
                "copoMl", u.getCopoMl(),
                "metaMl", u.getMetaMl(),
                "metaCopos", u.getMetaCopos(),
                "mlHoje", db.mlHoje(u),
                "coposHoje", db.coposHoje(u.getId()),
                "intervaloMinutos", u.getIntervaloMinutos(),
                "horaInicio", u.getHoraInicio().format(HM),
                "horaFim", u.getHoraFim().format(HM),
                "lembretesAtivos", u.isAtivo(),
                "metaKcal", u.getMetaKcalEfetiva(peso),
                "kcalHoje", k.containsKey(hoje) ? k.get(hoje) : 0,
                "peso", peso,
                "pesoMeta", u.getPesoMetaKg(),
                "altura", u.getAlturaCm(),
                "resumoPeso", ReguaProgresso.resumoPeso(u, ms),
                "ultimaMedida", ultData == null ? null : ultData.toString(),
                "lembreteMedidasDias", u.getLembreteMedidasDias(),
                "proximaMedida", ultData == null || u.getLembreteMedidasDias() <= 0 ? null
                        : ultData.plusDays(u.getLembreteMedidasDias()).toString());
        if (ult != null && u.getAlturaCm() > 0) {
            double imc = ult.imc(u.getAlturaCm());
            r.put("imc", Math.round(imc * 10) / 10.0);
            r.put("imcClasse", Medida.classificarImc(imc));
        }
        return r;
    }

    private Map<String, Object> agua(Usuario u, Map<String, Object> b) {
        String acao = Json.str(b, "acao", Database.ACAO_MANUAL).toUpperCase();
        if (!Arrays.asList(Database.ACAO_BEBEU, Database.ACAO_MANUAL, Database.ACAO_ADIOU, Database.ACAO_IGNOROU).contains(acao))
            throw new IllegalArgumentException("Ação inválida: " + acao);
        int ml = (int) Json.num(b, "ml", acao.equals(Database.ACAO_BEBEU) || acao.equals(Database.ACAO_MANUAL) ? u.getCopoMl() : 0);
        LocalDateTime quando = null;
        String qs = Json.str(b, "quando", null);
        if (qs != null && !qs.isEmpty()) {
            quando = LocalDateTime.parse(qs.length() > 19 ? qs.substring(0, 19) : qs);
            if (quando.isAfter(LocalDateTime.now().plusMinutes(5))) quando = LocalDateTime.now();
        }
        db.registrar(u.getId(), acao, ml, quando);
        aoMudar.run();
        return Json.obj("ok", true, "mlHoje", db.mlHoje(u), "coposHoje", db.coposHoje(u.getId()), "metaMl", u.getMetaMl());
    }

    private List<Object> aguaSemana(Usuario u) {
        List<Object> l = new ArrayList<Object>();
        for (Database.DiaConsumo d : db.consumoPorDia(u, LocalDate.now().minusDays(6), LocalDate.now()))
            l.add(Json.obj("dia", d.dia.toString(), "ml", d.ml, "copos", d.copos));
        return l;
    }

    private List<Object> alimentos() {
        List<Object> l = new ArrayList<Object>();
        for (Alimentos.Alimento a : Alimentos.todos()) l.add(Json.obj("nome", a.nome, "porcao", a.porcao, "kcal", a.kcal));
        return l;
    }

    private Map<String, Object> refeicoes(Usuario u, String data) throws Exception {
        LocalDate dia = data == null || data.isEmpty() ? LocalDate.now() : LocalDate.parse(data);
        List<Object> itens = new ArrayList<Object>();
        int total = 0;
        for (AlimentacaoDao.Item it : alim.itens(u.getId(), dia)) {
            itens.add(Json.obj("id", it.id, "refeicao", it.refeicao, "descricao", it.descricao, "porcao", it.porcao,
                    "quantidade", it.quantidade, "kcal", it.kcal));
            total += it.kcal;
        }
        Medida ult = ReguaProgresso.ultimaComPeso(corpo.listar(u.getId()));
        return Json.obj("data", dia.toString(), "itens", itens, "total", total,
                "meta", u.getMetaKcalEfetiva(u.pesoReferencia(ult)), "refeicoes", Arrays.asList(AlimentacaoDao.REFEICOES));
    }

    private Map<String, Object> novaRefeicao(Usuario u, Map<String, Object> b) throws Exception {
        AlimentacaoDao.Item it = new AlimentacaoDao.Item();
        String data = Json.str(b, "data", "");
        it.data = data.isEmpty() ? LocalDate.now() : LocalDate.parse(data);
        it.refeicao = Json.str(b, "refeicao", AlimentacaoDao.REFEICOES[0]);
        it.descricao = Json.str(b, "descricao", "").trim();
        if (it.descricao.isEmpty()) throw new IllegalArgumentException("Informe o alimento.");
        if (it.descricao.length() > 150) it.descricao = it.descricao.substring(0, 150);
        it.porcao = Json.str(b, "porcao", "");
        it.quantidade = Json.num(b, "quantidade", 1);
        it.kcal = (int) Math.round(Json.num(b, "kcal", 0));
        alim.adicionar(u.getId(), it);
        return Json.obj("ok", true);
    }

    private Map<String, Object> medidas(Usuario u) throws Exception {
        List<Object> campos = new ArrayList<Object>();
        for (int i = 0; i < Medida.COLUNAS.length; i++)
            campos.add(Json.obj("coluna", Medida.COLUNAS[i], "nome", Medida.NOMES[i], "comoMedir", Medida.COMO_MEDIR[i]));
        List<Medida> ms = corpo.listar(u.getId());
        List<Object> lista = new ArrayList<Object>();
        for (int k = Math.max(0, ms.size() - 30); k < ms.size(); k++) {
            Medida m = ms.get(k);
            Map<String, Object> v = new LinkedHashMap<String, Object>();
            for (int i = 0; i < Medida.COLUNAS.length; i++) v.put(Medida.COLUNAS[i], m.get(i));
            lista.add(Json.obj("id", m.getId(), "data", m.getData().toString(), "peso", m.getPeso(), "valores", v,
                    "observacao", m.getObservacao()));
        }
        return Json.obj("campos", campos, "medidas", lista);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> novaMedida(Usuario u, Map<String, Object> b) throws Exception {
        Medida m = new Medida();
        String data = Json.str(b, "data", "");
        m.setData(data.isEmpty() ? LocalDate.now() : LocalDate.parse(data));
        m.setPeso(Math.max(0, Json.num(b, "peso", 0)));
        Object vals = b.get("valores");
        if (vals instanceof Map) {
            Map<String, Object> v = (Map<String, Object>) vals;
            for (int i = 0; i < Medida.COLUNAS.length; i++) m.set(i, Math.max(0, Json.num(v, Medida.COLUNAS[i], 0)));
        }
        m.setObservacao(Json.str(b, "observacao", ""));
        corpo.salvar(u.getId(), m);
        if (m.getPeso() > 0 && !m.getData().isBefore(LocalDate.now().minusDays(1))) {
            u.setPesoKg(m.getPeso());
            db.salvarUsuario(u);
        }
        aoMudar.run();
        return Json.obj("ok", true, "id", m.getId());
    }

    private Map<String, Object> foto(Map<String, Object> b) throws Exception {
        int medidaId = (int) Json.num(b, "medidaId", 0);
        if (medidaId <= 0) throw new IllegalArgumentException("Informe a medição da foto.");
        String tipo = Json.str(b, "tipo", "FRENTE").toUpperCase();
        if (!Arrays.asList(CorpoDao.TIPOS_FOTO).contains(tipo)) tipo = "OUTRA";
        byte[] dados = Base64.getMimeDecoder().decode(Json.str(b, "imagem", ""));
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(dados));
        if (img == null) throw new IllegalArgumentException("Imagem inválida.");
        corpo.adicionarFoto(medidaId, tipo, TelaCorpo.jpeg(TelaCorpo.reduzir(img, 1280)));
        return Json.obj("ok", true);
    }

    private void imagemCorpo(HttpExchange ex, Usuario u, Map<String, String> q) throws Exception {
        List<Medida> ms = corpo.listar(u.getId());
        if (ms.isEmpty()) { json(ex, 404, Json.obj("erro", "Sem medições.")); return; }
        int w = limitar(q.get("w"), 600, 300, 1400), h = limitar(q.get("h"), 700, 350, 1600);
        Medida at = ms.get(ms.size() - 1), comp = ms.size() >= 2 ? ms.get(0) : null;
        BufferedImage img = DesenhoCorpo.imagem(u, at, comp,
                comp == null ? "" : "primeira medição (" + comp.getData().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")", w, h);
        png(ex, img);
    }

    private void imagemRegua(HttpExchange ex, Usuario u, Map<String, String> q) throws Exception {
        int w = limitar(q.get("w"), 700, 360, 1400);
        png(ex, ReguaProgresso.imagem(u, corpo.listar(u.getId()), w, 780));
    }

    // ------------------------------------------------------------------ utilidades HTTP

    private static int limitar(String s, int padrao, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(s))); } catch (Exception e) { return padrao; }
    }

    private static Map<String, String> parametros(String raw) {
        Map<String, String> m = new HashMap<String, String>();
        if (raw == null) return m;
        for (String par : raw.split("&")) {
            int i = par.indexOf('=');
            try {
                if (i > 0) m.put(URLDecoder.decode(par.substring(0, i), "UTF-8"), URLDecoder.decode(par.substring(i + 1), "UTF-8"));
            } catch (Exception ignored) { }
        }
        return m;
    }

    private static String lerCorpo(HttpExchange ex) throws IOException {
        InputStream in = ex.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n, total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > 20 * 1024 * 1024) throw new IllegalArgumentException("Envio muito grande (máx. 20 MB).");
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void json(HttpExchange ex, int status, Object o) throws IOException {
        enviar(ex, status, "application/json; charset=utf-8", Json.escrever(o).getBytes(StandardCharsets.UTF_8));
    }

    private static void texto(HttpExchange ex, int status, String s) throws IOException {
        enviar(ex, status, "text/plain; charset=utf-8", s.getBytes(StandardCharsets.UTF_8));
    }

    private static void png(HttpExchange ex, BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        enviar(ex, 200, "image/png", out.toByteArray());
    }

    private static void enviar(HttpExchange ex, int status, String tipo, byte[] dados) throws IOException {
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, dados.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(dados); }
    }
}
