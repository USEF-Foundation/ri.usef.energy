
call mvn clean install %*
if %errorlevel% neq 0 goto End

:End
echo "Press enter to exit.. "
pause