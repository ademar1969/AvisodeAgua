; Instalador alternativo com Inno Setup (https://jrsoftware.org/isinfo.php)
; 1) Rode gerar-instalador.bat (ele cria a pasta dist\AvisodeAgua se o WiX nao estiver instalado)
; 2) Abra este arquivo no Inno Setup e clique em Build > Compile

#define AppName "Aviso de Agua"
#define AppVersion "1.0.0"
#define AppExe "AvisodeAgua.exe"

[Setup]
AppId={{6F1C2B7E-4A8D-4E2F-9B1A-A9C3D0E5F701}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=Jintellige
DefaultDirName={localappdata}\AvisodeAgua
DefaultGroupName={#AppName}
PrivilegesRequired=lowest
OutputDir=..\dist
OutputBaseFilename=AvisodeAgua-Setup-{#AppVersion}
SetupIconFile=icone.ico
UninstallDisplayIcon={app}\{#AppExe}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Tasks]
Name: "desktopicon"; Description: "Criar atalho na area de trabalho"; GroupDescription: "Atalhos:"
Name: "startup"; Description: "Iniciar junto com o Windows"; GroupDescription: "Opcoes:"

[Files]
Source: "..\dist\AvisodeAgua\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"
Name: "{group}\Desinstalar {#AppName}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "AvisodeAgua"; ValueData: """{app}\{#AppExe}"" --oculto"; Flags: uninsdeletevalue; Tasks: startup

[Run]
Filename: "{app}\{#AppExe}"; Description: "Abrir o Aviso de Agua agora"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "taskkill"; Parameters: "/F /IM {#AppExe}"; Flags: runhidden; RunOnceId: "FecharApp"

[UninstallDelete]
Type: dirifempty; Name: "{app}"
