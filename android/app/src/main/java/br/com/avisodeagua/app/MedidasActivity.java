package br.com.avisodeagua.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Nova medição do corpo (peso + 12 medidas) com fotos da câmera ou da galeria. */
public class MedidasActivity extends Activity {

    private static final int REQ_CAMERA = 21, REQ_GALERIA = 22, REQ_PERM = 23;
    private static final String[] TIPOS = {"FRENTE", "LADO", "COSTAS", "OUTRA"};
    private static final String[] NOMES_TIPO = {"Frente", "Lado", "Costas", "Outra"};

    private LocalDate data = LocalDate.now();
    private Button btData;
    private EditText campoPeso, campoObs;
    private final List<String> colunas = new ArrayList<>();
    private final List<EditText> campos = new ArrayList<>();
    private LinearLayout form, miniaturas;
    private Spinner spTipo;
    private Uri fotoCamera;
    private final List<Uri> fotos = new ArrayList<>();
    private final List<String> tiposFotos = new ArrayList<>();
    private Button btSalvar;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout p = Ui.pagina(this);
        LinearLayout c = Ui.cartao(this);
        c.addView(Ui.texto(this, "Nova medição", 20, Ui.AZUL_ESCURO, true));
        c.addView(Ui.texto(this, "Medidas em centímetros, com fita métrica. Toque no nome da medida para ver como medir. "
                + "Deixe em branco o que não quiser medir.", 13, Ui.SUAVE, false), Ui.largura(this, 4));
        btData = Ui.botao(this, "", false);
        btData.setOnClickListener(v -> new DatePickerDialog(this, (dp, a, m, d) -> { data = LocalDate.of(a, m + 1, d); mostrarData(); },
                data.getYear(), data.getMonthValue() - 1, data.getDayOfMonth()).show());
        c.addView(btData, Ui.largura(this, 10));
        mostrarData();
        form = Ui.coluna(this);
        campoPeso = Ui.campo(this, "Peso (kg)", true);
        form.addView(rotulo("Peso (kg)", "Mesma balança, sem sapatos, de preferência ao acordar."));
        form.addView(campoPeso);
        c.addView(form, Ui.largura(this, 8));
        c.addView(Ui.texto(this, "Carregando as medidas...", 13, Ui.SUAVE, false));
        p.addView(c);

        LinearLayout f = Ui.cartao(this);
        f.addView(Ui.texto(this, "Fotos do corpo (opcional)", 16, Ui.TEXTO, true));
        spTipo = new Spinner(this);
        spTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, NOMES_TIPO));
        f.addView(spTipo, Ui.largura(this, 6));
        LinearLayout bt = Ui.linha(this);
        Button cam = Ui.botao(this, "📷 Tirar foto", false);
        Button gal = Ui.botao(this, "🖼️ Galeria", false);
        cam.setOnClickListener(v -> tirarFoto());
        gal.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            startActivityForResult(Intent.createChooser(i, "Escolha a foto"), REQ_GALERIA);
        });
        LinearLayout.LayoutParams l1 = Ui.peso(1);
        l1.rightMargin = Ui.dp(this, 6);
        bt.addView(cam, l1);
        bt.addView(gal, Ui.peso(1));
        f.addView(bt, Ui.largura(this, 8));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        miniaturas = Ui.linha(this);
        hs.addView(miniaturas);
        f.addView(hs, Ui.largura(this, 8));
        p.addView(f);

        LinearLayout o = Ui.cartao(this);
        o.addView(Ui.texto(this, "Observação", 14, Ui.SUAVE, true));
        campoObs = Ui.campo(this, "ex.: comecei a caminhar", false);
        campoObs.setSingleLine(false);
        o.addView(campoObs);
        p.addView(o);

        btSalvar = Ui.botao(this, "Salvar medição", true);
        btSalvar.setOnClickListener(v -> salvar());
        p.addView(btSalvar, Ui.largura(this, 4));
        setContentView(Ui.rolagem(this, p));
        carregarCampos(c);
    }

    private void mostrarData() {
        btData.setText("📅 " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private TextView rotulo(String nome, final String comoMedir) {
        TextView t = Ui.texto(this, nome + "  ⓘ", 14, Ui.TEXTO, true);
        t.setPadding(0, Ui.dp(this, 10), 0, 0);
        t.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(nome).setMessage(comoMedir)
                .setPositiveButton("OK", null).show());
        return t;
    }

    private void carregarCampos(final LinearLayout cartao) {
        Api.get(this, "/api/medidas", new Api.Resposta() {
            @Override public void ok(String corpo) {
                try {
                    cartao.removeViewAt(cartao.getChildCount() - 1); // "Carregando..."
                    JSONObject r = new JSONObject(corpo);
                    JSONArray cs = r.getJSONArray("campos");
                    JSONArray ms = r.getJSONArray("medidas");
                    JSONObject ult = ms.length() > 0 ? ms.getJSONObject(ms.length() - 1) : null;
                    if (ult != null && ult.optDouble("peso") > 0) campoPeso.setHint("anterior: " + Ui.fmt(ult.optDouble("peso")));
                    for (int i = 0; i < cs.length(); i++) {
                        JSONObject cp = cs.getJSONObject(i);
                        String col = cp.getString("coluna");
                        form.addView(rotulo(cp.getString("nome") + " (cm)", cp.optString("comoMedir")));
                        EditText e = Ui.campo(MedidasActivity.this, "cm", true);
                        double ant = 0;
                        for (int k = ms.length() - 1; k >= 0 && ant <= 0; k--)
                            ant = ms.getJSONObject(k).getJSONObject("valores").optDouble(col, 0);
                        if (ant > 0) e.setHint("anterior: " + Ui.fmt(ant));
                        form.addView(e);
                        colunas.add(col);
                        campos.add(e);
                    }
                } catch (Exception e) {
                    Ui.aviso(MedidasActivity.this, "Erro ao carregar: " + e.getMessage());
                }
            }
            @Override public void erro(String m) {
                Ui.aviso(MedidasActivity.this, m);
            }
        });
    }

    // ---------------------------------------------------------------- fotos

    private void tirarFoto() {
        if (Build.VERSION.SDK_INT <= 28
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM);
            return;
        }
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, "medida_" + System.currentTimeMillis() + ".jpg");
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            fotoCamera = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, fotoCamera);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) {
            Ui.aviso(this, "Não foi possível abrir a câmera: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_PERM && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) tirarFoto();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent dados) {
        super.onActivityResult(req, res, dados);
        if (res != RESULT_OK) return;
        Uri u = req == REQ_CAMERA ? fotoCamera : (dados != null ? dados.getData() : null);
        if (u == null) return;
        fotos.add(u);
        tiposFotos.add(TIPOS[spTipo.getSelectedItemPosition()]);
        ImageView iv = new ImageView(this);
        Bitmap mini = carregar(u, 300);
        if (mini != null) iv.setImageBitmap(mini);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout box = Ui.coluna(this);
        box.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 96), Ui.dp(this, 128)));
        box.addView(Ui.texto(this, NOMES_TIPO[spTipo.getSelectedItemPosition()], 12, Ui.SUAVE, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        miniaturas.addView(box, lp);
    }

    /** Lê a imagem reduzida e já na posição certa (corrige a rotação da câmera). */
    private Bitmap carregar(Uri u, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(u)) { BitmapFactory.decodeStream(in, null, o); }
            int amostra = 1;
            while (Math.max(o.outWidth, o.outHeight) / (amostra * 2) >= max) amostra *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = amostra;
            Bitmap bmp;
            try (InputStream in = getContentResolver().openInputStream(u)) { bmp = BitmapFactory.decodeStream(in, null, o2); }
            if (bmp == null) return null;
            int graus = 0;
            try (InputStream in = getContentResolver().openInputStream(u)) {
                int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                graus = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90 : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                        : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            } catch (Exception ignored) { }
            float esc = Math.min(1f, max / (float) Math.max(bmp.getWidth(), bmp.getHeight()));
            Matrix m = new Matrix();
            m.postScale(esc, esc);
            if (graus != 0) m.postRotate(graus);
            return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- salvar

    private void salvar() {
        try {
            JSONObject o = new JSONObject();
            o.put("data", data.toString());
            double peso = Ui.ler(campoPeso.getText().toString());
            if (peso < 0 || peso > 400) { Ui.aviso(this, "Peso inválido."); return; }
            o.put("peso", peso);
            JSONObject v = new JSONObject();
            boolean algum = peso > 0;
            for (int i = 0; i < campos.size(); i++) {
                double x = Ui.ler(campos.get(i).getText().toString());
                if (x < 0 || x > 300) { Ui.aviso(this, "Valor inválido numa das medidas."); return; }
                if (x > 0) { v.put(colunas.get(i), x); algum = true; }
            }
            if (!algum) { Ui.aviso(this, "Informe pelo menos o peso ou uma medida."); return; }
            o.put("valores", v);
            o.put("observacao", campoObs.getText().toString().trim());
            btSalvar.setEnabled(false);
            btSalvar.setText("Enviando...");
            Api.post(this, "/api/medidas", o, new Api.Resposta() {
                @Override public void ok(String corpo) {
                    try {
                        int id = new JSONObject(corpo).getInt("id");
                        enviarFotos(id);
                    } catch (Exception e) {
                        terminar("Medição salva.");
                    }
                }
                @Override public void erro(String m) {
                    btSalvar.setEnabled(true);
                    btSalvar.setText("Salvar medição");
                    Ui.aviso(MedidasActivity.this, m);
                }
            });
        } catch (Exception e) {
            Ui.aviso(this, e.getMessage());
        }
    }

    private void enviarFotos(final int medidaId) {
        if (fotos.isEmpty()) { terminar("Medição salva no computador!"); return; }
        btSalvar.setText("Enviando fotos...");
        new Thread(() -> {
            int ok = 0;
            String erro = null;
            for (int i = 0; i < fotos.size(); i++) {
                try {
                    Bitmap bmp = carregar(fotos.get(i), 1600);
                    if (bmp == null) continue;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    JSONObject f = new JSONObject();
                    f.put("medidaId", medidaId);
                    f.put("tipo", tiposFotos.get(i));
                    f.put("imagem", Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
                    Api.postSinc(this, "/api/foto", f);
                    ok++;
                } catch (Exception e) {
                    erro = e.getMessage();
                }
            }
            final String msg = "Medição salva com " + ok + " foto(s)." + (erro != null ? " Erro em alguma foto: " + erro : "");
            runOnUiThread(() -> terminar(msg));
        }).start();
    }

    private void terminar(String msg) {
        Ui.aviso(this, msg);
        startActivity(new Intent(this, ProgressoActivity.class));
        finish();
    }
}
