/**
 * 1. 查找 Multi-Maven 工程中结尾为 .yang 或 .java 的可写源文件所在 Maven 模块，编译后上传 Jar 包到远程服务器。
 * 2. 查找 Multi-Maven 工程中结尾为 .java 的可写源文件，找到其 Maven 模块并编译，然后将其字节码上传到远程服务器。
 * 3. 查找 Multi-Maven 工程中结尾为 .java 的可写源文件，找到其 Maven 模块并生成 `maven clean install` 命令。
 * 4. 查找 Multi-Maven 工程中结尾为 .java 的可写源文件，找到其 Maven 模块将编译好的 Jar 包收集到本地一目录。
 * 相关配置和远程 ICE 列在下方，直接修改即可。
 * @author Corkine Ma
 * @since 0.0.3
 * @lastUpdate 2022-5-30
 */
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.util.io.*
import liveplugin.*
import java.nio.file.CopyOption
import java.nio.file.Path
import kotlin.io.path.name

val r79 = listOf(
    Remote("172.20.79.11", kotlin.io.path.Path("/root/ice-test")),
    Remote("172.20.79.12", kotlin.io.path.Path("/root/ice-test")),
    Remote("172.20.79.13", kotlin.io.path.Path("/root/ice-test"))
)
val r77 = listOf(
    Remote("172.20.77.111", kotlin.io.path.Path("/root/ice-test")),
    Remote("172.20.77.112", kotlin.io.path.Path("/root/ice-test")),
    Remote("172.20.77.113", kotlin.io.path.Path("/root/ice-test"))
)

val shortCutPatch = "alt F2"
val shortCutHotPatch = "alt F10"
val shortCutGenCompile = "ctrl alt F8"
val shortCutCollectJar = "ctrl alt F9"
val compileFirstWhenPatch = true
val compileFirstWhenHotPatch = true
val withRemoteBackup = true
val remotes = r79
val remotesHotPathFolder: Path = Path.of("/root")
val localCollectFolder: Path = Path.of("C:/Users/mazhangjing/Desktop/collectJar")
val suffix = listOf(".java", ".yang")

registerAction(
    id = "Gen Maven Compile Command", keyStroke = shortCutGenCompile,
    actionGroupId = "ToolsMenu", positionInGroup = com.intellij.openapi.actionSystem.Constraints.LAST,
) { event: AnActionEvent ->
    val files = tabFile(suffix)
    if (files.isEmpty()) {
        showOkCancelDialog("提示", "当前打开的页面中没有可操作的文件, 已中断生成命令。", "确定", "取消")
        return@registerAction
    }
    try {
        val modules = files.mapNotNull { module(find(from = it)) }.toSet().toList()
        val allCommands = genMaven(modules).joinToString("\n\n")
        PluginUtil.showInConsole(unixPath(allCommands), event.project!!)
    } catch (e: Exception) {
        showOkCancelDialog("错误", "发生了一些错误：$e", "确定", "取消")
    }
}

registerAction(
    id = "Collect Jar to Folder", keyStroke = shortCutCollectJar,
    actionGroupId = "ToolsMenu", positionInGroup = com.intellij.openapi.actionSystem.Constraints.LAST,
) { _: AnActionEvent ->
    val files = tabFile(suffix)
    if (files.isEmpty()) {
        showOkCancelDialog("提示", "当前打开的页面中没有可操作的文件, 已中断生成命令。", "确定", "取消")
        return@registerAction
    }
    try {
        val modules = files.mapNotNull { module(find(from = it)) }.toSet().toList()
        modules.forEach { copyJarToCollect(it, localCollectFolder) }
        showOkCancelDialog("成功", "收集模块 ${modules.joinToString(", ") { it.artifactId }} " +
                "Jar 包到 $localCollectFolder 完成。", "确定", "取消")
    } catch (e: Exception) {
        showOkCancelDialog("错误", "发生了一些错误：$e", "确定", "取消")
    }
}

registerAction(
    id = "Gen Patch Jar Command", keyStroke = shortCutPatch,
    actionGroupId = "ToolsMenu", positionInGroup = com.intellij.openapi.actionSystem.Constraints.LAST,
) { event: AnActionEvent ->
    val files = tabFile(suffix)
    if (files.isEmpty()) {
        showOkCancelDialog("提示", "当前打开的页面中没有可操作的文件, 已中断生成命令。", "确定", "取消")
        return@registerAction
    }
    try {
        val modules = files.mapNotNull { module(find(from = it)) }.toSet().toList()
        val compileCommands = if (compileFirstWhenPatch) genMaven(modules) else arrayListOf()
        val scpCommands = modules.map { genScpPatch(it, remotes, copyFirst = withRemoteBackup).joinToString("\n") }
        val allCommands = "# ===================== Maven 编译  =====================  \n\n" +
                compileCommands.joinToString("\n# ----------------\n") + "\n\n" +
                "#  ===================== SCP 传送  ===================== \n\n" +
                scpCommands.joinToString("\n\n# ----------------\n\n" + "\n\n")
        PluginUtil.showInConsole(unixPath(allCommands), event.project!!)
    } catch (e: Exception) {
        showOkCancelDialog("错误", "发生了一些错误：$e", "确定", "取消")
    }
}

registerAction(
    id = "Gen Patch Class Command", keyStroke = shortCutHotPatch,
    actionGroupId = "ToolsMenu", positionInGroup = com.intellij.openapi.actionSystem.Constraints.LAST,
) { event: AnActionEvent ->
    val files = tabFile(listOf(".java"))
    if (files.isEmpty()) {
        showOkCancelDialog("提示", "当前打开的页面中没有可操作的 java 文件, 已中断生成命令。", "确定", "取消")
        return@registerAction
    }
    try {
        val moduleFilePair = files.mapNotNull {
            val mayModule = module(find(from = it))
            if (mayModule == null) null else Pair(mayModule, it)
        }
        val modules = moduleFilePair.map { it.first }.toSet().toList()
        val compileCommands = if (compileFirstWhenHotPatch) genMaven(modules) else arrayListOf()
        val scpCommands = moduleFilePair.map { genScpHotPatch(it.first, it.second, remotes).joinToString("\n") }
        val allCommands = "# ===================== Maven 编译  =====================  \n\n" +
                compileCommands.joinToString("\n# ----------------\n") + "\n\n" +
                "#  ===================== SCP 传送  ===================== \n\n" +
                scpCommands.joinToString("\n\n# ----------------\n\n") + "\n\n"
        PluginUtil.showInConsole(unixPath(allCommands), event.project!!)
    } catch (e: Exception) {
        showOkCancelDialog("错误", "发生了一些错误：$e", "确定", "取消")
    }
}

fun tabFile(suffix: List<String>): List<Path> {
    return FileEditorManager.getInstance(project!!).openFiles.map {
        Path.of(it.path)
    }.filter { p -> endsWith(p.name, suffix) && p.isFile() && p.isWritable }
}

data class Module(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val localPath: Path?,
    val remotePath: Path?
)

data class Remote(
    val ip: String,
    val targetFolder: Path,
    val user: String = "root",
    val pass: String = "inspur@123",
)

fun endsWith(path: String, suffix: List<String>) = suffix.any { path.endsWith(it, true) }

fun find(level: Int = 15, from: Path?): Path? {
    if (level <= 0 || from == null) return null
    val checkDir = if (from.isDirectory()) from else from.parent
    //show("Now checking $checkDir")
    val isFind = java.nio.file.Files.list(checkDir).anyMatch { it.name.uppercase() == "POM.XML" }
    return if (isFind) checkDir.resolve("pom.xml")
    else find(level - 1, checkDir.parent)
}

fun module(pom: Path?): Module? {
    if (pom == null) return null
    val content: String = java.nio.file.Files.readString(pom)
    val groupFinder = java.util.regex.Pattern.compile("<groupId>([\\da-zA-Z-_.]+)</groupId>").matcher(content)
    val groupId = if (groupFinder.find()) groupFinder.group(1) else "com.inspur.ice"
    val matcher: java.util.regex.Matcher = java.util.regex.Pattern.compile(
        "(?s)<artifactId>([\\da-zA-Z-_.]+)</artifactId>[\\s\\D]*" +
                "<version>([\\da-zA-Z-_.]+)</version>[\\s\\D]*" +
                "<packaging>bundle</packaging>",
        java.util.regex.Pattern.CASE_INSENSITIVE or java.util.regex.Pattern.MULTILINE
    ).matcher(content)
    if (matcher.find() && matcher.groupCount() >= 2) {
        val module = matcher.group(1)
        val version = matcher.group(2)
        return Module(
            artifactId = module, version = version,
            localPath = pom.parent, groupId = groupId, remotePath = null
        )
    } else {
        val matcherAgain: java.util.regex.Matcher = java.util.regex.Pattern.compile(
            "(?s)<artifactId>([\\da-zA-Z-_.]+)</artifactId>[\\s\\D]*" +
                    "<packaging>bundle</packaging>[\\s\\D]*" +
                    "<version>([\\da-zA-Z-_.]+)</version>",
            java.util.regex.Pattern.CASE_INSENSITIVE or java.util.regex.Pattern.MULTILINE
        ).matcher(content)
        if (matcherAgain.find() && matcherAgain.groupCount() >= 2) {
            val module = matcherAgain.group(1)
            val version = matcherAgain.group(2)
            return Module(
                artifactId = module, version = version,
                localPath = pom.parent, groupId = groupId, remotePath = null
            )
        }
    }
    return null
}

fun unixPath(input: String):String {
    return input.replace("\\","/")
}

fun jar(module: Module?): Path? {
    if (module == null) return null
    val jar = module.localPath?.resolve("target/${module.artifactId}-${module.version}.jar")
    return if (jar != null && jar.isFile()) jar
    else null
}

fun clazz(module: Module?, file: Path?): Path? {
    if (module == null || file == null) return null
    val clazz = module.localPath?.resolve("src/main/java")?.relativize(file)?.let {
        module.localPath.resolve("target/classes").resolve(it).toString()
            .replace(".java", ".class")
    } ?: return null
    return if (Path.of(clazz).isFile()) Path.of(clazz)
    else null
}

fun genMaven(modules: List<Module>): List<String> {
    return modules.map { module ->
        val path = module.localPath
        if (path == null || !path.isDirectory()) {
            throw RuntimeException("不存在此模块 ${module.artifactId} 在 ${module.localPath}")
        }
        String.format("cd ${module.localPath}; mvn clean install")
    }
}

fun copyJarToCollect(module: Module, folder: Path): Module {
    val jarPath = jar(module) ?: throw java.lang.RuntimeException(
        "此模块 ${module.artifactId} 不存在编译好的 jar 包在 ${module.localPath}!"
    )
    if (folder.exists()) {
        folder.delete(true)
    }
    folder.createDirectories()
    java.nio.file.Files.copy(jarPath, folder.resolve(jarPath.fileName))
    return module
}

fun genScpPatch(module: Module, remotes: List<Remote>, copyFirst: Boolean = false): List<String> {
    val jarPath = jar(module) ?: throw java.lang.RuntimeException(
        "此模块 ${module.artifactId} 不存在编译好的 jar 包在 ${module.localPath}!"
    )
    val copy = if (copyFirst) remotes.map {
        val remoteLine = String.format(
            "%s@%s:%s",
            it.user, it.ip, it.targetFolder.resolve(
                String.format(
                    "system/%s/%s/%s/%s",
                    module.groupId.replace(".", "/"),
                    module.artifactId, module.version, jarPath.name
                )
            )
        )
        val randomId = kotlin.random.Random.Default.nextInt(100) + 1
        String.format("echo y | pscp -pw %s %s %s_%s", it.pass, remoteLine, remoteLine, randomId)
    } else arrayListOf()
    val move = remotes.map {
        String.format(
            "echo y | pscp -pw %s %s %s@%s:%s",
            it.pass, jarPath.toString(), it.user, it.ip, it.targetFolder.resolve(
                String.format(
                    "system/%s/%s/%s/%s",
                    module.groupId.replace(".", "/"),
                    module.artifactId, module.version, jarPath.name
                )
            )
        )
    }
    return copy + move
}

fun genScpHotPatch(module: Module, file: Path, remotes: List<Remote>): List<String> {
    val classPath = clazz(module, file) ?: throw java.lang.RuntimeException(
        "此模块 ${module.artifactId} 不存在编译好的 class 文件在 ${module.localPath}!"
    )
    val move = remotes.map {
        String.format(
            "echo y | pscp -pw %s %s %s@%s:%s",
            it.pass, classPath.toString(), it.user, it.ip, remotesHotPathFolder.resolve(classPath.name)
        )
    }
    return move
}

//show(files)
//PluginUtil.showInConsole(PluginUtil.execute("dir").toString(), event.project!!)
//PsiViewerDialog(project, PluginUtil.currentEditorIn(event.project!!)).show()
/*try {
    val command = "cmd /c dir"
    val result = PluginUtil.execute(command)
    val output = "> " + command + "\n" + result["stdout"] + "\n------\n" + result["stderr"]
    PluginUtil.showInConsole("Output $output", event.project!!)
} catch (e: Exception) {
    PluginUtil.showInConsole("Exception: $e", event.project!!)
}*/
//ProgressManager.getInstance().run(CommandMonitoringBackgroundTask(shellTerminalWidget, processListener, project, commandLineString))
