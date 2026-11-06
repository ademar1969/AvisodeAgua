package br.com.avisodeagua;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Mostra os dados para conectar o aplicativo Android (endereço e código) e liga/desliga a API. */
public class TelaCelular extends JDialog {

    private final ServidorCelular servidor;
    private final JCheckBox chkAtivo = new JCheckBox("Permitir que o aplicativo do celular se conecte a este computador");
    private final JSpinner campoPorta = new JSpinner(new SpinnerNumberModel(ServidorCelular.PORTA_PADRAO, 1024, 65535, 1));
    private final JLabel lblEndereco = new JLabel();
    private final JLabel lblCodigo = new JLabel();
    private final JLabel lblStatus = new JLabel();

    public TelaCelular(Window dono, ServidorCelular servidor) {
        super(dono, "Aviso de Água - Aplicativo do celular", ModalityType.APPLICATION_MODAL);
        this.servidor = servidor;
        setIconImage(Icones.gota(64));

        campoPorta.setEditor(new JSpinner.NumberEditor(campoPorta, "#"));
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 10, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 4, 5, 6);
        c.gridwidth = 2;
        int y = 0;
        JLabel t = new JLabel("Conectar o app Android");
        t.setFont(TelaAlerta.fonte(Font.BOLD, 18));
        t.setForeground(new Color(0x0B4F75));
        c.gridx = 0; c.gridy = y++; p.add(t, c);
        c.gridy = y++; p.add(chkAtivo, c);
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = y; p.add(new JLabel("Porta:"), c);
        c.gridx = 1; c.gridy = y++; p.add(campoPorta, c);

        lblEndereco.setFont(TelaAlerta.fonte(Font.BOLD, 18));
        lblCodigo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        lblCodigo.setForeground(new Color(0x1E90DC));
        c.gridx = 0; c.gridy = y; p.add(new JLabel("Endereço do servidor:"), c);
        c.gridx = 1; c.gridy = y++; p.add(lblEndereco, c);
        c.gridx = 0; c.gridy = y; p.add(new JLabel("Código de acesso:"), c);
        c.gridx = 1; c.gridy = y++; p.add(lblCodigo, c);
        c.gridx = 0; c.gridy = y++; c.gridwidth = 2; p.add(lblStatus, c);

        JLabel passos = new JLabel("<html><div style='width:440px'><b>No celular:</b><ol>"
                + "<li>Instale o app <b>Aviso de Água</b> (projeto Android na pasta <i>android</i>).</li>"
                + "<li>Conecte o celular no <b>mesmo Wi-Fi</b> deste computador.</li>"
                + "<li>No app, toque em <b>Configurar</b> e digite o endereço e o código acima.</li>"
                + "<li>Toque em <b>Testar conexão</b>. Se o Windows perguntar, permita o acesso do Java na rede.</li></ol>"
                + "<small>Os lembretes de água funcionam no celular mesmo sem o PC; os copos marcados sem conexão "
                + "são enviados quando o celular voltar a alcançar o computador. Com o banco em nuvem os dados ficam "
                + "na nuvem, mas o app conversa com este programa (o PC precisa estar ligado).</small></div></html>");
        c.gridy = y; c.insets = new Insets(12, 4, 4, 6); p.add(passos, c);

        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        JButton novo = new JButton("Gerar novo código");
        JButton aplicar = new JButton("Aplicar");
        JButton fechar = new JButton("Fechar");
        novo.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Gerar um novo código? O celular precisará ser configurado de novo.",
                    "Código", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { servidor.novoToken(); atualizar(); }
        });
        aplicar.addActionListener(e -> aplicar());
        fechar.addActionListener(e -> dispose());
        b.add(novo);
        b.add(aplicar);
        b.add(fechar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(p, BorderLayout.CENTER);
        raiz.add(b, BorderLayout.SOUTH);
        setContentPane(raiz);
        chkAtivo.setSelected(servidor.ativoNaConfig());
        campoPorta.setValue(servidor.getPorta());
        atualizar();
        pack();
        setResizable(false);
        setLocationRelativeTo(dono);
    }

    private void aplicar() {
        try {
            servidor.configurar(chkAtivo.isSelected(), (Integer) campoPorta.getValue());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Não foi possível abrir a porta " + campoPorta.getValue() + ":\n" + e.getMessage()
                    + "\n\nTalvez outro programa esteja usando essa porta. Tente outra.", "Celular", JOptionPane.ERROR_MESSAGE);
        }
        atualizar();
    }

    private void atualizar() {
        List<String> ips = ServidorCelular.enderecosLocais();
        StringBuilder sb = new StringBuilder("<html>");
        if (ips.isEmpty()) sb.append("sem rede local");
        for (int i = 0; i < ips.size(); i++) {
            if (i > 0) sb.append("<br><small>ou </small>");
            sb.append(ips.get(i)).append(":").append(servidor.getPorta());
        }
        lblEndereco.setText(sb.append("</html>").toString());
        lblCodigo.setText(servidor.getToken());
        lblStatus.setText(servidor.rodando()
                ? "<html><span style='color:#2E9E5B'><b>Ligado:</b> aguardando o celular.</span></html>"
                : "<html><span style='color:#B86E00'><b>Desligado.</b> Marque a opção acima e clique em Aplicar.</span></html>");
    }
}
