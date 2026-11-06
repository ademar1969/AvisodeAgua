-- Aviso de Água - script do banco MySQL
-- O programa cria tudo sozinho na primeira execução; este script é opcional.
-- Conexão usada pelo programa: localhost:3306, usuário root, senha 100219

CREATE DATABASE IF NOT EXISTS avisodeagua CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE avisodeagua;

CREATE TABLE IF NOT EXISTS usuario (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    celular VARCHAR(20) NOT NULL,
    intervalo_minutos INT NOT NULL DEFAULT 60,
    hora_inicio TIME NOT NULL DEFAULT '08:00:00',
    hora_fim TIME NOT NULL DEFAULT '22:00:00',
    meta_copos INT NOT NULL DEFAULT 8,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    iniciar_windows BOOLEAN NOT NULL DEFAULT TRUE,
    peso_kg DECIMAL(5,1) NULL,
    copo_ml INT NOT NULL DEFAULT 250,
    email VARCHAR(150) NULL,
    sexo CHAR(1) NOT NULL DEFAULT 'M',
    data_nascimento DATE NULL,
    altura_cm DECIMAL(5,1) NULL,
    nivel_atividade INT NOT NULL DEFAULT 1,      -- 0 sedentário, 1 leve, 2 moderado, 3 intenso
    peso_meta_kg DECIMAL(5,1) NULL,
    meta_kcal INT NOT NULL DEFAULT 0,             -- 0 = calcular automaticamente
    lembrete_medidas_dias INT NOT NULL DEFAULT 15, -- 0 = não lembrar
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cada registro: BEBEU (no alerta), MANUAL (menu da bandeja), ADIOU (lembrar em 5 min) ou IGNOROU (fechou)
CREATE TABLE IF NOT EXISTS registro_consumo (
    id INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id INT NOT NULL,
    data_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acao VARCHAR(10) NOT NULL,
    quantidade_ml INT NULL,
    INDEX idx_usuario_data (usuario_id, data_hora),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Medições do corpo (cm; peso em kg)
CREATE TABLE IF NOT EXISTS medida_corpo (
    id INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id INT NOT NULL,
    data DATE NOT NULL,
    peso_kg DECIMAL(5,1) NULL,
    cabeca DECIMAL(5,1) NULL, pescoco DECIMAL(5,1) NULL, torax DECIMAL(5,1) NULL,
    cintura DECIMAL(5,1) NULL, barriga DECIMAL(5,1) NULL, quadril DECIMAL(5,1) NULL,
    braco DECIMAL(5,1) NULL, antebraco DECIMAL(5,1) NULL, mao DECIMAL(5,1) NULL,
    coxa DECIMAL(5,1) NULL, panturrilha DECIMAL(5,1) NULL, pe DECIMAL(5,1) NULL,
    observacao VARCHAR(500) NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_medida_usuario (usuario_id, data),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fotos do corpo (JPEG reduzido para 1280 px) ligadas a uma medição
CREATE TABLE IF NOT EXISTS foto_corpo (
    id INT PRIMARY KEY AUTO_INCREMENT,
    medida_id INT NOT NULL,
    tipo VARCHAR(10) NOT NULL,                    -- FRENTE, LADO, COSTAS, OUTRA
    imagem MEDIUMBLOB NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medida_id) REFERENCES medida_corpo(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Alimentação do dia (calorias aproximadas)
CREATE TABLE IF NOT EXISTS refeicao_item (
    id INT PRIMARY KEY AUTO_INCREMENT,
    usuario_id INT NOT NULL,
    data DATE NOT NULL,
    refeicao VARCHAR(30) NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    porcao VARCHAR(60) NULL,
    quantidade DECIMAL(6,2) NOT NULL DEFAULT 1,
    kcal INT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refeicao_usuario (usuario_id, data),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Servidor de e-mail para envio dos relatórios
CREATE TABLE IF NOT EXISTS config_email (
    id INT PRIMARY KEY,
    servidor VARCHAR(120), porta INT, seguranca VARCHAR(10),
    usuario VARCHAR(150), senha VARCHAR(200), remetente VARCHAR(150),
    destinatarios VARCHAR(500), enviar_fotos BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Consultas úteis
-- Evolução do peso e da cintura:
-- SELECT data, peso_kg, cintura, barriga FROM medida_corpo ORDER BY data;
-- Calorias por dia:
-- SELECT data, SUM(kcal) kcal FROM refeicao_item GROUP BY data ORDER BY data DESC;
-- Água por dia:
-- SELECT DATE(r.data_hora) dia, COUNT(*) copos, SUM(COALESCE(r.quantidade_ml, u.copo_ml)) ml,
--        u.meta_copos * u.copo_ml meta_ml
--   FROM registro_consumo r JOIN usuario u ON u.id = r.usuario_id
--  WHERE r.acao IN ('BEBEU','MANUAL') GROUP BY DATE(r.data_hora), u.meta_copos, u.copo_ml ORDER BY dia DESC;
