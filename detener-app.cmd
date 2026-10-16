@echo off
rem Detiene la API de Polizas que escucha en el puerto indicado (por defecto 8085).
setlocal
set "PUERTO=%~1"
if "%PUERTO%"=="" set "PUERTO=8085"

set "ENCONTRADO="
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":%PUERTO% .*LISTENING"') do (
    if not defined ENCONTRADO (
        set "ENCONTRADO=%%p"
        taskkill /PID %%p /F >nul && echo Aplicacion detenida ^(PID %%p, puerto %PUERTO%^).
    )
)
if not defined ENCONTRADO echo No hay ninguna aplicacion escuchando en el puerto %PUERTO%.
