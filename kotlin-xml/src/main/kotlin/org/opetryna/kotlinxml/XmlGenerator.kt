package org.opetryna.kotlinxml

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
/**
 * Use this name instead of the declared property name.
 */
annotation class XmlName(val name: String)

@Target(AnnotationTarget.PROPERTY)
/**
 * The property will be represented as an XML attribute.
 */
annotation class XmlAttribute

@Target(AnnotationTarget.CLASS)
/**
 * The property will be represented as an XmlText node.
 */
annotation class XmlString

@Target(AnnotationTarget.PROPERTY)
/**
 * The property will not be included in the XML model.
 */
annotation class XmlIgnore

/**
 * Presents functions for generating new XmlEntities.
 */
class XmlGenerator {

    companion object {

        private val primitiveTypes: List<KClass<*>> = listOf(
            Byte::class, Short::class, Int::class, Long::class,
            Float::class, Double::class, Boolean::class,
            Char::class, String::class
        )

        private fun retrieveClassName(c: KClass<*>): String {
            val name =
                if (c.hasAnnotation<XmlName>())
                    c.findAnnotation<XmlName>()?.name
                else
                    c.simpleName
            if (name == null)
                throw Exception("Given class does not have a name.")
            return name
        }

        private fun retrievePropertyName(p: KProperty<*>): String {
            val name =
                if (p.hasAnnotation<XmlName>())
                    p.findAnnotation<XmlName>()?.name
                else
                    p.name
            if (name == null)
                throw Exception("Given property does not have a name.")
            return name
        }

        /**
         * Generates an XML representation of the given object.
         *
         * @param o Object to be represented in XML.
         * @param name XmlEntity name of the object.
         *
         * @return XmlEntity structure.
         */
        fun generate(o: Any, name: String = retrieveClassName(o::class)): XmlEntity {

            fun processObject(o: Any, parent: XmlEntity) {

                if (o::class.hasAnnotation<XmlString>() || primitiveTypes.contains(o::class)) {
                    parent.appendChild(XmlText(o.toString()))

                } else if (o is Collection<*>) {
                    o.filterNotNull().forEach { item ->
                        val itemEntity = XmlEntity(retrieveClassName(item::class))
                        processObject(item, itemEntity)
                        parent.appendChild(itemEntity)
                    }

                } else if (o is Map<*, *>) {
                    o.forEach { (key, value) ->
                        if (key != null && value != null) {
                            val keyEntity = XmlEntity(retrieveClassName(key::class))
                            processObject(key, keyEntity)
                            val valueEntity = XmlEntity(retrieveClassName(value::class))
                            processObject(value, valueEntity)
                            val entryEntity = XmlEntity("Pair")
                            entryEntity.appendChild(keyEntity)
                            entryEntity.appendChild(valueEntity)
                            parent.appendChild(entryEntity)
                        }
                    }

                } else {
                    o::class.declaredMemberProperties.forEach { p ->
                        if (! p.hasAnnotation<XmlIgnore>()) {
                            p.call(o)?.let {
                                if (p.hasAnnotation<XmlAttribute>()) {
                                    parent.appendAttribute(retrievePropertyName(p), it.toString())
                                } else {
                                    val propertyEntity = XmlEntity(retrievePropertyName(p))
                                    processObject(it, propertyEntity)
                                    parent.appendChild(propertyEntity)
                                }
                            }
                        }
                    }
                }
            }

            val root = XmlEntity(name)
            processObject(o, root)
            return root
        }

        /**
         * Generates a new XmlEntity structure based on the given filtration criteria.
         *
         * @param src Source XmlEntity
         * @param accept Acceptation criteria function.
         *
         * @return Filtered XmlEntity structure.
         */
        fun filter(src: XmlEntity, accept: (XmlNode) -> Boolean): XmlEntity? {

            val v = object : XmlVisitor {

                var root: XmlEntity? = null
                var current: XmlEntity? = null
                val accepted: ArrayDeque<Boolean> = ArrayDeque()

                override fun visit(e: XmlText) {
                    if (accept(e)) {
                        val copy = XmlText(e)
                        current?.appendChild(copy)
                    }
                }

                override fun visit(e: XmlEntity): Boolean {
                    val copy = XmlEntity(e)
                    if (root == null) {
                        root = copy
                    }
                    accepted.addLast(accept(e))
                    current?.appendChild(copy)
                    current = copy
                    return true
                }

                override fun endVisit(e: XmlEntity) {
                    val parent = current?.parent ?: root
                    if (! accepted.removeLast() && current?.children?.isEmpty()!!) {
                        if (current == root)
                            root = null
                        else
                            parent?.removeChild(current!!)

                    }
                    current = parent
                }

            }

            src.accept(v)
            return v.root
        }

    }

}