# 💧 Aviso de Água

Programa para Windows (Java 8) que fica rodando em segundo plano e lembra de **beber água**,
com tela de alerta de uma cachoeira, além de **acompanhamento de dieta**: medidas do corpo com desenho
gerado pelas medidas, fotos de antes e depois, régua de progresso, alimentação do dia com calorias
aproximadas, relatórios, gráficos e envio por e-mail. Inclui um **aplicativo Android**.

## Funcionalidades
- Lembretes de água por intervalo e horário, ícone na bandeja do Windows, meta pelo peso (35 ml/kg)
- Relatórios e gráficos de consumo de água (exporta CSV / imprime)
- Medidas do corpo (12 medidas + peso), desenho aproximado do corpo, IMC, % de gordura estimada
- Fotos do corpo e comparação antes/depois
- Régua de progresso do peso até a meta, com previsão
- Alimentação do dia com ~100 alimentos e calorias aproximadas
- Relatório de acompanhamento por e-mail (SMTP: Gmail, Outlook, Yahoo...)
- Banco de dados **local** (MariaDB/MySQL) ou **em nuvem**
- App **Android** (pasta `android`) conectado ao programa do PC pelo Wi-Fi

## Instalação no Windows
Pasta [`Instalador`](Instalador) → `instalar.bat` (instala Java 8 e MariaDB se precisar).
Detalhes em [LEIAME.md](LEIAME.md).

## App Android
Abra a pasta [`android`](android) no Android Studio. Veja [android/LEIA-ME.md](android/LEIA-ME.md).

## Tecnologias
Java 8 (Swing, Java2D, HttpServer embutido), MySQL/MariaDB (JDBC), Android SDK (Java, sem bibliotecas externas).

> Valores de calorias e % de gordura são estimativas para acompanhamento pessoal e não substituem
> a orientação de nutricionista ou médico.
