@echo off
setlocal 

for %%i in (*.svg) do call :sub %%i
pause
goto :eof

:sub
echo convert: %1

call :convert 32 ..\..\res\drawable-mdpi %1
call :convert 48 ..\..\res\drawable-hdpi %1
call :convert 64 ..\..\res\drawable-xhdpi %1
call :convert 96 ..\..\res\drawable-xxhdpi %1
goto :eof

:convert
set name=%3

if exist %2\%name:.svg=.png% (
	echo "%2\%name:.svg=.png%" already exists, no recreate.
	goto :eof
)
"C:\Program Files (x86)\Inkscape\inkscape.exe" -w %1 -e %2\%name:.svg=.png% %name%
goto :eof
