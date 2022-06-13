package org.opetryna.xmleditor

import org.opetryna.kotlinxml.*
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder

interface ComponentEvent {
    fun changeText(xmlText: XmlText, newValue: String)
    fun appendChild(entity: XmlEntity, child: XmlNode)
    fun removeChild(node: XmlNode)
    fun renameEntity(entity: XmlEntity, newName: String)
    fun appendAttribute(entity: XmlEntity, name: String, value: String)
    fun updateAttribute(entity: XmlEntity, name: String, value: String)
    fun removeAttribute(entity: XmlEntity, name: String)
    fun executeCommand(cmd: Command)
}

class TextComponent(private val parent: EntityComponent, val xmlText: XmlText)
    : JTextField(xmlText.value), IObservable<ComponentEvent>, Injectable {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    interface Action : Command {
        val name: String
        var textComponent: TextComponent
    }

    @InjectAdd
    private val actions: MutableList<Action> = mutableListOf()
    private lateinit var popupmenu: JPopupMenu

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

        popupmenu = JPopupMenu("Actions")

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
            notifyObservers { it.removeChild(this.xmlText) }
        }
        popupmenu.add(remove)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e))
                    popupmenu.show(this@TextComponent, e.x, e.y)
            }
        })
    }

    override fun injectionCompleted() {
        popupmenu.addSeparator()
        actions.forEach { action ->
            action.textComponent = this@TextComponent
            val menuItem = JMenuItem(action.name)
            menuItem.addActionListener {
                notifyObservers { it.executeCommand(action) }
            }
            popupmenu.add(menuItem)
        }
    }
}

class AttributesComponent(val parent: EntityComponent)
    : JPanel(), IObservable<ComponentEvent>, Injectable {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    private val _attributes: MutableMap<String, Triple<JLabel, AttributeViewTemplate.AttributeView, JButton>> = mutableMapOf()
    val attributes: Map<String, Triple<JLabel, AttributeViewTemplate.AttributeView, JButton>>
        get() = _attributes

    interface AttributeViewTemplate {
        var attributesComponent: AttributesComponent
        val entityName: String?
        val attributeName: String?
        interface AttributeView {
            val component: JComponent
            fun updateValue(value: String)
        }
        fun create(name: String, value: String): AttributeView
    }

    @InjectAdd
    private val views: MutableList<AttributeViewTemplate> = mutableListOf()

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
            var viewTemplate: AttributeViewTemplate = views.last()
            views.forEach {
                if (it.entityName == this.parent.entity.name && it.attributeName == name) {
                    viewTemplate = it
                    return@forEach
                }
            }
            val view = viewTemplate.create(name, value)
            this.add(view.component)
            val deleteButton = JButton("Remove")
            deleteButton.addActionListener {
                notifyObservers { it.removeAttribute(parent.entity, name) }
            }
            this.add(deleteButton)
            this._attributes[name] = Triple(label, view, deleteButton)
        } else {
            elements.second.updateValue(parent.entity.attributes[name]!!)
        }
        revalidate()
        repaint()
    }

    fun removeAttribute(name: String) {
        val elements = _attributes.remove(name)
        if (elements != null) {
            this.remove(elements.first)
            this.remove(elements.second.component)
            this.remove(elements.third)
        }
        revalidate()
        repaint()
    }

    override fun injectionCompleted() {
        views.add(object : AttributeViewTemplate {
            override var attributesComponent = this@AttributesComponent
            override val entityName: String? = null
            override val attributeName: String? = null
            override fun create(name: String, value: String): AttributeViewTemplate.AttributeView {
                val textField = JTextField(value)
                textField.addActionListener {
                    notifyObservers { it.updateAttribute(parent.entity, name, textField.text) }
                }
                return object : AttributeViewTemplate.AttributeView {
                    override val component: JTextField = textField
                    override fun updateValue(value: String) {
                        component.text = value
                    }
                }
            }
        })
        views.forEach { it.attributesComponent = this@AttributesComponent }
    }

}

class EntityComponent(val parent: EntityComponent?, val entity: XmlEntity)
    : JPanel(), IObservable<ComponentEvent>, Injectable {

    override val observers: MutableList<ComponentEvent> = mutableListOf()

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.font = Font("Arial", Font.BOLD, 16)
        g.drawString(entity.name, 10, 20)
    }

    val attributes = Injector.create(AttributesComponent::class, this)

    interface Action : Command {
        val name: String
        var entityComponent: EntityComponent
    }

    @InjectAdd
    private val actions: MutableList<Action> = mutableListOf()
    private lateinit var popupmenu: JPopupMenu

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

                val v = object : XmlVisitor {

                    private var current = this@EntityComponent

                    override fun visit(e: XmlText) {
                        val textComponent = Injector.create(TextComponent::class, current, e)
                        observers.forEach { textComponent.addObserver(it) }
                        add(textComponent)
                    }
                    override fun visit(e: XmlEntity): Boolean {
                        val entityComponent = Injector.create(EntityComponent::class, current, e)
                        observers.forEach { entityComponent.addObserver(it) }
                        add(entityComponent)
                        current = entityComponent
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

                child.accept(v)
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

        popupmenu = JPopupMenu("Actions")

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
            notifyObservers { it.removeChild(this.entity) }
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

    override fun injectionCompleted() {
        popupmenu.addSeparator()
        actions.forEach { action ->
            action.entityComponent = this@EntityComponent
            val menuItem = JMenuItem(action.name)
            menuItem.addActionListener {
                notifyObservers { it.executeCommand(action) }
            }
            popupmenu.add(menuItem)
        }
    }

}