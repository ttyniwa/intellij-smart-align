package com.github.ttyniwa.intellij.plugin.align

import com.github.ttyniwa.intellij.plugin.align.aligner.Aligner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project

class CodeAlignerAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(PlatformDataKeys.PROJECT)!!
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        if (!editor.document.isWritable) {
            return
        }

        val document = editor.document
        val currentLine = editor.caretModel.logicalPosition.line
        val selectedText = editor.selectionModel.selectedText

        val alignedText = if (selectedText != null) {
            // just align the selected text
            val selection = editor.selectionModel
            val startPos = editor.visualToLogicalPosition(selection.selectionStartPosition!!)
            val endPos = editor.visualToLogicalPosition(selection.selectionEndPosition!!)
            val startLine = startPos.line
            val endLine = endPos.line + if (endPos.column == 0) -1 else 0
            Aligner.align(document.text, IntRange(startLine, endLine))
        } else {
            // auto-align surrounding caret
            Aligner.align(document.text, currentLine)
        }

        replaceString(project, document, alignedText, 0, document.textLength)
    }

    private fun replaceString(project: Project?, document: Document, replaceText: String, startOffset: Int, endOffset: Int) {
        CommandProcessor.getInstance().executeCommand(project,
                {
                    ApplicationManager.getApplication().runWriteAction {
                        document.replaceString(startOffset, endOffset, replaceText)
                    }
                }, "Smart-Align", this)
    }
}
