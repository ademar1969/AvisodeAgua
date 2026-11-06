@echo off
rem Baixa os instaladores do Java 8, MariaDB e o driver para a pasta "pacotes".
rem Depois copie a pasta Instalador inteira (pen drive) para instalar em outros computadores sem internet.
cd /d "%~dp0"
if not exist pacotes mkdir pacotes
echo Baixando Java 8 (Temurin JRE 64 bits)...
curl -L --fail -o pacotes\java8-jre.msi "https://api.adoptium.net/v3/installer/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse"
echo Baixando MariaDB 11.4.4...
curl -L --fail -o pacotes\mariadb.msi "https://archive.mariadb.org/mariadb-11.4.4/winx64-packages/mariadb-11.4.4-winx64.msi"
echo Baixando driver do banco...
curl -L --fail -o pacotes\mysql-connector-j-8.4.0.jar "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar"
echo.
dir pacotes
echo Pronto.
pause
