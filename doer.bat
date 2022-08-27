@echo off
SET JVMCMD="%JAVA_HOME%bin\java"
SET JAR=%DOER_HOME%\build\libs\doer-all.jar

%JVMCMD% %DOER_JVM_ARGS% -jar %JAR% %*
