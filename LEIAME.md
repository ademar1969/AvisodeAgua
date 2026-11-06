# Aviso de Água 💧

Programa Java que fica rodando em segundo plano (ícone de gota perto do relógio do Windows)
e, no horário programado, abre uma tela com a imagem de uma cachoeira avisando
**"Hora de tomar água!"**.

## O que ele faz
- Cadastro de **nome** e **celular**, salvo no MySQL.
- Você define **a cada quantos minutos** quer ser lembrado e o **horário** (ex.: das 08:00 às 22:00).
- Na hora, abre a tela da cachoeira com som de gotas e três botões:
  **Já bebi** (conta um copo), **Lembrar em 5 min** e **Fechar**.
- Mostra quantos copos você já bebeu hoje e avisa quando atingir a meta.
- Ícone na bandeja: clique com o botão direito para *Abrir cadastro*, *Tomar água agora*,
  *Pausar lembretes* ou *Sair*. Duplo clique abre o cadastro.
- Opção **Iniciar junto com o Windows** (já vem marcada): ele abre sozinho, escondido na bandeja.

## Meta de água
- Informe seu **peso** e o **tamanho do copo** no cadastro: o programa calcula a meta recomendada
  (**35 ml por kg**; ex.: 70 kg = 2,45 L = 10 copos de 250 ml). O botão **Usar recomendada** aplica a meta.
- A meta aparece no cadastro, no alerta (com barra de progresso e quanto falta) e nos relatórios.

## Relatórios e gráficos
Abra pelo botão **Relatórios e gráficos** do cadastro ou pelo menu do ícone de gota.
Períodos: 7, 15, 30, 90 dias, este mês e mês passado.
- **Resumo:** meta, progresso de hoje, média diária, dias com meta atingida, sequência de dias,
  total, melhor dia, horário em que mais bebe, % de alertas respondidos, adiados.
- **Consumo por dia:** barras com a linha da meta (azul forte = meta atingida).
- **Horários:** copos bebidos por hora do dia.
- **Respostas aos alertas:** gráfico de rosca (bebeu / adiou / ignorou).
- **Histórico:** tabela dia a dia, **Exportar CSV** (abre no Excel) e **Imprimir**.
- Pelo menu da gota, **Registrar copo bebido** conta um copo fora do horário do alerta.

## Medidas do corpo
Abra pelo botão **Medidas do corpo** (cadastro) ou pelo menu da gota.
- **Medição:** data, peso e 12 medidas em cm (cabeça, pescoço, tórax, cintura, barriga, quadril,
  braço, antebraço, mão, coxa, panturrilha, pé). Ao lado aparece o **desenho aproximado do corpo**
  gerado pelas medidas, com o contorno tracejado da medição anterior (ou da primeira) e a variação de cada medida.
  Botão **Como medir?** explica cada medida. O desenho pode ser salvo em PNG.
- **Régua de progresso:** peso inicial → atual → meta, % do caminho, ritmo por semana e previsão de quando chega na meta;
  régua do IMC; barras de quanto cada medida diminuiu/aumentou desde o início.
- **Gráficos:** evolução no tempo de peso, IMC, % de gordura estimada ou qualquer medida.
- **Fotos:** fotos de frente/lado/costas por medição (gravadas no MySQL), botão girar, e **comparar antes e depois**.
- **Histórico:** todas as medições, editar, excluir, exportar CSV.
- **Perfil e metas:** sexo, nascimento, altura, nível de atividade, peso meta, meta de calorias
  (automática pelo gasto diário estimado) e de quanto em quanto tempo **sugerir atualizar as medidas** (7, 15 ou 30 dias).
  Quando passa o prazo, o programa avisa e oferece abrir a tela de medidas.

## Alimentação do dia
Registre o que comeu em cada refeição. A tabela tem cerca de 100 alimentos comuns com porção caseira e
**calorias aproximadas** (digite para buscar). Também aceita qualquer alimento digitado, com as kcal que você informar.
Mostra total do dia x meta, total por refeição, água do dia e gráfico dos últimos 7 dias. Botão **Copiar do dia anterior**.

## E-mail
No cadastro, informe seu **e-mail**. Em **E-mail**, configure a conta que envia (Gmail: use uma *senha de app*;
Outlook/Microsoft 365 e Yahoo também têm configuração pronta) e para quem enviar (você, nutricionista...).
**Enviar relatório** manda: progresso do peso, IMC, gordura estimada, tabela de medidas (início, anterior, atual),
desenho do corpo, régua de progresso, água e calorias dos últimos 7 dias e as fotos da última medição.
> A senha do e-mail fica gravada na tabela `config_email` do seu MySQL local.

## Banco de dados
- MySQL em `localhost:3306`, usuário `root`, senha `100219`.
- O banco `avisodeagua` e as tabelas são criados automaticamente na primeira execução
  (o script `banco/criar_banco.sql` é opcional).
- Para mudar a conexão, edite `%APPDATA%\AvisodeAgua\config.properties`.

## Instalação (Windows)
Abra a pasta **`Instalador`** e dê dois cliques em **`instalar.bat`** (ele pede permissão de administrador).
O instalador:
1. Pergunta qual banco usar: **[1] instalar o MariaDB** neste computador (root / 100219),
   **[2] usar o MySQL/MariaDB que já existe**, ou **[3] banco em nuvem** (configurado depois no programa).
   Se já houver um banco rodando na porta 3306, ele sugere a opção 2 e não instala outro.
2. Instala o **Java 8** (Eclipse Temurin) se não houver Java no computador.
3. Instala o **MariaDB 11.4** sem perguntas (serviço "MariaDB", porta 3306, usuário root, senha 100219), se escolhido.
4. Copia o programa, baixa o driver do banco, cria os atalhos e libera a porta **8085** no firewall
   (só para a rede local) para o app do celular.

Precisa de internet para baixar Java/MariaDB. Para instalar **sem internet** em outros computadores,
rode **`baixar-pacotes.bat`** uma vez e copie a pasta `Instalador` inteira (os instaladores ficam em `pacotes`).

## Banco local ou em nuvem
No cadastro clique em **Banco** (ou menu da gota > *Banco de dados*):
- **Local:** MariaDB/MySQL neste computador (localhost:3306, root / 100219).
- **Nuvem:** informe servidor, porta, nome do banco, usuário, senha e marque **SSL**
  (Aiven, Railway, AWS RDS, Azure, Google Cloud SQL, MySQL da hospedagem...).
- **Testar conexão** antes de salvar. Ao trocar para um banco vazio, o programa oferece **copiar todos os seus dados**
  (cadastro, água, medidas, fotos, alimentação, e-mail) para o novo banco.
- A escolha fica em `%APPDATA%\AvisodeAgua\config.properties`.

## Aplicativo do celular (Android)
O projeto está na pasta **`android`** (abra no Android Studio — veja `android/LEIA-ME.md`).
O app conversa com este programa pela rede Wi-Fi: no cadastro clique em **Celular** para ver o
**endereço** e o **código de acesso** que devem ser digitados no app.
Os lembretes de água tocam no próprio celular; água, refeições, medidas e fotos vão para o mesmo banco do PC.

## Como gerar o instalador .exe (opcional, precisa de JDK 17+)
Requisito: **JDK 17 ou superior** (ex.: `winget install EclipseAdoptium.Temurin.21.JDK`).

1. Dê dois cliques em **`gerar-instalador.bat`**.
   Ele baixa o driver do MySQL, compila, gera o JAR e cria o instalador.
2. O instalador fica em **`dist\AvisodeAgua-1.0.0.exe`**. O Java vai embutido: quem instala não precisa ter Java.

O `.exe` gerado pelo `jpackage` exige o **WiX Toolset** (`winget install WiXToolset.WiXToolset`).
Sem o WiX, o script gera a versão portátil em `dist\AvisodeAgua\`, e você pode criar
o instalador com o **Inno Setup**: abra `packaging\instalador.iss` e clique em *Compile*.

Com Maven também funciona: `mvn package` gera `target\AvisodeAgua.jar` (com o driver embutido).

## Usar sua própria foto de cachoeira
Salve a foto como `%APPDATA%\AvisodeAgua\cachoeira.jpg` (ou `.png`). O programa passa a usá-la no alerta.

## Estrutura
```
src/main/java/br/com/avisodeagua/
  App.java                  bandeja do sistema, instância única, ligação entre as partes
  TelaCadastro.java         tela de cadastro (nome, celular, intervalo, horário, meta)
  TelaAlerta.java           tela da cachoeira "Hora de tomar água"
  Agendador.java            calcula e dispara os horários em segundo plano
  Database.java             MySQL (tabelas usuario e registro_consumo)
  InicializacaoWindows.java iniciar junto com o Windows
src/main/resources/img/     cachoeira.png e icone.png
packaging/                  icone.ico e instalador.iss (Inno Setup)
banco/criar_banco.sql       script opcional do banco
```
