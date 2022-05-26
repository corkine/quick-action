package com.mazhangjing.quickaction

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalView
import java.io.IOException


class MyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            val manager = FileEditorManager.getInstance(it)
            val allOpenFiles = manager.openFiles.joinToString(", ") { file ->
                file.name
            }
            if (allOpenFiles.isEmpty()) {
                Messages.showInfoMessage("No file open!", "ERROR")
            } else {
                Messages.showMessageDialog(
                    allOpenFiles, "TITLE", Messages.getInformationIcon()
                )
            }
            val terminalView = TerminalView.getInstance(e.project!!)
            val window: ToolWindow? =
                ToolWindowManager.getInstance(e.project!!).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            try {
                terminalView.createLocalShellWidget(".", "HELLO").executeCommand("java -jar wrapper.jar")
            } catch (e: IOException) {
                println("Cannot execute command")
            }
        }

    }

}
