import com.intellij.util.io.isDirectory
import com.intellij.util.io.isFile
import okhttp3.internal.format
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.io.path.Path
import kotlin.io.path.name

val testFile =
    "D:\\ICE4_3\\l47-app\\l47-service\\rest\\src\\main\\java\\com\\inspur\\ice\\l47app\\rest\\resource\\L47DeviceGroupResource.java"
val tP = Path(testFile)

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

fun find(level: Int = 15, from: Path?): Path? {
    if (level <= 0 || from == null) return null
    val checkDir = if (from.isDirectory()) from else from.parent
    println("Now checking $checkDir")
    val isFind = Files.list(checkDir).anyMatch { it.name.uppercase() == "POM.XML" }
    return if (isFind) checkDir.resolve("pom.xml")
    else find(level - 1, checkDir.parent)
}

fun module(pom: Path?): Module? {
    if (pom == null) return null
    val content: String = Files.readString(pom)
    val groupFinder = Pattern.compile("<groupId>([\\da-zA-Z-_.]+)</groupId>").matcher(content)
    val groupId = if (groupFinder.find()) groupFinder.group(1) else "com.inspur.ice"
    val matcher: Matcher = Pattern.compile(
        "(?s)<artifactId>([\\da-zA-Z-_.]+)</artifactId>[\\s\\D]*" +
                "<version>([\\da-zA-Z-_.]+)</version>[\\s\\D]*" +
                "<packaging>bundle</packaging>", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    ).matcher(content)
    if (matcher.find() && matcher.groupCount() >= 2) {
        val module = matcher.group(1)
        val version = matcher.group(2)
        return Module(
            artifactId = module, version = version,
            localPath = pom.parent, groupId = groupId, remotePath = null
        )
    }
    return null
}

//find(from = tP)
val file = Path("D:\\ICE4_3\\l47-app\\l47-service\\rest\\pom.xml")
//module(file)

val module = Module(
    groupId = "com.inspur.ice",
    artifactId = "l47-service-rest",
    version = "4.3.0-SNAPSHOT",
    localPath = Path("D:\\ICE4_3\\l47-app\\l47-service\\rest"),
    remotePath = null
)

fun jar(module: Module): Path? {
    val jar = module.localPath?.resolve("target/${module.artifactId}-${module.version}.jar")
    return if (jar != null && jar.isFile()) jar
    else null
}

val remotes = listOf(
    Remote("172.20.77.111", Path("/root/ice-test")),
    Remote("172.20.77.112", Path("/root/ice-test")),
    Remote("172.20.77.113", Path("/root/ice-test"))
)

jar(module).toString()

fun genScp(module: Module, remotes: List<Remote>): List<String> {
    val jarPath = jar(module) ?: throw java.lang.RuntimeException(
        "此模块 ${module.artifactId} 不存在编译好的 jar 包在 ${module.localPath}!"
    )
    return remotes.map {
        format(
            "echo y | pscp -pw %s %s %s:%s",
            it.pass, jarPath.toString(), it.user, it.targetFolder.resolve(
                format(
                    "system/%s/%s/%s/%s",
                    module.groupId.replace(".", "/"),
                    module.artifactId, module.version, jarPath.name
                )
            )
        )
    }
}

genScp(module, remotes)






