' StripBom.vbs - Remove BOM using FileSystemObject text read
Option Explicit

Dim objFSO
Set objFSO = CreateObject("Scripting.FileSystemObject")

Dim scriptPath, projectRoot
scriptPath = WScript.ScriptFullName
projectRoot = Left(scriptPath, InStrRev(scriptPath, "\scripts\") - 1)
WScript.StdOut.WriteLine "Project root: " & projectRoot

' Read text with BOM stripped manually
Function ReadTextSkipBom(path)
    Dim stream, raw
    Set stream = CreateObject("ADODB.Stream")
    stream.Type = 1
    stream.Open
    stream.LoadFromFile path
    ' Read first 3 bytes
    Dim first3
    first3 = stream.Read(3)
    ' If not BOM, rewind by setting Position to 0
    Dim b1, b2, b3
    b1 = AscB(MidB(first3, 1, 1))
    b2 = AscB(MidB(first3, 2, 1))
    b3 = AscB(MidB(first3, 3, 1))
    If Not (b1 = &HEF And b2 = &HBB And b3 = &HBF) Then
        stream.Position = 0
    End If
    ' Convert remaining to UTF-8 text
    stream.Type = 2
    stream.Charset = "UTF-8"
    ReadTextSkipBom = stream.ReadText
    stream.Close
End Function

Sub WriteTextNoBom(path, content)
    Dim stream
    Set stream = CreateObject("ADODB.Stream")
    stream.Type = 2
    stream.Charset = "UTF-8"
    stream.Open
    stream.WriteText content
    stream.SaveToFile path, 2
    stream.Close
End Sub

Sub ProcessFolder(folder)
    Dim f, s
    For Each f In folder.Files
        If LCase(objFSO.GetExtensionName(f.Name)) = "java" Then
            ' Read first 3 bytes to check BOM
            Dim stream, first3, b1, b2, b3
            Set stream = CreateObject("ADODB.Stream")
            stream.Type = 1
            stream.Open
            stream.LoadFromFile f.Path
            first3 = stream.Read(3)
            stream.Close
            b1 = AscB(MidB(first3, 1, 1))
            b2 = AscB(MidB(first3, 2, 1))
            b3 = AscB(MidB(first3, 3, 1))
            If b1 = &HEF And b2 = &HBB And b3 = &HBF Then
                Dim content
                content = ReadTextSkipBom(f.Path)
                WriteTextNoBom f.Path, content
                WScript.StdOut.WriteLine "  Strip BOM: " & Replace(f.Path, projectRoot, "")
            End If
        End If
    Next
    For Each s In folder.SubFolders
        ProcessFolder s
    Next
End Sub

Dim targetDirs(2)
targetDirs(0) = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity"
targetDirs(1) = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper"
targetDirs(2) = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\event"

Dim t
For t = 0 To UBound(targetDirs)
    ProcessFolder objFSO.GetFolder(targetDirs(t))
Next

WScript.StdOut.WriteLine ""
WScript.StdOut.WriteLine "=== DONE ==="