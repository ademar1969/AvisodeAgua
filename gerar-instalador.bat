@echo off
setlocal
cd /d "%~dp0"
title Aviso de Agua - Gerar instalador

echo ==============================================
echo   AVISO DE AGUA - compilacao e instalador
echo ==============================================
echo.

where javac >nul 2>nul || (echo [ERRO] JDK nao encontrado. Instale o JDK 17 ou superior ^(ex.: winget install EclipseAdoptium.Temurin.21.JDK^) & goto :fim)
where jpackage >nul 2>nul || (echo [ERRO] jpackage nao encontrado. Use um JDK 17 ou superior. & goto :fim)

set MYSQL_VER=8.4.0
set MYSQL_JAR=lib\mysql-connector-j-%MYSQL_VER%.jar

if not exist lib mkdir lib
if not exist "%MYSQL_JAR%" (
    echo Baixando driver MySQL %MYSQL_VER% ...
    curl -L -o "%MYSQL_JAR%" https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/%MYSQL_VER%/mysql-connector-j-%MYSQL_VER%.jar
    if errorlevel 1 (echo [ERRO] Falha ao baixar o driver. Coloque o arquivo manualmente em %MYSQL_JAR% & goto :fim)
)

echo [1/4] Limpando...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\classes build\input dist

echo [2/4] Compilando...
dir /s /b src\main\java\*.java > build\fontes.txt
javac --release 8 -encoding UTF-8 -d build\classes -cp "%MYSQL_JAR%" @build\fontes.txt
if errorlevel 1 (echo [ERRO] Falha na compilacao. & goto :fim)
xcopy /e /i /q /y src\main\resources build\classes >nul

echo [3/4] Gerando JAR...
jar --create --file build\input\AvisodeAgua.jar --main-class br.com.avisodeagua.App -C build\classes .
copy /y "%MYSQL_JAR%" build\input\ >nul

echo [4/4] Gerando instalador .exe ...
jpackage --type exe ^
  --name AvisodeAgua ^
  --app-version 1.0.0 ^
  --vendor "Jintellige" ^
  --description "Lembrete para tomar agua" ^
  --input build\input ^
  --main-jar AvisodeAgua.jar ^
  --main-class br.com.avisodeagua.App ^
  --icon packaging\icone.ico ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --win-shortcut --win-menu --win-menu-group "Aviso de Agua" ^
  --win-dir-chooser --win-per-user-install --win-shortcut-prompt ^
  --win-upgrade-uuid 6f1c2b7e-4a8d-4e2f-9b1a-a9c3d0e5f701 ^
  --dest dist

if errorlevel 1 (
    echo.
    echo [AVISO] Nao foi possivel gerar o .exe. O jpackage precisa do WiX Toolset.
    echo         Instale com:  winget install WiXToolset.WiXToolset
    echo         ou use o Inno Setup com o arquivo packaging\instalador.iss
    echo.
    echo Gerando versao portatil em dist\AvisodeAgua ...
    jpackage --type app-image --name AvisodeAgua --app-version 1.0.0 --vendor "Jintellige" ^
      --input build\input --main-jar AvisodeAgua.jar --main-class br.com.avisodeagua.App ^
      --icon packaging\icone.ico --java-options "-Dfile.encoding=UTF-8" --dest dist
    if errorlevel 1 (echo [ERRO] Falha ao gerar a versao portatil. & goto :fim)
    echo.
    echo Pronto: dist\AvisodeAgua\AvisodeAgua.exe
    echo Para gerar o instalador com Inno Setup, abra packaging\instalador.iss e clique em Compile.
    goto :fim
)

echo.
echo ==============================================
echo  PRONTO! Instalador gerado em: dist\AvisodeAgua-1.0.0.exe
echo ==============================================

:fim
echo.
pause
