package org.opetryna.kotlinxml

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class XmlTextTest {

    private val textNode = XmlText("This is a text node.")

    @Test
    fun getValue() {
        assertEquals("This is a text node.", textNode.value)
    }

    @Test
    fun setValue() {
        textNode.value = "This is a new value text node."
        assertEquals("This is a new value text node.", textNode.value)
    }

    @Test
    fun accept() {
        val v = object : XmlVisitor {
            var textNode: XmlText? = null
            override fun visit(e: XmlText) {
                textNode = e
            }
        }
        textNode.accept(v)
        assertSame(v.textNode, textNode)
    }
}