package eu.frlab.timekeeper.desktop

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea

class StatusWindow(private val statusText: String) {
    private var dialog: JDialog? = null

    fun show() {
        val frame = JFrame()
        dialog = JDialog(frame, "Timekeeper Status", true).apply {
            defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE
            size = Dimension(500, 400)
            setLocationRelativeTo(null)

            val textArea = JTextArea(statusText).apply {
                isEditable = false
                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                margin = Insets(10, 10, 10, 10)
            }

            val scrollPane = JScrollPane(textArea).apply {
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            }

            add(scrollPane, BorderLayout.CENTER)
            isVisible = true
        }
    }

    fun close() {
        dialog?.dispose()
        dialog = null
    }
}
