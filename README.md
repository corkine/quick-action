# IDEA 插件：quickAction

IDEA 自动化工作流插件，基于 LivePlugin 插件的动态 Kotlin 脚本和自行实现的 quickAction 插件。

## (真)一键替包
plugin.kts 实现了一键从打开的 Java 文件中找到其对应的 Maven 目录，编译 Jar 包，并且将编译后的产物上传到 ICE 单个或集群服务器中。

需要安装 LivePlugin 插件并且新建动态插件将 plugin.kts 加载运行，远程 ICE 配置和一些设置可在脚本中自行修改。

```kotlin
val compileFirst = true
val withRemoteBackup = true
val remotes = listOf(
    Remote("172.20.77.111", Path("/root/ice-test")),
    Remote("172.20.77.112", Path("/root/ice-test")),
    Remote("172.20.77.113", Path("/root/ice-test"))
)
```