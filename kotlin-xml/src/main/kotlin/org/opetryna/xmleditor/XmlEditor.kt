package org.opetryna.xmleditor

import org.opetryna.kotlinxml.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder

interface Command {
    fun execute()
    fun undo()
}

interface ComponentEvent {
    fun changeText(xmlText: XmlText, newValue: String)
    fun appendChild(entity: XmlEntity, child: XmlNode)
    fun removeNode(node: XmlNode)
    fun renameEntity(entity: XmlEntity, newName: String)
    fun appendAttribute(entity: XmlEntity, name: String, value: String)
    fun removeAttribute(entity: XmlEntity, name: String)
}

class TextComponent(private val parent: EntityComponent, val xmlText: XmlText)
    : JTextField(xmlText.value), IObservable<ComponentEvent> {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    init {
        addActionListener {
            notifyObservers { it.changeText(this.xmlText, this.text) }
        }
        createPopupMenu()
        xmlText.addObserver(object : XmlText.Event {
            override fun valueChanged() {
                this@TextComponent.text = xmlText.value
                revalidate()
                repaint()
            }
        })
    }

    private fun createPopupMenu() {

        val popupmenu = JPopupMenu("Actions")

        val change = JMenuItem("Change")
        change.addActionListener {
            val text = JOptionPane.showInputDialog("New value")
            if (text != null && text.isNotBlank()) {
                notifyObservers { it.changeText(this.xmlText, text) }
            }
        }
        popupmenu.add(change)

        val remove = JMenuItem("Remove")
        remove.addActionListener {
            notifyObservers { it.removeNode(this.xmlText) }
        }
        popupmenu.add(remove)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e))
                    popupmenu.show(this@TextComponent, e.x, e.y)
            }
        })
    }
}

class AttributesComponent(val parent: EntityComponent)
    : JPanel(), IObservable<ComponentEvent> {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    private val _attributes: MutableMap<String, Triple<JLabel, JTextField, JButton>> = mutableMapOf()
    val attributes: Map<String, Triple<JLabel, JTextField, JButton>>
        get() = _attributes

    init {
        layout = GridLayout(0, 3)
        border = CompoundBorder(
            BorderFactory.createEmptyBorder(30, 10, 10, 10),
            BorderFactory.createLineBorder(Color.BLACK, 2, true)
        )
    }

    fun appendAttribute(name: String, value: String) {
        val elements = attributes[name]
        if (elements == null) {
            val label = JLabel(name)
            this.add(label)
            val textField = JTextField(value)
            textField.addActionListener {
                notifyObservers { it.appendAttribute(parent.entity, name, textField.text) }
            }
            this.add(textField)
            val deleteButton = JButton("Remove")
            deleteButton.addActionListener {
                notifyObservers { it.removeAttribute(parent.entity, name) }
            }
            this.add(deleteButton)
            this._attributes[name] = Triple(label, textField, deleteButton)
        } else {
            elements.second.text = parent.entity.attributes[name]
        }
        revalidate()
        repaint()
    }

    fun removeAttribute(name: String) {
        val elements = _attributes.remove(name)
        if (elements != null) {
            this.remove(elements.first)
            this.remove(elements.second)
            this.remove(elements.third)
        }
        revalidate()
        repaint()
    }

}

class EntityComponent(val parent: EntityComponent?, val entity: XmlEntity)
    : JPanel(), IObservable<ComponentEvent> {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.font = Font("Arial", Font.BOLD, 16)
        g.drawString(entity.name, 10, 20)
    }

    val attributes = AttributesComponent(this)

    init {
        layout = GridLayout(0, 1)
        border = CompoundBorder(
            BorderFactory.createEmptyBorder(30, 10, 10, 10),
            BorderFactory.createLineBorder(Color.BLACK, 2, true)
        )
        createPopupMenu()
        entity.addObserver(object : XmlEntity.Event {
            override fun nameChanged() {
                revalidate()
                repaint()
            }

            override fun attributeAppended(attribute: String) {
                this@EntityComponent.attributes.appendAttribute(
                    attribute, this@EntityComponent.entity.attributes[attribute]!!
                )
            }

            override fun attributeRemoved(attribute: String) {
                this@EntityComponent.attributes.removeAttribute(attribute)
            }

            override fun childAppended(child: XmlNode) {
                if (child is XmlEntity) {
                    val entityComponent = EntityComponent(this@EntityComponent, child)
                    observers.forEach { entityComponent.addObserver(it) }
                    add(entityComponent)
                } else if (child is XmlText) {
                    val textComponent = TextComponent(this@EntityComponent, child)
                    observers.forEach { textComponent.addObserver(it) }
                    add(textComponent)
                }
                revalidate()
                repaint()
            }

            override fun childRemoved(child: XmlNode) {
                this@EntityComponent.components.forEach {
                    if (it is EntityComponent) {
                        if (it.entity === child)
                            remove(it)
                    } else if (it is TextComponent) {
                        if (it.xmlText === child)
                            remove(it)
                    }
                }
                revalidate()
                repaint()
            }

        })
        add(attributes)
    }

    override fun addObserver(observer: ComponentEvent) {
        super.addObserver(observer)
        observers.forEach { attributes.addObserver(it) }
    }

    private fun createPopupMenu() {

        val popupmenu = JPopupMenu("Actions")

        val addAttribute = JMenuItem("Append attribute")
        addAttribute.addActionListener {
            val nameField = JTextField(10)
            val valueField = JTextField(10)
            val panel = JPanel()
            panel.layout = GridLayout(2, 2)
            panel.add(JLabel("name:"))
            panel.add(nameField)
            panel.add(JLabel("value:"))
            panel.add(valueField)
            val result = JOptionPane.showConfirmDialog(null, panel, "Append attribute", JOptionPane.OK_CANCEL_OPTION)
            if (result == JOptionPane.OK_OPTION && nameField.text.isNotBlank() && valueField.text.isNotBlank()) {
                notifyObservers { it.appendAttribute(this.entity, nameField.text, valueField.text) }
            }
        }
        popupmenu.add(addAttribute)

        val addXmlEntity = JMenuItem("Append entity")
        addXmlEntity.addActionListener {
            val text = JOptionPane.showInputDialog("Entity name")
            if (text != null && text.isNotBlank()) {
                notifyObservers { it.appendChild(this.entity, XmlEntity(text)) }
            }
        }
        popupmenu.add(addXmlEntity)

        val addXmlText = JMenuItem("Append text")
        addXmlText.addActionListener {
            val text = JOptionPane.showInputDialog("Text value")
            if (text != null && text.isNotBlank()) {
                notifyObservers { it.appendChild(this.entity, XmlText(text)) }
            }
        }
        popupmenu.add(addXmlText)

        val remove = JMenuItem("Remove")
        remove.addActionListener {
            notifyObservers { it.removeNode(this.entity) }
        }
        popupmenu.add(remove)

        val rename = JMenuItem("Rename")
        rename.addActionListener {
            val text = JOptionPane.showInputDialog("New name")
            if (text != null && text.isNotBlank()) {
                notifyObservers { it.renameEntity(this.entity, text) }
            }
        }
        popupmenu.add(rename)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e))
                    popupmenu.show(this@EntityComponent, e.x, e.y)
            }
        })
    }

}

class CommandHistory {

    private val historyUndo = ArrayDeque<Command>()
    private val historyRedo = ArrayDeque<Command>()

    fun execute(cmd: Command) {
        cmd.execute()
        historyUndo.addLast(cmd)
        historyRedo.clear()
    }

    fun undo() {
        val cmd = historyUndo.removeLastOrNull()
        if (cmd != null) {
            cmd.undo()
            historyRedo.addLast(cmd)
        }
    }

    fun redo() {
        val cmd = historyRedo.removeLastOrNull()
        if (cmd != null) {
            cmd.execute()
            historyUndo.addLast(cmd)
        }
    }

    fun clear() {
        historyUndo.clear()
        historyRedo.clear()
    }

}

// Controller
class DocumentView() : JFrame("XML Editor") {

    private var root: EntityComponent = EntityComponent(null, XmlEntity("root"))
    private var commandHistory: CommandHistory = CommandHistory()

    val componentEventObserver = object : ComponentEvent {

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

        override fun removeNode(node: XmlNode) {
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

    }

    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(1000, 1000)
        createMenuBar()
        root.addObserver(componentEventObserver)
        add(root)
    }

    fun loadDocument(document: XmlDocument) {

        val v = object : XmlVisitor {

            val root = EntityComponent(null, document.root)
            var current = root

            override fun visit(e: XmlText) {
                val new = TextComponent(current, e)
                new.addObserver(componentEventObserver)
                current.add(new)
            }

            override fun visit(e: XmlEntity): Boolean {
                if (e != root.entity) {
                    val new = EntityComponent(current, e)
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

        document.root.accept(v)
        remove(root)
        root = v.root
        root.addObserver(componentEventObserver)
        commandHistory.clear()
        add(root)
        revalidate()
        repaint()
    }

    fun open() {
        this.setLocationRelativeTo(null)
        isVisible = true
    }

    private fun createMenuBar() {

        val menuBar = JMenuBar()

        val fileMenu = JMenu("File")
        val openDocument = JMenuItem("Open")
        fileMenu.add(openDocument)
        val saveDocument = JMenuItem("Save")
        fileMenu.add(saveDocument)
        val exportDocument = JMenuItem("Export")
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

}

fun main() {
    // TODO
}