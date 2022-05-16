package org.opetryna.kotlinxml

private fun validateName(s: String): String {
    val result = s.trim()
    if (result.isBlank())
        throw IllegalArgumentException("Name cannot be blank.")
    return result
}

private fun escapeValue(s: String): String {
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}

interface XmlVisitor {
    fun visit(e: XmlText) {}
    fun visit(e: XmlEntity): Boolean = true
    fun endVisit(e: XmlEntity) {}
}

abstract class XmlNode {

    var parent: XmlEntity? = null
        internal set

    abstract fun accept(v: XmlVisitor)

    fun find(accept: (XmlNode) -> Boolean): List<XmlNode> {

        val v = object : XmlVisitor {

            val found: MutableList<XmlNode> = mutableListOf()

            override fun visit(e: XmlText) {
                if (accept(e))
                    found.add(e)
            }

            override fun visit(e: XmlEntity): Boolean {
                if (accept(e))
                    found.add(e)
                return true
            }
        }

        this.accept(v)
        return v.found
    }

}

class XmlText(value: String) : XmlNode() {

    var value: String = validateName(value)
        set(value) {
            field = validateName(value)
        }

    override fun accept(v: XmlVisitor) {
        v.visit(this)
    }

}

class XmlEntity(name: String) : XmlNode() {

    var name: String = validateName(name)
        set(name) {
            field = validateName(name)
        }

    private val _attributes: MutableMap<String, String> = mutableMapOf()
    val attributes: Map<String, String>
        get() = _attributes

    private val _children: MutableList<XmlNode> = mutableListOf()
    val children: List<XmlNode>
        get() = _children

    fun appendAttribute(name: String, value: String) {
        this._attributes[validateName(name)] = value
    }

    fun removeAttribute(name: String) {
        this._attributes.remove(name)
    }

    fun appendChild(node: XmlNode) {
        node.parent?.removeChild(node)
        node.parent = this
        this._children.add(node)
    }

    fun removeChild(node: XmlNode) {
        this._children.remove(node)
        node.parent = null
    }

    override fun accept(v: XmlVisitor) {
        if (v.visit(this)) {
            children.forEach {
                it.accept(v)
            }
        }
        v.endVisit(this)
    }

}

class XmlDocument(val root: XmlEntity) {

    fun serialize(): String {

        val v = object : XmlVisitor {

            var xml: String = "<?xml version=\"1.0\" ?>"
            var level: Int = -1

            override fun visit(e: XmlText) {
                if (e.parent?.children?.size!! > 1)
                    xml += "\n" + "\t".repeat(level + 1)
                xml += escapeValue(e.value)
            }

            override fun visit(e: XmlEntity): Boolean {
                level++
                xml += "\n" + "\t".repeat(level) + "<${e.name}"
                e.attributes.forEach { (name, value) ->
                    xml += " $name=\"${escapeValue(value)}\""
                }
                if (e.children.isEmpty())
                    xml += "/"
                xml += ">"
                return true
            }

            override fun endVisit(e: XmlEntity) {
                if (e.children.isNotEmpty()) {
                    if (!(e.children.size == 1 && e.children[0] is XmlText))
                        xml += "\n" + "\t".repeat(level)
                    xml += "</${e.name}>"
                }
                level--
            }
        }

        root.accept(v)
        return v.xml
    }

}