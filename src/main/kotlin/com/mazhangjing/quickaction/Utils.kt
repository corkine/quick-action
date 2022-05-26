package com.mazhangjing.quickaction

import com.intellij.openapi.vfs.VirtualFile

/**
 * 查找当前文件所属的工程
 * @param level 最深解析深度
 */
fun findPom(file: VirtualFile, level: Int = 5): VirtualFile? {
    TODO()
}

/**
 * 解析 POM 文件并且获得其模块名
 */
fun parsePomGetModuleName(pom: VirtualFile): String? {
    TODO()
}

/**
 * 调用 `maven clean install` 构建此 Maven 模块，阻塞并返回其真实文件路径
 */
fun mavenBuildReturnLocalPath(pom:VirtualFile): String? {
    TODO()
}

/**
 * 返回远程 ICE 单机需要替包的文件路径
 */
fun parseRemotePath(): String? {
    TODO()
}

/**
 * 根据本地模块路径和远程文件路径生成 SCP 命令，此处允许执行备份
 */
fun genScpCommand(): List<String> {
    TODO()
}

/**
 * 调用 pscp 执行这些命令并返回结果
 */
fun callScpCommands(cmd: List<String>) {
    TODO()
}