' Debug.vbs - test path resolution
Option Explicit

Dim objFSO
Set objFSO = CreateObject("Scripting.FileSystemObject")

Dim scriptPath, projectRoot
scriptPath = WScript.ScriptFullName
projectRoot = Left(scriptPath, InStrRev(scriptPath, "\scripts\") - 1)
WScript.StdOut.WriteLine "Project root: " & projectRoot

Dim genE
genE = projectRoot & "\heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen"
WScript.StdOut.WriteLine "genE: " & genE
WScript.StdOut.WriteLine "FolderExists: " & objFSO.FolderExists(genE)

If objFSO.FolderExists(genE) Then
    Dim folder, file, count
    Set folder = objFSO.GetFolder(genE)
    count = 0
    For Each file In folder.Files
        count = count + 1
        If count <= 3 Then
            WScript.StdOut.WriteLine "  " & file.Name & " (Path=" & file.Path & ")"
        End If
    Next
    WScript.StdOut.WriteLine "Total: " & count & " files"
End If