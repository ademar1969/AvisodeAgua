package br.com.avisodeagua;

import java.time.LocalDate;
import java.util.Locale;

/** Uma medição do corpo (todas as medidas em centímetros, peso em kg). */
public class Medida {

    /** Medidas cadastradas: coluna no banco, nome na tela e como medir. */
    public static final String[] COLUNAS = {
            "cabeca", "pescoco", "torax", "cintura", "barriga", "quadril",
            "braco", "antebraco", "mao", "coxa", "panturrilha", "pe"};
    public static final String[] NOMES = {
            "Cabeça", "Pescoço", "Tórax / peito", "Cintura", "Barriga (umbigo)", "Quadril",
            "Braço", "Antebraço", "Mão", "Coxa", "Panturrilha", "Pé"};
    public static final String[] COMO_MEDIR = {
            "Volta da cabeça, passando pela testa, acima das sobrancelhas.",
            "Volta do pescoço, logo abaixo do pomo de Adão.",
            "Volta do peito, na altura dos mamilos, braços relaxados.",
            "Parte mais fina do tronco, entre as costelas e o umbigo.",
            "Volta da barriga na altura do umbigo, sem encolher.",
            "Parte mais larga do quadril / bumbum, pés juntos.",
            "Meio do braço (entre ombro e cotovelo), relaxado.",
            "Parte mais grossa do antebraço, logo abaixo do cotovelo.",
            "Volta da palma da mão, sem o polegar.",
            "Parte mais grossa da coxa, logo abaixo do bumbum.",
            "Parte mais grossa da batata da perna.",
            "Comprimento do pé, do calcanhar ao dedão."};
    /** Valores típicos usados no desenho quando a medida não foi informada (homem / mulher). */
    private static final double[] PADRAO_M = {57, 39, 100, 88, 92, 100, 32, 28, 21, 56, 38, 26};
    private static final double[] PADRAO_F = {55, 33, 90, 75, 82, 100, 28, 24, 18, 56, 35, 24};

    public static final int CABECA = 0, PESCOCO = 1, TORAX = 2, CINTURA = 3, BARRIGA = 4, QUADRIL = 5,
            BRACO = 6, ANTEBRACO = 7, MAO = 8, COXA = 9, PANTURRILHA = 10, PE = 11;

    private int id;
    private LocalDate data = LocalDate.now();
    private double peso;                       // 0 = não informado
    private final double[] valores = new double[COLUNAS.length]; // 0 = não informado
    private String observacao = "";

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }
    public double get(int i) { return valores[i]; }
    public void set(int i, double v) { valores[i] = v; }
    public String getObservacao() { return observacao == null ? "" : observacao; }
    public void setObservacao(String o) { this.observacao = o; }

    /** Valor para o desenho: o medido, ou o típico se não foi informado. */
    public double valorDesenho(int i, boolean feminino) {
        return valores[i] > 0 ? valores[i] : (feminino ? PADRAO_F[i] : PADRAO_M[i]);
    }

    /** Valor genérico por índice: -1 = peso, 0..11 = medidas, 100 = IMC, 101 = % gordura. */
    public double valorIndicador(int indicador, Usuario u) {
        if (indicador == -1) return peso;
        if (indicador == 100) return imc(u.getAlturaCm());
        if (indicador == 101) return gorduraEstimada(u);
        return valores[indicador];
    }

    public double imc(double alturaCm) {
        if (peso <= 0 || alturaCm <= 0) return 0;
        double m = alturaCm / 100.0;
        return peso / (m * m);
    }

    public static String classificarImc(double imc) {
        if (imc <= 0) return "";
        if (imc < 18.5) return "abaixo do peso";
        if (imc < 25) return "peso normal";
        if (imc < 30) return "sobrepeso";
        if (imc < 35) return "obesidade grau I";
        if (imc < 40) return "obesidade grau II";
        return "obesidade grau III";
    }

    /** Relação cintura / quadril. */
    public double rcq() {
        return valores[CINTURA] > 0 && valores[QUADRIL] > 0 ? valores[CINTURA] / valores[QUADRIL] : 0;
    }

    /** % de gordura corporal estimada pelo método da Marinha dos EUA (aproximado). */
    public double gorduraEstimada(Usuario u) {
        double h = u.getAlturaCm(), pes = valores[PESCOCO];
        double cint = valores[BARRIGA] > 0 ? valores[BARRIGA] : valores[CINTURA];
        if (h <= 0 || pes <= 0 || cint <= 0) return 0;
        double g;
        if (u.isFeminino()) {
            double q = valores[QUADRIL];
            if (q <= 0 || cint + q - pes <= 0) return 0;
            g = 495 / (1.29579 - 0.35004 * Math.log10(cint + q - pes) + 0.22100 * Math.log10(h)) - 450;
        } else {
            if (cint - pes <= 0) return 0;
            g = 495 / (1.0324 - 0.19077 * Math.log10(cint - pes) + 0.15456 * Math.log10(h)) - 450;
        }
        return g > 2 && g < 70 ? g : 0;
    }

    public static String nomeIndicador(int indicador) {
        if (indicador == -1) return "Peso";
        if (indicador == 100) return "IMC";
        if (indicador == 101) return "% de gordura (estimada)";
        return NOMES[indicador];
    }

    public static String unidadeIndicador(int indicador) {
        if (indicador == -1) return "kg";
        if (indicador == 100) return "";
        if (indicador == 101) return "%";
        return "cm";
    }

    public static String fmt(double v) {
        return String.format(new Locale("pt", "BR"), "%.1f", v);
    }

    /** Converte texto digitado ("92,5" ou "92.5") em número; 0 se vazio ou inválido. */
    public static double ler(String s) {
        if (s == null) return 0;
        s = s.trim().replace(",", ".");
        if (s.isEmpty()) return 0;
        try { return Math.max(0, Double.parseDouble(s)); } catch (NumberFormatException e) { return -1; }
    }
}
