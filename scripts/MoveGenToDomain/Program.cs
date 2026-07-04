using System;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

class MoveGenToDomain
{
    static string GetDomain(string fileName)
    {
        if (fileName.StartsWith("FlowWaitState")) return "event";
        if (fileName.StartsWith("SysInbox")) return "event";
        if (fileName.StartsWith("SysOutbox")) return "event";
        if (fileName.StartsWith("Auth")) return "auth";
        if (fileName.StartsWith("Hb")) return "flow";
        if (fileName.StartsWith("Wf")) return "workflow";
        if (fileName.StartsWith("Pay")) return "pay";
        if (fileName.StartsWith("Report")) return "report";
        if (fileName.StartsWith("Mobile")) return "mobile";
        if (fileName.StartsWith("Mp")) return "mp";
        if (fileName.StartsWith("Structure")) return "structure";
        if (fileName.StartsWith("Sys")) return "sys";
        return null;
    }

    static void Main(string[] args)
    {
        string baseDir = @"D:\Desktop\自定义文件夹\HeartBeat-jsonschema";
        string entityGen = Path.Combine(baseDir, @"heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity\gen");
        string entityParent = Path.Combine(baseDir, @"heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\entity");
        string mapperGen = Path.Combine(baseDir, @"heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper\gen");
        string mapperParent = Path.Combine(baseDir, @"heartbeat-infrastructure\src\main\java\top\kx\heartbeat\infrastructure\persistence\mapper");

        Console.OutputEncoding = Encoding.UTF8;

        Console.WriteLine("=== 创建子包目录 ===");
        string[] allDomains = { "auth", "flow", "workflow", "pay", "report", "mobile", "mp", "structure", "sys", "event", "tool", "common" };
        foreach (var d in allDomains)
        {
            Directory.CreateDirectory(Path.Combine(entityParent, d));
            Directory.CreateDirectory(Path.Combine(mapperParent, d));
        }

        Console.WriteLine("=== 移动并重写 DO 文件 ===");
        int countE = 0;
        foreach (var srcFile in Directory.GetFiles(entityGen, "*.java"))
        {
            var fileName = Path.GetFileName(srcFile);
            var baseName = Path.GetFileNameWithoutExtension(fileName);
            var domain = GetDomain(baseName);
            if (domain == null)
            {
                Console.WriteLine($"  未匹配: {fileName}");
                continue;
            }

            var content = File.ReadAllText(srcFile, Encoding.UTF8);
            content = content.Replace(
                "package top.kx.heartbeat.infrastructure.persistence.entity.gen;",
                $"package top.kx.heartbeat.infrastructure.persistence.entity.{domain};"
            );

            var targetFile = Path.Combine(entityParent, domain, fileName);
            File.WriteAllText(targetFile, content, new UTF8Encoding(false));
            File.Delete(srcFile);
            countE++;
        }
        Console.WriteLine($"移动了 {countE} 个 DO 文件");

        Console.WriteLine();
        Console.WriteLine("=== 移动并重写 Mapper 文件 ===");
        int countM = 0;
        foreach (var srcFile in Directory.GetFiles(mapperGen, "*.java"))
        {
            var fileName = Path.GetFileName(srcFile);
            var baseName = Path.GetFileNameWithoutExtension(fileName);
            var domain = GetDomain(baseName);
            if (domain == null)
            {
                Console.WriteLine($"  未匹配: {fileName}");
                continue;
            }

            var content = File.ReadAllText(srcFile, Encoding.UTF8);
            content = content.Replace(
                "package top.kx.heartbeat.infrastructure.persistence.mapper.gen;",
                $"package top.kx.heartbeat.infrastructure.persistence.mapper.{domain};"
            );
            content = content.Replace(
                "top.kx.heartbeat.infrastructure.persistence.entity.gen.",
                $"top.kx.heartbeat.infrastructure.persistence.entity.{domain}."
            );

            var targetFile = Path.Combine(mapperParent, domain, fileName);
            File.WriteAllText(targetFile, content, new UTF8Encoding(false));
            File.Delete(srcFile);
            countM++;
        }
        Console.WriteLine($"移动了 {countM} 个 Mapper 文件");

        Console.WriteLine();
        Console.WriteLine("=== 删除空的 gen 目录 ===");
        if (Directory.Exists(entityGen)) Directory.Delete(entityGen, true);
        if (Directory.Exists(mapperGen)) Directory.Delete(mapperGen, true);
        Console.WriteLine("  完成");

        Console.WriteLine();
        Console.WriteLine("=== 完成 ===");
    }
}