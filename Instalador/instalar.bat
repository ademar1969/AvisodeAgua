@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Aviso de Agua - Instalacao

rem ===== precisa de administrador para instalar Java / MariaDB e liberar o firewall =====
net session >nul 2>&1
if errorlevel 1 (
    echo Pedindo permissao de administrador...
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
    exit /b
)

echo ======================================================
echo   AVISO DE AGUA - Instalacao completa
echo   (programa + Java 8 + banco de dados MariaDB)
echo ======================================================
echo.

set "DESTINO=%LOCALAPPDATA%\AvisodeAgua"
set "DRIVER=mysql-connector-j-8.4.0.jar"
set "URL_DRIVER=https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/%DRIVER%"
set "MARIADB_VER=11.4.4"
set "URL_MARIADB=https://archive.mariadb.org/mariadb-%MARIADB_VER%/winx64-packages/mariadb-%MARIADB_VER%-winx64.msi"
set "URL_JAVA=https://api.adoptium.net/v3/installer/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse"
set "SENHA_DB=100219"
set "PORTA_CELULAR=8085"
set "PACOTES=%~dp0pacotes"
if not exist "%PACOTES%" mkdir "%PACOTES%"

rem ================================================================ 1. BANCO DE DADOS
set "TEM_BANCO="
netstat -an | findstr /c:":3306 " | findstr /i "LISTENING ESCUTANDO" >nul 2>&1 && set "TEM_BANCO=1"
sc query MariaDB >nul 2>&1 && set "TEM_BANCO=1"
sc query MySQL80 >nul 2>&1 && set "TEM_BANCO=1"
sc query MySQL >nul 2>&1 && set "TEM_BANCO=1"

echo Qual banco de dados voce vai usar?
echo   [1] Instalar o MariaDB neste computador (banco local - recomendado)
echo   [2] Ja tenho MySQL ou MariaDB instalado (usuario root, senha %SENHA_DB%)
echo   [3] Banco em nuvem (configuro depois no programa: botao "Banco")
if defined TEM_BANCO echo.& echo   * Encontrei um banco ja rodando neste computador: a opcao 2 e a indicada.
echo.
if defined TEM_BANCO (choice /c 123 /n /d 2 /t 60 /m "Escolha 1, 2 ou 3 [padrao 2]: ") else (choice /c 123 /n /d 1 /t 60 /m "Escolha 1, 2 ou 3 [padrao 1]: ")
set "OPCAO_BANCO=%errorlevel%"
echo.

rem ================================================================ 2. JAVA
echo [1/5] Verificando o Java...
call :acharJava
if defined JAVAW goto javaOk
echo       Java nao encontrado. Instalando o Java 8 (Eclipse Temurin JRE)...
if not exist "%PACOTES%\java8-jre.msi" (
    echo       Baixando o Java 8 ...
    curl -L --fail -o "%PACOTES%\java8-jre.msi" "%URL_JAVA%"
)
if exist "%PACOTES%\java8-jre.msi" (
    msiexec /i "%PACOTES%\java8-jre.msi" ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome /qn /norestart
) else (
    echo       Download falhou, tentando pelo winget...
    winget install -e --id EclipseAdoptium.Temurin.8.JRE --silent --accept-package-agreements --accept-source-agreements
)
call :acharJava
if not defined JAVAW (
    echo [ERRO] Nao foi possivel instalar o Java. Instale manualmente: https://adoptium.net/temurin/releases/?version=8
    goto :fim
)
:javaOk
echo       Java: %JAVAW%

rem ================================================================ 3. MARIADB
if not "%OPCAO_BANCO%"=="1" goto pularMariadb
echo [2/5] Banco de dados MariaDB...
if defined TEM_BANCO (
    echo       Ja existe um banco rodando na porta 3306. Nao vou instalar outro.
    goto pularMariadb
)
if /i not "%PROCESSOR_ARCHITECTURE%"=="AMD64" if not defined PROCESSOR_ARCHITEW6432 (
    echo [AVISO] O MariaDB precisa de Windows 64 bits. Use a opcao de banco em nuvem no programa.
    goto pularMariadb
)
if not exist "%PACOTES%\mariadb.msi" (
    echo       Baixando o MariaDB %MARIADB_VER% (cerca de 70 MB^)...
    curl -L --fail -o "%PACOTES%\mariadb.msi" "%URL_MARIADB%"
)
if exist "%PACOTES%\mariadb.msi" (
    echo       Instalando o MariaDB (servico "MariaDB", porta 3306, usuario root, senha %SENHA_DB%^)...
    msiexec /i "%PACOTES%\mariadb.msi" SERVICENAME=MariaDB PORT=3306 PASSWORD=%SENHA_DB% UTF8=1 /qn /norestart
) else (
    echo       Download falhou, tentando pelo winget...
    winget install -e --id MariaDB.Server --silent --accept-package-agreements --accept-source-agreements --override "SERVICENAME=MariaDB PORT=3306 PASSWORD=%SENHA_DB% UTF8=1 /qn"
)
sc query MariaDB | findstr /i "RUNNING" >nul 2>&1
if errorlevel 1 (
    net start MariaDB >nul 2>&1
)
sc query MariaDB | findstr /i "RUNNING" >nul 2>&1
if errorlevel 1 (
    echo [AVISO] O servico MariaDB nao esta rodando. Verifique em services.msc ou use o banco em nuvem.
) else (
    echo       MariaDB instalado e rodando.
)
:pularMariadb
if "%OPCAO_BANCO%"=="3" echo [2/5] Banco em nuvem: configure no programa pelo botao "Banco".
if "%OPCAO_BANCO%"=="2" echo [2/5] Usando o MySQL/MariaDB ja instalado.

rem ================================================================ 4. PROGRAMA
echo [3/5] Copiando o programa para %DESTINO% ...
wmic process where "name='javaw.exe' and commandline like '%%AvisodeAgua.jar%%'" call terminate >nul 2>nul
powershell -NoProfile -Command "Get-WmiObject Win32_Process | Where-Object { $_.Name -eq 'javaw.exe' -and $_.CommandLine -like '*AvisodeAgua.jar*' } | ForEach-Object { $_.Terminate() }" >nul 2>nul
if not exist "%DESTINO%\lib" mkdir "%DESTINO%\lib"
copy /y "AvisodeAgua.jar" "%DESTINO%\" >nul || (echo [ERRO] Falha ao copiar. Feche o programa e tente de novo. & goto :fim)
copy /y "icone.ico" "%DESTINO%\" >nul
copy /y "desinstalar.bat" "%DESTINO%\" >nul
if exist "lib\%DRIVER%" copy /y "lib\%DRIVER%" "%DESTINO%\lib\" >nul
if exist "%PACOTES%\%DRIVER%" copy /y "%PACOTES%\%DRIVER%" "%DESTINO%\lib\" >nul
if not exist "%DESTINO%\lib\%DRIVER%" (
    echo       Baixando o driver do banco ...
    curl -L --fail -s -o "%DESTINO%\lib\%DRIVER%" "%URL_DRIVER%"
)
if not exist "%DESTINO%\lib\%DRIVER%" (
    echo [ERRO] Nao foi possivel baixar o driver: %URL_DRIVER%
    echo        Baixe e coloque na pasta "pacotes" ao lado deste instalador.
    goto :fim
)
if "%OPCAO_BANCO%"=="3" (
    if not exist "%APPDATA%\AvisodeAgua" mkdir "%APPDATA%\AvisodeAgua"
)

echo [4/5] Atalhos e firewall (para o app do celular)...
set "PS1=%TEMP%\avisodeagua_atalho.ps1"
> "%PS1%" echo $s = New-Object -ComObject WScript.Shell
>>"%PS1%" echo foreach ($p in @([Environment]::GetFolderPath('Desktop'), [Environment]::GetFolderPath('Programs'))) {
>>"%PS1%" echo   $l = $s.CreateShortcut((Join-Path $p 'Aviso de Agua.lnk'))
>>"%PS1%" echo   $l.TargetPath = $env:AVISO_JAVAW
>>"%PS1%" echo   $l.Arguments = '-Dfile.encoding=UTF-8 -jar "' + $env:AVISO_DEST + '\AvisodeAgua.jar"'
>>"%PS1%" echo   $l.WorkingDirectory = $env:AVISO_DEST
>>"%PS1%" echo   $l.IconLocation = $env:AVISO_DEST + '\icone.ico'
>>"%PS1%" echo   $l.Description = 'Lembrete para tomar agua'
>>"%PS1%" echo   $l.Save()
>>"%PS1%" echo }
>>"%PS1%" echo $l = $s.CreateShortcut((Join-Path $env:AVISO_DEST 'Aviso de Agua.lnk'))
>>"%PS1%" echo $l.TargetPath = $env:AVISO_JAVAW
>>"%PS1%" echo $l.Arguments = '-Dfile.encoding=UTF-8 -jar "' + $env:AVISO_DEST + '\AvisodeAgua.jar"'
>>"%PS1%" echo $l.WorkingDirectory = $env:AVISO_DEST
>>"%PS1%" echo $l.IconLocation = $env:AVISO_DEST + '\icone.ico'
>>"%PS1%" echo $l.Save()
set "AVISO_JAVAW=%JAVAW%"
set "AVISO_DEST=%DESTINO%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
del "%PS1%" >nul 2>nul
netsh advfirewall firewall delete rule name="Aviso de Agua - celular" >nul 2>nul
netsh advfirewall firewall add rule name="Aviso de Agua - celular" dir=in action=allow protocol=TCP localport=%PORTA_CELULAR% remoteip=localsubnet profile=any >nul 2>nul

echo [5/5] Abrindo o Aviso de Agua ...
rem abre pelo Explorer para o programa rodar como usuario normal (sem administrador)
start "" explorer.exe "%DESTINO%\Aviso de Agua.lnk"

echo.
echo ======================================================
echo  INSTALADO COM SUCESSO!
echo  - Atalho "Aviso de Agua" na Area de Trabalho
echo  - O icone de gota fica perto do relogio
if "%OPCAO_BANCO%"=="1" echo  - Banco MariaDB local: usuario root, senha %SENHA_DB%
if "%OPCAO_BANCO%"=="3" echo  - Banco em nuvem: no programa, clique em "Banco" e informe os dados
echo  - App do celular: no programa, clique em "Celular"
echo ======================================================
goto :fim

rem ---------------------------------------------------------------- procura o javaw.exe
:acharJava
set "JAVAW="
for /f "delims=" %%i in ('where javaw 2^>nul') do if not defined JAVAW set "JAVAW=%%i"
if defined JAVAW exit /b
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
if defined JAVAW exit /b
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jre-8*" "%ProgramFiles%\Eclipse Adoptium\jdk-8*" "%ProgramFiles%\Java\jre1.8*" "%ProgramFiles(x86)%\Java\jre1.8*" "%ProgramFiles%\Java\jre*") do (
    if not defined JAVAW if exist "%%~d\bin\javaw.exe" set "JAVAW=%%~d\bin\javaw.exe"
)
exit /b

:fim
echo.
pause
