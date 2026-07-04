@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul

set "BASE=D:\Desktop\自定义文件夹\HeartBeat-jsonschema"
set "GEN_E=%BASE%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen"
set "PARENT_E=%BASE%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity"
set "GEN_M=%BASE%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper\gen"
set "PARENT_M=%BASE%\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper"

echo === 创建子包目录 ===
for %%D in (auth flow workflow pay report mobile mp structure sys event tool common) do (
    if not exist "%PARENT_E%\%%D" mkdir "%PARENT_E%\%%D"
    if not exist "%PARENT_M%\%%D" mkdir "%PARENT_M%\%%D"
)

REM 分类函数 (cmd 不支持函数,使用嵌套 if 实现)
:GetDomain
    set "DOMAIN="
    set "NAME=%~1"
    REM 注意:flow 要在 wf 之前匹配吗?Hb* vs Wf* 无冲突
    echo %NAME% | findstr /b "FlowWaitState" > nul && set "DOMAIN=event" & exit /b 0
    echo %NAME% | findstr /b "SysInbox" > nul && set "DOMAIN=event" & exit /b 0
    echo %NAME% | findstr /b "SysOutbox" > nul && set "DOMAIN=event" & exit /b 0
    echo %NAME% | findstr /b "Auth" > nul && set "DOMAIN=auth" & exit /b 0
    echo %NAME% | findstr /b "Hb" > nul && set "DOMAIN=flow" & exit /b 0
    echo %NAME% | findstr /b "Wf" > nul && set "DOMAIN=workflow" & exit /b 0
    echo %NAME% | findstr /b "Pay" > nul && set "DOMAIN=pay" & exit /b 0
    echo %NAME% | findstr /b "Report" > nul && set "DOMAIN=report" & exit /b 0
    echo %NAME% | findstr /b "Mobile" > nul && set "DOMAIN=mobile" & exit /b 0
    echo %NAME% | findstr /b "Mp" > nul && set "DOMAIN=mp" & exit /b 0
    echo %NAME% | findstr /b "Structure" > nul && set "DOMAIN=structure" & exit /b 0
    echo %NAME% | findstr /b "Sys" > nul && set "DOMAIN=sys" & exit /b 0
    exit /b 1

echo === 移动并重写 DO 文件 ===
set /a COUNT_E=0
for %%F in ("%GEN_E%\*.java") do (
    set "BASENAME=%%~nF"
    call :GetDomain "%%~nF"
    if defined DOMAIN (
        set "TARGET_DIR=%PARENT_E%\!DOMAIN!"
        set "TARGET_FILE=!TARGET_DIR!\%%~nxF"

        REM 读取并替换 package
        set "TEMP_FILE=!TARGET_FILE!.tmp"
        (
            for /f "usebackq tokens=* delims=" %%L in ("%%F") do (
                set "LINE=%%L"
                if "!LINE!" == "package top.kx.heartbeat.infrastructure.persistence.entity.gen;" (
                    echo package top.kx.heartbeat.infrastructure.persistence.entity.!DOMAIN!.
                ) else (
                    echo !LINE!
                )
            )
        ) > "!TEMP_FILE!"

        REM 原子替换
        move /y "!TEMP_FILE!" "!TARGET_FILE!" > nul
        del "%%F"
        set /a COUNT_E+=1
    ) else (
        echo   未匹配: %%~nxF
    )
)
echo 移动了 %COUNT_E% 个 DO 文件

echo.
echo === 移动并重写 Mapper 文件 ===
set /a COUNT_M=0
for %%F in ("%GEN_M%\*.java") do (
    set "BASENAME=%%~nF"
    call :GetDomain "%%~nF"
    if defined DOMAIN (
        set "TARGET_DIR=%PARENT_M%\!DOMAIN!"
        set "TARGET_FILE=!TARGET_DIR!\%%~nxF"

        set "TEMP_FILE=!TARGET_FILE!.tmp"
        (
            for /f "usebackq tokens=* delims=" %%L in ("%%F") do (
                set "LINE=%%L"
                if "!LINE!" == "package top.kx.heartbeat.infrastructure.persistence.mapper.gen;" (
                    echo package top.kx.heartbeat.infrastructure.persistence.mapper.!DOMAIN!.
                ) else (
                    set "TMP=!LINE!"
                    REM 替换 entity.gen 引用
                    set "TMP=!TMP:top.kx.heartbeat.infrastructure.persistence.entity.gen.=top.kx.heartbeat.infrastructure.persistence.entity.!DOMAIN!.!"
                    echo !TMP!
                )
            )
        ) > "!TEMP_FILE!"

        move /y "!TEMP_FILE!" "!TARGET_FILE!" > nul
        del "%%F"
        set /a COUNT_M+=1
    ) else (
        echo   未匹配: %%~nxF
    )
)
echo 移动了 %COUNT_M% 个 Mapper 文件

echo.
echo === 删除空的 gen 目录 ===
if exist "%GEN_E%" rmdir "%GEN_E%"
if exist "%GEN_M%" rmdir "%GEN_M%"

echo === 完成 ===
endlocal