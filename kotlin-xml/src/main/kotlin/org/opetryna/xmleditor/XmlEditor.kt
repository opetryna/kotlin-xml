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
    fun changeValue(xmlText: XmlText, newValue: String)
    fun appendChild(entity: XmlEntity, child: XmlNode)
    fun remove(node: XmlNode)
    fun rename(entity: XmlEntity, newName: String)
}

class TextComponent(private val parent: EntityComponent, val xmlText: XmlText)
    : JLabel(xmlText.value), IObservable<ComponentEvent> {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    init {
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
                notifyObservers { it.changeValue(this.xmlText, text) }
            }
        }
        popupmenu.add(change)

        val remove = JMenuItem("Remove")
        remove.addActionListener {
            notifyObservers { it.remove(this.xmlText) }
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

class EntityComponent(val parent: EntityComponent?, val entity: XmlEntity)
    : JPanel(), IObservable<ComponentEvent> {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.font = Font("Arial", Font.BOLD, 16)
        g.drawString(entity.name, 10, 20)
    }

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
                TODO("Not yet implemented")
            }

            override fun attributeRemoved(attribute: String) {
                TODO("Not yet implemented")
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
    }

    private fun createPopupMenu() {

        val popupmenu = JPopupMenu("Actions")

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
            notifyObservers { it.remove(this.entity) }
        }
        popupmenu.add(remove)

        val rename = JMenuItem("Rename")
        rename.addActionListener {
            val text = JOptionPane.showInputDialog("New name")
            if (text != null && text.isNotBlank()) {
                notifyObservers { it.rename(this.entity, text) }
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

// Controller
class DocumentView() : JFrame("XML Editor") {

    private var root: EntityComponent = EntityComponent(null, XmlEntity("root"))
    private val historyUndo = ArrayDeque<Command>()
    private val historyRedo = ArrayDeque<Command>()

    val componentEventObserver = object : ComponentEvent {

        override fun changeValue(xmlText: XmlText, newValue: String) {
            val cmd = object : Command {
                val oldValue = xmlText.value
                override fun execute() {
                    xmlText.value = newValue
                }
                override fun undo() {
                    xmlText.value = oldValue
                }
            }
            cmd.execute()
            historyUndo.addLast(cmd)
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
            cmd.execute()
            historyUndo.addLast(cmd)
        }

        override fun remove(node: XmlNode) {
            val cmd = object : Command {
                val parent = node.parent
                override fun execute() {
                    parent?.removeChild(node)
                }
                override fun undo() {
                    parent?.appendChild(node)
                }
            }
            cmd.execute()
            historyUndo.addLast(cmd)
        }

        override fun rename(entity: XmlEntity, newName: String) {
            val cmd = object : Command {
                val oldName = entity.name
                override fun execute() {
                    entity.name = newName
                }
                override fun undo() {
                    entity.name = oldName
                }
            }
            cmd.execute()
            historyUndo.addLast(cmd)
        }

    }

    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(1000, 1000)

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
        historyUndo.clear()
        historyRedo.clear()
        add(root)
        revalidate()
        repaint()
    }

    fun open() {
        this.setLocationRelativeTo(null)
        isVisible = true
    }

}

fun main() {
    // TODO
}