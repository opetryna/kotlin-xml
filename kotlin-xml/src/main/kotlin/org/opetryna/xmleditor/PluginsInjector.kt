package org.opetryna.xmleditor

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

@Target(AnnotationTarget.PROPERTY)
annotation class Inject

@Target(AnnotationTarget.PROPERTY)
annotation class InjectAdd

interface Injectable {
    fun injectionCompleted()
}

class Injector() {

    companion object {

        private val config: Map<String, String> = mutableMapOf()

        fun readConfig(configFile: File) {
            configFile.useLines { lines -> lines.forEach {
                val entry = it.split("=")
                if (entry.size == 2)
                    (this.config as MutableMap)[entry[0]] = entry[1]
                else
                    System.err.println("Config error: $it")
            } }
        }

        fun readConfig(config: String) {
            config.split("\n").forEach {
                val entry = it.split("=")
                if (entry.size == 2)
                    (this.config as MutableMap)[entry[0]] = entry[1]
                else
                    System.err.println("Config error: $it")
            }
        }

        fun <T:Any> create(c: KClass<T>, vararg args: Any?): T {
            val obj = c.primaryConstructor!!.call(*args)
            c.declaredMemberProperties.filter { it.hasAnnotation<Inject>() }.forEach {
                val className = config["${c.simpleName}.${it.name}"]
                if (className != null) {
                    val instance = Class.forName(className).kotlin.createInstance()
                    it.isAccessible = true
                    (it as KMutableProperty<*>).setter.call(obj, instance)
                }
            }
            c.declaredMemberProperties.filter { it.hasAnnotation<InjectAdd>() }.forEach {
                val classNames = config["${c.simpleName}.${it.name}"]
                classNames?.split(",")?.forEach { className ->
                    val instance = Class.forName(className).kotlin.createInstance()
                    it.isAccessible = true
                    (it.call(obj) as MutableList<Any>).add(instance)
                }
            }
            c.declaredFunctions.forEach {
                if (it.name == "injectionCompleted") {
                    it.call(obj)
                }
            }
            return obj
        }
    }

}