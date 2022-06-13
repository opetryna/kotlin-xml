package org.opetryna.xmleditor

import org.opetryna.kotlinxml.XmlAttribute
import org.opetryna.kotlinxml.XmlDocument
import org.opetryna.kotlinxml.XmlEntity
import org.opetryna.kotlinxml.XmlGenerator
import javax.swing.JComboBox
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

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

internal class AddCourse : EntityComponent.Action {

    override val name = "Add Course"
    override lateinit var entityComponent: EntityComponent
    override val isEnabled: Boolean
        get() = entityComponent.entity.name == "courses" && entityComponent.entity.parent?.name == "Student"
    private lateinit var courseEntity: XmlEntity

    override fun execute() {
        courseEntity = XmlGenerator.generate(Course("", "", 2022))
        entityComponent.entity.appendChild(courseEntity)
    }

    override fun undo() {
        entityComponent.entity.removeChild(courseEntity)
    }

}

internal class DuplicateCourse : EntityComponent.Action {

    override val name = "Duplicate Course"
    override lateinit var entityComponent: EntityComponent
    override val isEnabled: Boolean
        get() = entityComponent.entity.name == "Course"
    private lateinit var courseEntity: XmlEntity

    override fun execute() {
        courseEntity = XmlEntity(entityComponent.entity)
        entityComponent.entity.parent?.appendChild(courseEntity)
    }

    override fun undo() {
        entityComponent.entity.parent?.removeChild(courseEntity)
    }

}

internal class CourseTypeView : AttributesComponent.AttributeViewTemplate {

    override lateinit var attributesComponent: AttributesComponent
    override val entityName = "Course"
    override val attributeName = "type"

    override fun create(name: String, value: String): AttributesComponent.AttributeViewTemplate.AttributeView {
        val dropdown = JComboBox<String>(arrayOf("BSc", "MSc", "PhD"))
        dropdown.addActionListener {
            if (attributesComponent.parent.entity.attributes[name] != dropdown.selectedItem)
                attributesComponent.parent.notifyObservers {
                    it.updateAttribute(attributesComponent.parent.entity, name, dropdown.selectedItem.toString())
                }
        }
        dropdown.selectedItem = value
        return object : AttributesComponent.AttributeViewTemplate.AttributeView {
            override val component: JComboBox<String> = dropdown
            override fun updateValue(value: String) {
                component.selectedItem = value
            }
        }
    }
}

internal class CourseYearView : AttributesComponent.AttributeViewTemplate {

    override lateinit var attributesComponent: AttributesComponent
    override val entityName = "Course"
    override val attributeName = "year"

    override fun create(name: String, value: String): AttributesComponent.AttributeViewTemplate.AttributeView {
        val spinner = JSpinner(SpinnerNumberModel(2022, null, null, 1))
        spinner.addChangeListener {
            attributesComponent.parent.notifyObservers {
                it.updateAttribute(attributesComponent.parent.entity, name, spinner.value.toString())
            }
        }
        return object : AttributesComponent.AttributeViewTemplate.AttributeView {
            override val component: JSpinner = spinner
            override fun updateValue(value: String) {
                spinner.value = value.toInt()
            }
        }
    }
}

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

    Injector.readConfig("" +
            "EntityComponent.actions=org.opetryna.xmleditor.AddCourse,org.opetryna.xmleditor.DuplicateCourse\n" +
            "AttributesComponent.views=org.opetryna.xmleditor.CourseTypeView,org.opetryna.xmleditor.CourseYearView"
    )
    val xmlEditor = XmlEditor()
    xmlEditor.loadDocument(xmlStudent)
    xmlEditor.open()
}