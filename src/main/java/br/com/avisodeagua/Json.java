package br.com.avisodeagua;

import java.util.*;

/** JSON mínimo (sem bibliotecas): converte Map/List/String/Number/Boolean em texto e vice-versa. */
public final class Json {

    private Json() { }

    // ------------------------------------------------------------------ escrita

    public static String escrever(Object o) {
        StringBuilder sb = new StringBuilder();
        escrever(sb, o);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void escrever(StringBuilder sb, Object o) {
        if (o == null) sb.append("null");
        else if (o instanceof String) texto(sb, (String) o);
        else if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append((long) d);
            else sb.append(d);
        } else if (o instanceof Number || o instanceof Boolean) sb.append(o);
        else if (o instanceof Map) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                texto(sb, e.getKey());
                sb.append(':');
                escrever(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof Collection) {
            sb.append('[');
            boolean primeiro = true;
            for (Object x : (Collection<Object>) o) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escrever(sb, x);
            }
            sb.append(']');
        } else texto(sb, o.toString());
    }

    private static void texto(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    /** Atalho para montar objetos: Json.obj("a", 1, "b", "x"). */
    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    // ------------------------------------------------------------------ leitura

    public static Object ler(String s) {
        Leitor l = new Leitor(s);
        l.espacos();
        Object o = l.valor();
        l.espacos();
        if (l.i != s.length()) throw new IllegalArgumentException("JSON inválido perto da posição " + l.i);
        return o;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> lerObjeto(String s) {
        Object o = ler(s == null || s.trim().isEmpty() ? "{}" : s);
        if (!(o instanceof Map)) throw new IllegalArgumentException("Esperava um objeto JSON.");
        return (Map<String, Object>) o;
    }

    public static String str(Map<String, Object> m, String k, String padrao) {
        Object v = m.get(k);
        return v == null ? padrao : v.toString();
    }

    public static double num(Map<String, Object> m, String k, double padrao) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) try { return Double.parseDouble(((String) v).replace(',', '.')); } catch (Exception ignored) { }
        return padrao;
    }

    private static final class Leitor {
        final String s;
        int i;
        Leitor(String s) { this.s = s; }

        void espacos() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        Object valor() {
            espacos();
            if (i >= s.length()) throw new IllegalArgumentException("JSON incompleto");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return lista();
            if (c == '"') return texto();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return numero();
        }

        Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            i++;
            espacos();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                espacos();
                String k = texto();
                espacos();
                if (s.charAt(i++) != ':') throw new IllegalArgumentException("Esperava ':'");
                m.put(k, valor());
                espacos();
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw new IllegalArgumentException("Esperava ',' ou '}'");
            }
        }

        List<Object> lista() {
            List<Object> l = new ArrayList<Object>();
            i++;
            espacos();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(valor());
                espacos();
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw new IllegalArgumentException("Esperava ',' ou ']'");
            }
        }

        String texto() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("Esperava texto");
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u': sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: sb.append(e);
                    }
                } else sb.append(c);
            }
        }

        Number numero() {
            int ini = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(ini, i);
            if (n.isEmpty()) throw new IllegalArgumentException("Valor JSON inválido na posição " + ini);
            return Double.parseDouble(n);
        }
    }
}
