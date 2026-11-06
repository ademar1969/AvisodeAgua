@echo off
setlocal
cd /d "%~dp0"
title Aviso de Agua - Publicar no GitHub
echo ==============================================
echo   Publicar o Aviso de Agua no GitHub
echo ==============================================
echo.
where git >nul 2>nul || (echo [ERRO] Git nao encontrado. Instale com:  winget install Git.Git   e abra este arquivo de novo. & goto :fim)

if not exist ".git" (
    git init -b main
)
rem a pasta deste computador pode ser de rede: considera a pasta segura para o git
git config --global --add safe.directory "*" >nul 2>nul

rem ---- usuario do git: gravado so neste projeto (nao precisa da pasta pessoal) ----
git config user.name >nul 2>nul && goto temNome
set /p NOME=Seu nome para os commits: 
git config user.name "%NOME%"
:temNome
git config user.email >nul 2>nul && goto temEmail
set /p EMAIL=Seu e-mail do GitHub: 
git config user.email "%EMAIL%"
:temEmail
git add -A
git commit -m "Aviso de Agua: programa Windows, instalador e app Android" || echo (nada novo para gravar)

git remote get-url origin >nul 2>nul && goto enviar

echo.
echo Crie o repositorio no site: https://github.com/new
echo   - Nome sugerido: AvisodeAgua
echo   - NAO marque "Add a README" (o projeto ja tem)
echo   - Escolha Private ou Public
echo.
set /p URL=Cole aqui o endereco do repositorio (ex.: https://github.com/SEU_USUARIO/AvisodeAgua.git):
if "%URL%"=="" goto :fim
git remote add origin "%URL%"

:enviar
echo.
echo Enviando... (na primeira vez abre uma janela para entrar na sua conta do GitHub)
git push -u origin main
if errorlevel 1 (
    echo.
    echo [ERRO] O envio falhou. Confira o endereco do repositorio e o login do GitHub.
    goto :fim
)
echo.
echo PRONTO! Projeto publicado. Para enviar mudancas no futuro, rode este arquivo de novo.

:fim
echo.
pause
