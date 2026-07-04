' MoveXml.vbs - Move mapper-xml/gen/*.xml to per-domain subdirs
Option Explicit

Dim objFSO
Set objFSO = CreateObject("Scripting.FileSystemObject")

Dim scriptPath, projectRoot
scriptPath = WScript.ScriptFullName
projectRoot = Left(scriptPath, InStrRev(scriptPath, "\scripts\") - 1)

Dim genXml, parentXml
genXml = projectRoot & "\heartbeat-infrastructure\src\main\resources\mapper-xml\gen"
parentXml = projectRoot & "\heartbeat-infrastructure\src\main\resources\mapper-xml"

WScript.StdOut.WriteLine "genXml: " & genXml

If Not objFSO.FolderExists(genXml) Then
    WScript.StdOut.WriteLine "ERROR: gen not found"
    WScript.Quit 1
End If

' Create subdirs
Dim domains
domains = Array("auth","flow","workflow","pay","report","mobile","mp","structure","sys","event","tool","common")
Dim d
For Each d In domains
    If Not objFSO.FolderExists(parentXml & "\" & d) Then objFSO.CreateFolder(parentXml & "\" & d)
Next
WScript.StdOut.WriteLine "  subdirs created"

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

' Collect all xml files
Dim folderX, file, allFiles(), idx, count
Set folderX = objFSO.GetFolder(genXml)
count = 0
For Each file In folderX.Files
    If LCase(objFSO.GetExtensionName(file.Name)) = "xml" Then
        ReDim Preserve allFiles(count)
        allFiles(count) = file.Path
        count = count + 1
    End If
Next
WScript.StdOut.WriteLine "  collected " & count & " xml files"

Dim i, baseName, domain, targetFile, content
For i = 0 To count - 1
    Dim srcPath, srcName
    srcPath = allFiles(i)
    srcName = objFSO.GetFileName(srcPath)
    baseName = objFSO.GetBaseName(srcName)
    domain = GetDomain(baseName)
    If domain <> "" Then
        targetFile = parentXml & "\" & domain & "\" & srcName
        content = ReadUTF8(srcPath)
        content = Replace(content, _
            "top.kx.heartbeat.infrastructure.persistence.mapper.gen.", _
            "top.kx.heartbeat.infrastructure.persistence.mapper." & domain & ".")
        content = Replace(content, _
            "top.kx.heartbeat.infrastructure.persistence.entity.gen.", _
            "top.kx.heartbeat.infrastructure.persistence.entity." & domain & ".")
        WriteUTF8 targetFile, content
        objFSO.DeleteFile srcPath, True
        WScript.StdOut.WriteLine "  " & srcName & " -> " & domain & "/"
    Else
        WScript.StdOut.WriteLine "  SKIP: " & srcName
    End If
Next

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== Delete gen dir ==="
If objFSO.FolderExists(genXml) Then objFSO.DeleteFolder genXml, True
WScript.StdOut.WriteLine "  done"

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== DONE ==="