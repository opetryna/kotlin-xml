# kotlin-xml
Kotlin XML project contains:
 - **XML Model**: for manipulation of XML data structures.
 - **XML Generator**: for generation of XML documents out of Kotlin objects using automatic inference.
 - **XML Editor**: a GUI application for operations on XML Model, which can be extended via plugins.
 
## XML Model
The XML Model consists of the following classes:
 - **XmlDocument**: represents an XML Document, contains a root XmlEntity and a method for serialization of the document to text.
 - **XmlNode**: abstract class reprsenting an XML Node, can be an XmlEntity or XmlText.
 - **XmlEntity**: represents an XML Entity, contains a name, a list of attributes in format of key-value and a list of nested XmlNodes.
 - **XmlText**: represents an XML text node, contains a String type value.
 - **XmlVisitor**: an interface for traversal of the XML data structure, is passed to a XmlNode by the *accept* method.
 
### Usage example:
```     
        val root = XmlEntity("root")
        root.appendAttribute("attributeName", "attributeValue")
        val childEntity = XmlEntity("childEntity")
        val textNode = XmlText("This is a text node.")
        childEntity.appendChild(textNode)
        root.appendChild(childEntity)
        val emptyEntity = XmlEntity("emptyEntity")
        root.appendChild(emptyEntity)
        val document = XmlDocument(root)
        println(document.serialize())
```
Would produce the following output:
```     
<?xml version="1.0" ?>
<root attributeName="attributeValue">
    <childEntity>This is a text node.</childEntity>
    <emptyEntity/>
</root>"
```

## XML Generator
The XML Genearator is a static class providing functions for automatic XML inference and the following annotations:
 - **XmlName**: to set a specific name of the object in the resulting XML Model.
 - **XmlAttribute**: to indicate that a property should be represented as an attribute (the default is as an entity).
 - **XmlString**: to indicate that a class should be represented as a XmlText node (by calling toString on the object).
 - **XmlIngnore**: to indicate that a property should not be included in the XML Model.
 
### Usage example:
```     
internal data class Course(
    @XmlAttribute
    val name: String,
    @XmlAttribute
    val type: String,
    @XmlAttribute
    val year: Int
)

internal data class Student(
    @XmlAttribute
    val number: Int,
    @XmlAttribute
    val name: String,
    val courses: MutableList<Course>
)

internal fun main() {

    val course = Course(
        "Telecommunications and Computer Engineering",
        "MSc",
        2022
    )
    val student = Student(
        73132,
        "Oleh Petryna",
        mutableListOf(course)
    )

    val xmlStudent = XmlDocument(XmlGenerator.generate(student))
    println(xmlStudent.serialize())
}
```
Would produce the following output:
```     
<?xml version="1.0" ?>
<Student name="Oleh Petryna" number="73132">
	<courses>
		<Course name="Telecommunications and Computer Engineering" type="MSc" year="2022"/>
	</courses>
</Student>
```

## XML Editor
The XML Editor provides two interfaces for implementing plugins:
 - **EntityComponent.Action**: for implementing custom context-menu actions on XML Entities. Has a *lateinit var EntityComponent* which will be instantiated with the EntityComponent on which the plugin will perform operations. Must implement *execute()* and *undo()* methods and override the *name* attribute.
 - **AttributesComponent.AttributeViewTemplate**: for implementing custom views on XML Attributes. Must implement a *create(name: String, value: String)* function which returns a *AttributesComponent.AttributeViewTemplate.AttributeView* object which must contain a *val component: JComponent* property and a *updateValue(value: String)* method.
 
Check an example implementation at ***kotlin-xml/src/test/kotlin/org/opetryna/xmleditor/XmlEditorPluginsExample.kt***
