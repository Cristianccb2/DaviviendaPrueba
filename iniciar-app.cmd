@echo off
rem Arranca la API de Polizas en http://localhost:8085 con datos semilla limpios.
rem Para usar otro puerto: iniciar-app.cmd 9090
setlocal
cd /d "%~dp0"
set "PUERTO=%~1"
if "%PUERTO%"=="" set "PUERTO=8085"

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not exist "target\polizas-api-1.0.0.jar" (
    echo Compilando la aplicacion por primera vez...
    call mvnw.cmd -q clean package -DskipTests || goto :error
)

title Polizas API - puerto %PUERTO% (cierra esta ventana para detenerla)
echo Iniciando Polizas API en http://localhost:%PUERTO% ...
"%JAVA_EXE%" -jar target\polizas-api-1.0.0.jar --server.port=%PUERTO% --logging.file.name=logs/polizas-api.log
echo.
echo La aplicacion se detuvo.
pause
exit /b 0

:error
echo No se pudo compilar la aplicacion. Revisa que JDK 17 este instalado (java -version).
pause
exit /b 1
