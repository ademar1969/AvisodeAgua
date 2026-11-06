package br.com.avisodeagua;

import java.time.LocalTime;

/** Dados do usuário e da configuração dos lembretes. */
public class Usuario {
    private int id = 1;
    private String nome = "";
    private String celular = "";
    private int intervaloMinutos = 60;
    private LocalTime horaInicio = LocalTime.of(8, 0);
    private LocalTime horaFim = LocalTime.of(22, 0);
    private int metaCopos = 8;
    private boolean ativo = true;
    private boolean iniciarComWindows = true;
    private double pesoKg = 0;      // 0 = não informado
    private int copoMl = 250;       // tamanho do copo

    /** ml de água recomendados por kg de peso por dia. */
    public static final int ML_POR_KG = 35;

    /** Meta diária em ml (copos x tamanho do copo). */
    public int getMetaMl() { return metaCopos * copoMl; }

    /** Meta recomendada pelo peso (35 ml/kg), ou 0 se o peso não foi informado. */
    public int getMetaRecomendadaMl() { return pesoKg > 0 ? (int) Math.round(pesoKg * ML_POR_KG) : 0; }

    /** Copos necessários para a meta recomendada. */
    public int getCoposRecomendados() {
        int r = getMetaRecomendadaMl();
        return r > 0 ? (int) Math.ceil(r / (double) Math.max(1, copoMl)) : 0;
    }

    /** Formata ml como "2,0 L" ou "750 ml". */
    public static String formatarMl(int ml) {
        if (ml >= 1000) {
            String v = String.format(new java.util.Locale("pt", "BR"), "%.2f", ml / 1000.0);
            if (v.endsWith("0")) v = v.substring(0, v.length() - 1); // 2,50 -> 2,5
            return v + " L";
        }
        return ml + " ml";
    }

    public boolean isCadastrado() {
        return nome != null && !nome.trim().isEmpty();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public int getIntervaloMinutos() { return intervaloMinutos; }
    public void setIntervaloMinutos(int intervaloMinutos) { this.intervaloMinutos = intervaloMinutos; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public int getMetaCopos() { return metaCopos; }
    public void setMetaCopos(int metaCopos) { this.metaCopos = metaCopos; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public double getPesoKg() { return pesoKg; }
    public void setPesoKg(double pesoKg) { this.pesoKg = pesoKg; }
    public int getCopoMl() { return copoMl; }
    public void setCopoMl(int copoMl) { this.copoMl = copoMl; }
    public boolean isIniciarComWindows() { return iniciarComWindows; }
    public void setIniciarComWindows(boolean iniciarComWindows) { this.iniciarComWindows = iniciarComWindows; }

    // ------------------------------------------------------------ perfil corporal e dieta
    public static final String[] NIVEIS_ATIVIDADE = {
            "Sedentário (pouco ou nenhum exercício)",
            "Leve (exercício 1 a 3 vezes por semana)",
            "Moderado (exercício 3 a 5 vezes por semana)",
            "Intenso (exercício 6 a 7 vezes por semana)"};
    private static final double[] FATOR_ATIVIDADE = {1.2, 1.375, 1.55, 1.725};

    private String email = "";
    private String sexo = "M";                  // M ou F
    private java.time.LocalDate dataNascimento; // pode ser null
    private double alturaCm = 0;
    private int nivelAtividade = 1;
    private double pesoMetaKg = 0;              // 0 = sem meta
    private int metaKcal = 0;                   // 0 = calcular automaticamente
    private int lembreteMedidasDias = 15;       // 0 = não lembrar

    public String getEmail() { return email == null ? "" : email; }
    public void setEmail(String email) { this.email = email; }
    public String getSexo() { return sexo == null ? "M" : sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public boolean isFeminino() { return "F".equalsIgnoreCase(getSexo()); }
    public java.time.LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(java.time.LocalDate d) { this.dataNascimento = d; }
    public double getAlturaCm() { return alturaCm; }
    public void setAlturaCm(double alturaCm) { this.alturaCm = alturaCm; }
    public int getNivelAtividade() { return nivelAtividade; }
    public void setNivelAtividade(int n) { this.nivelAtividade = Math.max(0, Math.min(3, n)); }
    public double getPesoMetaKg() { return pesoMetaKg; }
    public void setPesoMetaKg(double pesoMetaKg) { this.pesoMetaKg = pesoMetaKg; }
    public int getMetaKcal() { return metaKcal; }
    public void setMetaKcal(int metaKcal) { this.metaKcal = metaKcal; }
    public int getLembreteMedidasDias() { return lembreteMedidasDias; }
    public void setLembreteMedidasDias(int d) { this.lembreteMedidasDias = d; }

    public int getIdade() {
        if (dataNascimento == null) return 0;
        return java.time.Period.between(dataNascimento, java.time.LocalDate.now()).getYears();
    }

    /** Taxa metabólica basal aproximada (Mifflin-St Jeor), ou 0 se faltarem dados. */
    public int getTmb(double pesoAtual) {
        if (pesoAtual <= 0 || alturaCm <= 0 || getIdade() <= 0) return 0;
        double b = 10 * pesoAtual + 6.25 * alturaCm - 5 * getIdade() + (isFeminino() ? -161 : 5);
        return (int) Math.round(b);
    }

    /** Gasto calórico diário aproximado (TMB x fator de atividade). */
    public int getGastoDiario(double pesoAtual) {
        return (int) Math.round(getTmb(pesoAtual) * FATOR_ATIVIDADE[nivelAtividade]);
    }

    /** Meta sugerida: gasto diário - 500 kcal se quer emagrecer, + 300 se quer ganhar peso. */
    public int getMetaKcalSugerida(double pesoAtual) {
        int g = getGastoDiario(pesoAtual);
        if (g <= 0) return 0;
        int meta = g;
        if (pesoMetaKg > 0 && pesoMetaKg < pesoAtual - 0.5) meta = g - 500;
        else if (pesoMetaKg > 0 && pesoMetaKg > pesoAtual + 0.5) meta = g + 300;
        int minimo = isFeminino() ? 1200 : 1500;
        return Math.max(minimo, meta);
    }

    /** Meta de calorias em uso: a informada ou a sugerida. */
    public int getMetaKcalEfetiva(double pesoAtual) {
        return metaKcal > 0 ? metaKcal : getMetaKcalSugerida(pesoAtual);
    }

    /** Peso usado nos cálculos: o da última medição, ou o do cadastro de água. */
    public double pesoReferencia(Medida ultima) {
        if (ultima != null && ultima.getPeso() > 0) return ultima.getPeso();
        return pesoKg;
    }
}
