package org.opetryna.kotlinxml

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class XmlName(val name: String)

@Target(AnnotationTarget.PROPERTY)
annotation class XmlIgnore

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
                throw Exception("Class does not have a name.")
            return name
        }

        private fun retrievePropertyName(p: KProperty<*>): String {
            val name =
                if (p.hasAnnotation<XmlName>())
                    p.findAnnotation<XmlName>()?.name
                else
                    p.name
            if (name == null)
                throw Exception("Property does not have a name.")
            return name
        }

        fun generate(o: Any, name: String = retrieveClassName(o::class)): XmlEntity {

            fun processObject(o: Any, parent: XmlEntity) {

                if (primitiveTypes.contains(o::class)) {
                    parent.appendChild(XmlText(o.toString()))

                } else if (o is Iterable<*>) {
                    o.filterNotNull().forEach { item ->
                        val itemEntity = XmlEntity("item")
                        processObject(item, itemEntity)
                        parent.appendChild(itemEntity)
                    }

                } else if (o is Map<*, *>) {
                    o.forEach { (key, value) ->
                        if (key != null && value != null) {
                            val keyEntity = XmlEntity("key")
                            processObject(key, keyEntity)
                            val valueEntity = XmlEntity("value")
                            processObject(value, valueEntity)
                            val entryEntity = XmlEntity("entry")
                            entryEntity.appendChild(keyEntity)
                            entryEntity.appendChild(valueEntity)
                            parent.appendChild(entryEntity)
                        }
                    }

                } else {
                    o::class.declaredMemberProperties.filterNotNull().forEach { p ->
                        if (! p.hasAnnotation<XmlIgnore>()) {
                            p.call(o)?.let {
                                val propertyEntity = XmlEntity(retrievePropertyName(p))
                                processObject(it, propertyEntity)
                                parent.appendChild(propertyEntity)
                            }
                        }
                    }
                }

            }

            val root = XmlEntity(name)
            processObject(o, root)
            return root
        }

    }

}