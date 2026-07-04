' MoveGen.vbs - UTF-8 aware file mover (collect first, then process)
Option Explicit

Dim objFSO
Set objFSO = CreateObject("Scripting.FileSystemObject")

Dim scriptPath, projectRoot
scriptPath = WScript.ScriptFullName
projectRoot = Left(scriptPath, InStrRev(scriptPath, "\scripts\") - 1)
WScript.StdOut.WriteLine "Project root: " & projectRoot

Dim genE, parentE, genM, parentM
genE = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen"
parentE = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity"
genM = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper\gen"
parentM = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper"

WScript.StdOut.WriteLine "genE: " & genE
WScript.StdOut.WriteLine "genM: " & genM

If Not objFSO.FolderExists(genE) Then
    WScript.StdOut.WriteLine "ERROR: " & genE & " not found"
    WScript.Quit 1
End If

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== Create subdirs ==="
Dim domains
domains = Array("auth","flow","workflow","pay","report","mobile","mp","structure","sys","event","tool","common")
Dim d
For Each d In domains
    If Not objFSO.FolderExists(parentE & "\" & d) Then objFSO.CreateFolder(parentE & "\" & d)
    If Not objFSO.FolderExists(parentM & "\" & d) Then objFSO.CreateFolder(parentM & "\" & d)
Next
WScript.StdOut.WriteLine "  done"

Function GetDomain(name)
    If Left(name, 13) = "FlowWaitState" Then GetDomain = "event" : Exit Function
    If Left(name, 8) = "SysInbox" Then GetDomain = "event" : Exit Function
    If Left(name, 9) = "SysOutbox" Then GetDomain = "event" : Exit Function
    If Left(name, 4) = "Auth" Then GetDomain = "auth" : Exit Function
    If Left(name, 2) = "Hb" Then GetDomain = "flow" : Exit Function
    If Left(name, 2) = "Wf" Then GetDomain = "workflow" : Exit Function
    If Left(name, 3) = "Pay" Then GetDomain = "pay" : Exit Function
    If Left(name, 6) = "Report" Then GetDomain = "report" : Exit Function
    If Left(name, 6) = "Mobile" Then GetDomain = "mobile" : Exit Function
    If Left(name, 2) = "Mp" Then GetDomain = "mp" : Exit Function
    If Left(name, 9) = "Structure" Then GetDomain = "structure" : Exit Function
    If Left(name, 3) = "Sys" Then GetDomain = "sys" : Exit Function
    GetDomain = ""
End Function

Function ReadUTF8(path)
    Dim stream
    Set stream = CreateObject("ADODB.Stream")
    stream.Type = 2
    stream.Charset = "UTF-8"
    stream.Open
    stream.LoadFromFile path
    ReadUTF8 = stream.ReadText
    stream.Close
End Function

Sub WriteUTF8(path, content)
    Dim stream
    Set stream = CreateObject("ADODB.Stream")
    stream.Type = 2
    stream.Charset = "UTF-8"
    stream.Open
    stream.WriteText content
    stream.SaveToFile path, 2
    stream.Close
End Sub

' Collect all file paths first
WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== Move DO files ==="
Dim folderE, file, allFiles(), idx, countE
Set folderE = objFSO.GetFolder(genE)
countE = 0
For Each file In folderE.Files
    If LCase(objFSO.GetExtensionName(file.Name)) = "java" Then
        ReDim Preserve allFiles(countE)
        allFiles(countE) = file.Path
        countE = countE + 1
    End If
Next
WScript.StdOut.WriteLine "  Collected " & countE & " DO files"

Dim i, baseName, domain, targetFile, content
For i = 0 To countE - 1
    Dim srcPath, srcName
    srcPath = allFiles(i)
    srcName = objFSO.GetFileName(srcPath)
    baseName = objFSO.GetBaseName(srcName)
    domain = GetDomain(baseName)
    If domain <> "" Then
        targetFile = parentE & "\" & domain & "\" & srcName
        content = ReadUTF8(srcPath)
        content = Replace(content, _
            "package top.kx.heartbeat.infrastructure.persistence.entity.gen;", _
            "package top.kx.heartbeat.infrastructure.persistence.entity." & domain & ";")
        WriteUTF8 targetFile, content
        objFSO.DeleteFile srcPath, True
        WScript.StdOut.WriteLine "  " & srcName & " -> entity/" & domain & "/"
    Else
        WScript.StdOut.WriteLine "  SKIP: " & srcName
    End If
Next

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== Move Mapper files ==="
Dim folderM, allFilesM(), countM
Set folderM = objFSO.GetFolder(genM)
countM = 0
For Each file In folderM.Files
    If LCase(objFSO.GetExtensionName(file.Name)) = "java" Then
        ReDim Preserve allFilesM(countM)
        allFilesM(countM) = file.Path
        countM = countM + 1
    End If
Next
WScript.StdOut.WriteLine "  Collected " & countM & " Mapper files"

For i = 0 To countM - 1
    srcPath = allFilesM(i)
    srcName = objFSO.GetFileName(srcPath)
    baseName = objFSO.GetBaseName(srcName)
    domain = GetDomain(baseName)
    If domain <> "" Then
        targetFile = parentM & "\" & domain & "\" & srcName
        content = ReadUTF8(srcPath)
        content = Replace(content, _
            "package top.kx.heartbeat.infrastructure.persistence.mapper.gen;", _
            "package top.kx.heartbeat.infrastructure.persistence.mapper." & domain & ";")
        content = Replace(content, _
            "top.kx.heartbeat.infrastructure.persistence.entity.gen.", _
            "top.kx.heartbeat.infrastructure.persistence.entity." & domain & ".")
        WriteUTF8 targetFile, content
        objFSO.DeleteFile srcPath, True
        WScript.StdOut.WriteLine "  " & srcName & " -> mapper/" & domain & "/"
    Else
        WScript.StdOut.WriteLine "  SKIP: " & srcName
    End If
Next

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== Delete gen dirs ==="
If objFSO.FolderExists(genE) Then objFSO.DeleteFolder genE, True
If objFSO.FolderExists(genM) Then objFSO.DeleteFolder genM, True
WScript.StdOut.WriteLine "  done"

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== DONE ==="