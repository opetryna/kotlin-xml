package org.opetryna.kotlinxml

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class XmlEntityTest {

    private val root = XmlEntity("root")

    @Test
    fun getName() {
        assertEquals("root", root.name)
    }

    @Test
    fun setName() {
        root.name = "newNameRoot"
        assertEquals("newNameRoot", root.name)
    }

    @Test
    fun getAttributes() {
        root.appendAttribute("attributeName", "attributeValue")
        val attributes = root.attributes
        assertEquals(attributes["attributeName"], "attributeValue")
    }

    @Test
    fun getChildren() {
        val childEntity = XmlEntity("childEntity")
        root.appendChild(childEntity)
        val children = root.children
        assertTrue(children.contains(childEntity))
    }

    @Test
    fun appendAttribute() {
        root.appendAttribute("attributeName", "attributeValue")
    }

    @Test
    fun removeAttribute() {
        root.appendAttribute("attributeName", "attributeValue")
        root.removeAttribute("attributeName")
    }

    @Test
    fun appendChild() {
        val childEntity = XmlEntity("childEntity")
        root.appendChild(childEntity)
        assertSame(childEntity.parent, root)
    }

    @Test
    fun removeChild() {
        val childEntity = XmlEntity("childEntity")
        root.appendChild(childEntity)
        root.removeChild(childEntity)
        assertNull(childEntity.parent)
    }

    @Test
    fun accept() {
        val childEntity = XmlEntity("childEntity")
        val textNode = XmlText("This is a text node.")
        childEntity.appendChild(textNode)
        root.appendChild(childEntity)

        val v = object : XmlVisitor {
            val nodes = mutableListOf<XmlNode>()
            override fun visit(e: XmlText) {
                nodes.add(e)
            }
            override fun visit(e: XmlEntity): Boolean {
                nodes.add(e)
                return true
            }
        }
        root.accept(v)

        assertArrayEquals(arrayOf(root, childEntity, textNode), v.nodes.toTypedArray())
    }

}