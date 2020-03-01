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
        val autoAlignedText: String?
        val startOffset: Int
        val endOffset: Int

//        if (selectedText != null) { // just align the selected text
//            autoAlignedText = TokenAligner.align(selectedText, currentLine)
//            startOffset = selection.selectionStart
//            endOffset = selection.selectionEnd
//        } else { // auto-align surrounding caret
            autoAlignedText = Aligner.align(document.text, currentLine)
            startOffset = 0
            endOffset = document.textLength
//        }
        replaceString(project, document, autoAlignedText, startOffset, endOffset)
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
