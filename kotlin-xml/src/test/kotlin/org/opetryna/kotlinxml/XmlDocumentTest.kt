package org.opetryna.kotlinxml

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class XmlDocumentTest {

    @Test
    fun serialize() {

        val root = XmlEntity("root")
        root.appendAttribute("attributeName", "attributeValue")
        val childEntity = XmlEntity("childEntity")
        val textNode = XmlText("This is a text node.")
        childEntity.appendChild(textNode)
        root.appendChild(childEntity)
        val emptyEntity = XmlEntity("emptyEntity")
        root.appendChild(emptyEntity)
        val document = XmlDocument(root)

        val expectedXml = """<?xml version="1.0" ?>
                            |<root attributeName="attributeValue">
                            |    <childEntity>This is a text node.</childEntity>
                            |    <emptyEntity/>
                            |</root>""".trimMargin()

        val serializedXml = document.serialize().replace("\t", "    ")

        assertEquals(expectedXml, serializedXml)
    }

    @Test
    fun getRoot() {

        val root = XmlEntity("root")
        val document = XmlDocument(root)

        assertSame(root, document.root)
    }
}