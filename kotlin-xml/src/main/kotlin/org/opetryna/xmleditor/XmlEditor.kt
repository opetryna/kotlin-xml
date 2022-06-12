package org.opetryna.xmleditor

import org.opetryna.kotlinxml.*
import java.awt.*
import java.io.*
import java.nio.file.Paths
import javax.swing.*

class XmlEditor() : JFrame("XML Editor") {

    private var document: XmlDocument = XmlDocument(XmlEntity("root"))
    private var root: EntityComponent = Injector.create(EntityComponent::class, null, document.root)

    private val commandHistory: CommandHistory = CommandHistory()

    private val componentEventObserver = object : ComponentEvent {

        override fun changeText(xmlText: XmlText, newValue: String) {
            val cmd = object : Command {
                val oldValue = xmlText.value
                override fun execute() {
                    xmlText.value = newValue
                }
                override fun undo() {
                    xmlText.value = oldValue
                }
            }
            commandHistory.execute(cmd)
        }

        override fun appendChild(entity: XmlEntity, child: XmlNode) {
            val cmd = object : Command {
                override fun execute() {
                    entity.appendChild(child)
                }
                override fun undo() {
                    entity.removeChild(child)
                }
            }
            commandHistory.execute(cmd)
        }

        override fun removeChild(node: XmlNode) {
            val cmd = object : Command {
                val parent = node.parent
                override fun execute() {
                    parent?.removeChild(node)
                }
                override fun undo() {
                    parent?.appendChild(node)
                }
            }
            commandHistory.execute(cmd)
        }

        override fun renameEntity(entity: XmlEntity, newName: String) {
            val cmd = object : Command {
                val oldName = entity.name
                override fun execute() {
                    entity.name = newName
                }
                override fun undo() {
                    entity.name = oldName
                }
            }
            commandHistory.execute(cmd)
        }

        override fun appendAttribute(entity: XmlEntity, name: String, value: String) {
            val cmd = object : Command {
                override fun execute() {
                    entity.appendAttribute(name, value)
                }
                override fun undo() {
                    entity.removeAttribute(name)
                }
            }
            commandHistory.execute(cmd)
        }

        override fun updateAttribute(entity: XmlEntity, name: String, value: String) {
            val cmd = object : Command {
                val oldValue = entity.attributes[name]
                override fun execute() {
                    entity.appendAttribute(name, value)
                }
                override fun undo() {
                    entity.appendAttribute(name, oldValue!!)
                }
            }
            commandHistory.execute(cmd)
        }

        override fun removeAttribute(entity: XmlEntity, name: String) {
            val cmd = object : Command {
                val value = entity.attributes[name]
                override fun execute() {
                    entity.removeAttribute(name)
                }
                override fun undo() {
                    entity.appendAttribute(name, value!!)
                }
            }
            commandHistory.execute(cmd)
        }

        override fun executeCommand(cmd: Command) {
            commandHistory.execute(cmd)
        }

    }

    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(1000, 1000)
        createMenuBar()
        root.addObserver(componentEventObserver)
        add(root)
    }

    private fun createMenuBar() {

        val menuBar = JMenuBar()

        val fileMenu = JMenu("File")
        val loadDocument = JMenuItem("Load")
        loadDocument.addActionListener {
            val fileDialog = FileDialog(this, "Load document", FileDialog.LOAD)
            fileDialog.file = "*.xmlkt"
            fileDialog.setLocationRelativeTo(this)
            fileDialog.isVisible = true
            val filename = Paths.get(fileDialog.directory, fileDialog.file).toString()
            if (filename.isNotBlank()) {
                val ois = ObjectInputStream(FileInputStream(filename))
                ois.use { ois ->
                    val document = ois.readObject() as XmlDocument
                    loadDocument(document)
                }
            }
        }
        fileMenu.add(loadDocument)
        val saveDocument = JMenuItem("Save")
        saveDocument.addActionListener {
            val fileDialog = FileDialog(this, "Save document", FileDialog.SAVE)
            fileDialog.file = "*.xmlkt"
            fileDialog.setLocationRelativeTo(this)
            fileDialog.isVisible = true
            val filename = Paths.get(fileDialog.directory, fileDialog.file).toString()
            if (filename.isNotBlank()) {
                val oos = ObjectOutputStream(FileOutputStream(filename))
                oos.use { oos ->
                    oos.writeObject(document)
                }
            }
        }
        fileMenu.add(saveDocument)
        val exportDocument = JMenuItem("Export")
        exportDocument.addActionListener {
            val fileDialog = FileDialog(this, "Export document", FileDialog.SAVE)
            fileDialog.file = "*.xml"
            fileDialog.setLocationRelativeTo(this)
            fileDialog.isVisible = true
            val filename = Paths.get(fileDialog.directory, fileDialog.file).toString()
            if (filename.isNotBlank()) {
                File(filename).writeText(this.document.serialize() + "\n", charset = Charsets.UTF_8)
            }
        }
        fileMenu.add(exportDocument)
        menuBar.add(fileMenu)

        val editMenu = JMenu("Edit")
        val undoCommand = JMenuItem("Undo")
        undoCommand.addActionListener { commandHistory.undo() }
        editMenu.add(undoCommand)
        val redoCommand = JMenuItem("Redo")
        redoCommand.addActionListener { commandHistory.redo() }
        editMenu.add(redoCommand)
        menuBar.add(editMenu)

        this.jMenuBar = menuBar
    }

    fun open() {
        this.setLocationRelativeTo(null)
        isVisible = true
    }

    fun loadDocument(document: XmlDocument) {

        val v = object : XmlVisitor {

            val root = Injector.create(EntityComponent::class, null, document.root)
            var current = root

            override fun visit(e: XmlText) {
                val new = Injector.create(TextComponent::class, current, e)
                new.addObserver(componentEventObserver)
                current.add(new)
            }

            override fun visit(e: XmlEntity): Boolean {
                if (e != root.entity) {
                    val new = Injector.create(EntityComponent::class, current, e)
                    new.addObserver(componentEventObserver)
                    current.add(new)
                    current = new
                }
                e.attributes.forEach { (name, value) ->
                    current.attributes.appendAttribute(name, value)
                }
                return true
            }

            override fun endVisit(e: XmlEntity) {
                if (current.parent != null)
                    current = current.parent!!
            }

        }

        this.document = document
        document.root.accept(v)
        remove(root)
        root = v.root
        root.addObserver(componentEventObserver)
        commandHistory.clear()
        add(root)
        revalidate()
        repaint()
    }

}

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        try {
            Injector.readConfig(args[0])
        } catch (e: Exception) {
            System.err.println("It was not possible to read the config.")
            e.printStackTrace()
        }
    }
    val xmlEditor = XmlEditor()
    xmlEditor.open()
}