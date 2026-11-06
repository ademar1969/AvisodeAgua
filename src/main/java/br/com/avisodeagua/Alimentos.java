package br.com.avisodeagua;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabela de alimentos comuns com porção caseira e calorias APROXIMADAS
 * (referência média de tabelas nutricionais brasileiras; varia com o preparo e a marca).
 */
public final class Alimentos {

    public static class Alimento {
        public final String nome, porcao;
        public final int kcal;
        Alimento(String nome, String porcao, int kcal) { this.nome = nome; this.porcao = porcao; this.kcal = kcal; }
        @Override public String toString() { return nome + " — " + porcao + " (" + kcal + " kcal)"; }
    }

    private static final List<Alimento> LISTA = new ArrayList<Alimento>();

    private static void a(String nome, String porcao, int kcal) { LISTA.add(new Alimento(nome, porcao, kcal)); }

    static {
        // Pães, cereais e massas
        a("Pão francês", "1 unidade (50 g)", 150);
        a("Pão francês com manteiga", "1 unidade", 185);
        a("Pão de forma", "1 fatia (25 g)", 65);
        a("Pão integral", "1 fatia (25 g)", 60);
        a("Pão de queijo", "1 médio (40 g)", 120);
        a("Tapioca (só a goma)", "1 média (50 g de goma)", 170);
        a("Cuscuz de milho", "1 xícara (120 g)", 135);
        a("Bolo simples", "1 fatia (60 g)", 200);
        a("Biscoito cream cracker", "5 unidades (30 g)", 130);
        a("Biscoito recheado", "3 unidades (40 g)", 190);
        a("Aveia em flocos", "2 colheres de sopa (30 g)", 115);
        a("Granola", "2 colheres de sopa (30 g)", 130);
        a("Cereal matinal", "1 xícara (30 g)", 115);
        a("Arroz branco", "4 colheres de sopa (100 g)", 128);
        a("Arroz integral", "4 colheres de sopa (100 g)", 124);
        a("Feijão (carioca ou preto)", "1 concha (100 g)", 77);
        a("Macarrão cozido", "1 escumadeira cheia (110 g)", 170);
        a("Macarrão com molho de tomate", "1 prato raso (200 g)", 300);
        a("Lasanha", "1 pedaço (200 g)", 330);
        a("Farofa", "2 colheres de sopa (30 g)", 120);
        a("Batata cozida", "1 média (150 g)", 80);
        a("Purê de batata", "3 colheres de sopa (100 g)", 110);
        a("Batata frita", "1 porção média (100 g)", 280);
        a("Mandioca cozida", "1 pedaço (100 g)", 125);
        a("Polenta", "1 pedaço (100 g)", 90);
        // Carnes, ovos e proteínas
        a("Frango grelhado (peito)", "1 filé (100 g)", 160);
        a("Frango frito (coxa ou sobrecoxa)", "1 pedaço (90 g)", 220);
        a("Carne bovina grelhada", "1 bife (100 g)", 220);
        a("Carne moída refogada", "4 colheres de sopa (100 g)", 210);
        a("Bife à milanesa", "1 unidade (120 g)", 320);
        a("Carne de porco (lombo)", "1 fatia (100 g)", 210);
        a("Linguiça", "1 gomo (60 g)", 190);
        a("Peixe grelhado", "1 filé (120 g)", 155);
        a("Atum em lata", "2 colheres de sopa (50 g)", 70);
        a("Sardinha em lata", "2 unidades (60 g)", 130);
        a("Salsicha", "1 unidade (50 g)", 115);
        a("Hambúrguer de carne", "1 unidade (90 g)", 210);
        a("Ovo cozido", "1 unidade", 70);
        a("Ovo frito", "1 unidade", 100);
        a("Omelete", "2 ovos", 200);
        a("Strogonoff de frango", "4 colheres de sopa (150 g)", 240);
        a("Feijoada", "1 concha (200 g)", 340);
        a("Whey protein", "1 dose (30 g)", 120);
        // Leite e derivados
        a("Leite integral", "1 copo (200 ml)", 120);
        a("Leite desnatado", "1 copo (200 ml)", 70);
        a("Café com leite", "1 xícara (200 ml)", 90);
        a("Achocolatado com leite", "1 copo (200 ml)", 180);
        a("Iogurte natural", "1 pote (170 g)", 100);
        a("Iogurte adoçado / com frutas", "1 pote (170 g)", 150);
        a("Queijo muçarela", "1 fatia (20 g)", 65);
        a("Queijo minas frescal", "1 fatia (30 g)", 75);
        a("Requeijão", "1 colher de sopa (30 g)", 75);
        a("Manteiga", "1 colher de chá (5 g)", 36);
        a("Margarina", "1 colher de chá (5 g)", 30);
        a("Presunto", "2 fatias (30 g)", 30);
        a("Peito de peru", "2 fatias (30 g)", 30);
        // Frutas
        a("Banana", "1 média", 80);
        a("Maçã", "1 média", 70);
        a("Laranja", "1 média", 65);
        a("Mamão papaia", "1/2 unidade", 60);
        a("Melancia", "1 fatia (200 g)", 60);
        a("Melão", "1 fatia (150 g)", 45);
        a("Manga", "1 média", 120);
        a("Abacaxi", "1 fatia", 35);
        a("Uva", "1 cacho pequeno (100 g)", 70);
        a("Morango", "10 unidades", 30);
        a("Abacate", "2 colheres de sopa (45 g)", 75);
        a("Açaí com granola", "1 tigela (300 ml)", 450);
        // Verduras e legumes
        a("Salada de folhas", "1 prato de sobremesa", 15);
        a("Tomate", "1 unidade", 15);
        a("Cenoura crua", "2 colheres de sopa (30 g)", 10);
        a("Legumes cozidos", "1 xícara (100 g)", 40);
        a("Brócolis cozido", "1 xícara", 25);
        a("Sopa de legumes", "1 prato fundo (300 ml)", 150);
        // Gorduras e molhos
        a("Azeite ou óleo", "1 colher de sopa", 108);
        a("Maionese", "1 colher de sopa (12 g)", 80);
        a("Castanhas / amendoim", "1 punhado (30 g)", 170);
        a("Pasta de amendoim", "1 colher de sopa (15 g)", 90);
        // Lanches e salgados
        a("Pizza", "1 fatia", 270);
        a("Hambúrguer (lanche completo)", "1 unidade", 500);
        a("Cachorro-quente", "1 unidade", 300);
        a("Coxinha", "1 média", 220);
        a("Pastel frito", "1 unidade", 250);
        a("Esfiha", "1 unidade", 170);
        a("Salgado assado", "1 unidade", 200);
        a("Prato feito (arroz, feijão, carne e salada)", "1 prato", 600);
        // Doces
        a("Chocolate ao leite", "1 barra pequena (25 g)", 135);
        a("Sorvete", "1 bola (60 g)", 120);
        a("Brigadeiro", "1 unidade", 80);
        a("Pudim", "1 fatia (100 g)", 230);
        a("Açúcar", "1 colher de chá (5 g)", 20);
        a("Mel", "1 colher de sopa", 60);
        // Bebidas
        a("Água", "1 copo", 0);
        a("Café sem açúcar", "1 xícara (50 ml)", 2);
        a("Café com açúcar", "1 xícara (50 ml)", 35);
        a("Suco natural de laranja", "1 copo (200 ml)", 90);
        a("Suco de caixinha", "1 copo (200 ml)", 100);
        a("Refrigerante", "1 lata (350 ml)", 140);
        a("Refrigerante zero", "1 lata (350 ml)", 1);
        a("Água de coco", "1 copo (200 ml)", 40);
        a("Cerveja", "1 lata (350 ml)", 150);
        a("Vinho", "1 taça (150 ml)", 125);
    }

    private Alimentos() { }

    public static List<Alimento> todos() { return LISTA; }

    public static Alimento buscar(String nome) {
        for (Alimento al : LISTA) if (al.nome.equalsIgnoreCase(nome)) return al;
        return null;
    }
}
