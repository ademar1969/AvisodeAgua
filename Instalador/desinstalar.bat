@echo off
title Aviso de Agua - Desinstalar
echo Desinstalando o Aviso de Agua...
wmic process where "name='javaw.exe' and commandline like '%%AvisodeAgua.jar%%'" call terminate >nul 2>nul
del "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\AvisodeAgua.vbs" >nul 2>nul
for /f "delims=" %%p in ('powershell -NoProfile -Command "[Environment]::GetFolderPath('Desktop')"') do del "%%p\Aviso de Agua.lnk" >nul 2>nul
for /f "delims=" %%p in ('powershell -NoProfile -Command "[Environment]::GetFolderPath('Programs')"') do del "%%p\Aviso de Agua.lnk" >nul 2>nul
netsh advfirewall firewall delete rule name="Aviso de Agua - celular" >nul 2>nul
echo Os dados no banco (avisodeagua) foram mantidos. O MariaDB e o Java continuam instalados;
echo para remove-los use Configuracoes do Windows ^> Aplicativos.
echo Pronto. A pasta %LOCALAPPDATA%\AvisodeAgua sera removida.
pause
cd /d "%TEMP%"
start "" /min cmd /c "timeout /t 2 >nul & rmdir /s /q ""%LOCALAPPDATA%\AvisodeAgua"""
