package br.com.avisodeagua;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

/**
 * Faz o programa iniciar junto com o Windows criando o arquivo
 * AvisodeAgua.vbs na pasta "Inicializar" do usuário. Compatível com Java 8.
 */
public final class InicializacaoWindows {

    private InicializacaoWindows() { }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static File arquivoInicializar() {
        String appData = System.getenv("APPDATA");
        if (appData == null) return null;
        return new File(appData, "Microsoft\\Windows\\Start Menu\\Programs\\Startup\\AvisodeAgua.vbs");
    }

    /** Caminho do AvisodeAgua.jar em execução (ou null se rodando pela IDE). */
    private static String caminhoJar() {
        try {
            File f = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() && f.getName().toLowerCase().endsWith(".jar") ? f.getAbsolutePath() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void configurar(boolean ativar) {
        if (!isWindows()) return;
        File vbs = arquivoInicializar();
        if (vbs == null) return;
        try {
            if (!ativar) {
                if (vbs.exists()) vbs.delete();
                return;
            }
            String jar = caminhoJar();
            if (jar == null) return;
            String javaw = System.getProperty("java.home") + "\\bin\\javaw.exe";
            String cmd = "\"\"" + javaw + "\"\" -Dfile.encoding=UTF-8 -jar \"\"" + jar + "\"\" --oculto";
            vbs.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(vbs), Charset.forName("windows-1252"))) {
                w.write("' Aviso de Agua - inicia escondido na bandeja\r\n");
                w.write("Set s = CreateObject(\"WScript.Shell\")\r\n");
                w.write("s.Run \"" + cmd + "\", 0, False\r\n");
            }
        } catch (Exception e) {
            System.err.println("Não foi possível configurar a inicialização: " + e.getMessage());
        }
    }
}
