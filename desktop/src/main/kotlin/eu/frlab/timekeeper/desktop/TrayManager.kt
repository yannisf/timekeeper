package eu.frlab.timekeeper.desktop

import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TrayManager(
    private val cliExecutor: CliExecutor,
    private val scope: CoroutineScope,
    private val onExit: () -> Unit
) {
    private var trayIcon: TrayIcon? = null
    private var statusWindow: StatusWindow? = null

    fun initialize() {
        if (!SystemTray.isSupported()) {
            System.err.println("System tray is not supported on this platform")
            onExit()
            return
        }

        val tray = SystemTray.getSystemTray()
        val image = loadTrayIcon()

        if (image == null) {
            System.err.println("Failed to load tray icon")
            onExit()
            return
        }

        trayIcon = TrayIcon(image, "Timekeeper").apply {
            isImageAutoSize = true
            popupMenu = createPopupMenu()
        }

        tray.add(trayIcon)
    }

    private fun createPopupMenu(): PopupMenu {
        return PopupMenu().apply {
            val tickItem = MenuItem("Tick")
            tickItem.addActionListener {
                scope.launch {
                    try {
                        val output = cliExecutor.executeTick()
                        showNotification("Tick executed:\n$output")
                    } catch (e: Exception) {
                        showNotification("Error: ${e.message}")
                    }
                }
            }
            add(tickItem)

            val statusItem = MenuItem("Status")
            statusItem.addActionListener {
                scope.launch {
                    try {
                        val output = cliExecutor.executeStatus()
                        SwingUtilities.invokeLater {
                            statusWindow?.close()
                            statusWindow = StatusWindow(output)
                            statusWindow?.show()
                        }
                    } catch (e: Exception) {
                        showNotification("Error: ${e.message}")
                    }
                }
            }
            add(statusItem)

            addSeparator()

            val exitItem = MenuItem("Exit")
            exitItem.addActionListener {
                statusWindow?.close()
                onExit()
            }
            add(exitItem)
        }
    }

    private fun loadTrayIcon(): java.awt.Image? {
        val iconPaths = listOf(
            "tray-iconTemplate.png",
            "icons/tray-iconTemplate.png",
            "/icons/tray-iconTemplate.png"
        )

        for (iconPath in iconPaths) {
            try {
                val resource = javaClass.getResourceAsStream("/$iconPath") ?: continue
                val imageBytes = resource.readAllBytes()
                val tempFile = File.createTempFile("tray-icon", ".png")
                tempFile.writeBytes(imageBytes)
                tempFile.deleteOnExit()
                return java.awt.Toolkit.getDefaultToolkit().createImage(tempFile.absolutePath)
            } catch (e: Exception) {
                // Try next path
            }
        }

        // Fallback: create a simple placeholder image
        return createPlaceholderIcon()
    }

    private fun createPlaceholderIcon(): java.awt.Image {
        val size = 22
        val image = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val graphics = image.graphics
        graphics.color = java.awt.Color.WHITE
        graphics.fillRect(0, 0, size, size)
        graphics.color = java.awt.Color.BLACK
        graphics.drawRect(2, 2, size - 5, size - 5)
        graphics.drawString("T", 8, 16)
        graphics.dispose()
        return image
    }

    private fun showNotification(message: String) {
        SwingUtilities.invokeLater {
            trayIcon?.displayMessage("Timekeeper", message, TrayIcon.MessageType.INFO)
        }
    }
}
