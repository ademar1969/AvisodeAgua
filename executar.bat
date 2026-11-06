@echo off
rem Executa o programa sem instalar (precisa ter rodado gerar-instalador.bat antes, ou compilar).
cd /d "%~dp0"
if not exist build\input\AvisodeAgua.jar (echo Rode primeiro o gerar-instalador.bat & pause & exit /b)
start "" javaw -Dfile.encoding=UTF-8 -cp "build\input\*" br.com.avisodeagua.App
