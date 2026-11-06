# Aviso de Água — aplicativo Android

App em Java, **sem bibliotecas externas** (só o Android SDK). Android 8.0 ou mais novo.

## O que o app faz
- **Lembretes de água no próprio celular**, com os mesmos horários e intervalo do cadastro no PC.
  Funcionam mesmo sem conexão. A notificação tem os botões **Já bebi** e **Lembrar em 5 min**;
  ao tocar nela abre a tela da **cachoeira** "Hora de tomar água".
- **Água de hoje**: progresso da meta e botão **+ Bebi um copo**.
- **Alimentação**: busca nos ~100 alimentos com calorias aproximadas, total do dia x meta.
- **Nova medição**: peso + 12 medidas (com o valor anterior como dica) e **fotos** pela câmera ou galeria
  (a rotação da foto é corrigida automaticamente).
- **Progresso**: resumo do peso, IMC, gráfico da água da semana, **desenho do corpo** e **régua de progresso**
  (as mesmas imagens geradas pelo programa do PC).
- Copos marcados sem conexão ficam numa fila e são enviados quando o celular alcançar o PC.

## Como compilar e instalar (Android Studio)
1. Instale o **Android Studio** (developer.android.com/studio).
2. **File > Open** e escolha esta pasta `android`.
3. Espere o *Gradle Sync* terminar (na primeira vez ele baixa o Gradle 8.7 e o Android SDK 34).
   Se aparecer aviso de atualizar o "Android Gradle Plugin", pode aceitar.
4. No celular: **Configurações > Sobre o telefone**, toque 7 vezes em *Número da versão* para ativar o
   modo desenvolvedor; depois em **Opções do desenvolvedor** ative **Depuração USB**.
5. Ligue o celular no USB e clique no botão ▶ **Run**. O app é instalado e abre.
   - Para gerar o arquivo: **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
     O APK fica em `app/build/outputs/apk/debug/app-debug.apk` — dá para mandar por WhatsApp/e-mail e instalar
     (o celular pede para permitir "instalar apps desconhecidos").

> Se o Android Studio reclamar do *Gradle wrapper*, use **File > Sync Project with Gradle Files**
> ou, no terminal do Android Studio, `gradle wrapper` para gerar os scripts.

## Conectar ao computador
1. No programa do PC, clique em **Celular**: aparecem o **endereço** (ex.: `192.168.0.10:8085`)
   e o **código de acesso** (8 letras/números).
2. No app, toque em **Configurar conexão com o PC**, digite os dois e toque em **Testar conexão e salvar**.
3. O celular precisa estar no **mesmo Wi-Fi** do PC e o programa precisa estar aberto (ícone da gota).

## Permissões pedidas
- **Notificações** (Android 13+) para os lembretes.
- **Alarmes e lembretes** (Android 12+) para tocar na hora exata (sem ela, o Android pode atrasar alguns minutos).
- **Armazenamento** só no Android 8 e 9, para salvar a foto tirada pela câmera.

## Estrutura
```
app/src/main/java/br/com/avisodeagua/app/
  MainActivity         tela inicial (água, lembretes, calorias, progresso)
  ConfigActivity       endereço do PC e código de acesso
  AlertaActivity       tela da cachoeira "Hora de tomar água"
  AlimentacaoActivity  refeições do dia
  MedidasActivity      nova medição + fotos
  ProgressoActivity    resumo, gráfico da água, desenho do corpo e régua
  Lembretes            agenda os alarmes (AlarmManager)
  AlarmeReceiver       mostra a notificação
  AcaoReceiver         botões "Já bebi" / "Lembrar em 5 min"
  BootReceiver         reagenda depois de reiniciar o celular
  Registro             fila de copos para enviar ao PC
  Api, Prefs, Ui, BarrasView
```

## API do programa do PC (para referência)
Todas as chamadas levam o header `X-Token: <código>` (ou `?token=` nas imagens).
`GET /api/ping`, `GET /api/resumo`, `POST /api/agua`, `GET /api/agua/semana`, `GET /api/alimentos`,
`GET|POST /api/refeicoes`, `POST /api/refeicoes/remover`, `GET|POST /api/medidas`, `POST /api/foto`,
`GET /api/imagem/corpo.png`, `GET /api/imagem/regua.png`.
