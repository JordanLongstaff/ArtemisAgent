package com.walkertribe.ian.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.iface.ListenerArgument

class ListenerProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val listenerClassName = Listener::class.qualifiedName ?: return emptyList()

        val functionSymbols =
            resolver
                .getSymbolsWithAnnotation(listenerClassName)
                .filterIsInstance<KSFunctionDeclaration>()
                .groupBy { it.parentDeclaration?.qualifiedName?.asString() ?: "" }
                .toSortedMap()

        val classSymbols =
            functionSymbols.keys.mapNotNull { resolver.getClassDeclarationByName(it) }

        classSymbols.forEach {
            val functions = it.qualifiedName?.run { functionSymbols[asString()] } ?: return@forEach
            val visitor = ListenerVisitor(codeGenerator, functions)
            it.accept(visitor, Unit)
        }

        val defaultFunctions = functionSymbols[""].orEmpty()
        val defaultFunctionsGrouped =
            defaultFunctions.groupBy { it.packageName.asString() }.toSortedMap()
        val listenerFunctionClass = ClassName("com.walkertribe.ian.iface", "ListenerFunction")

        defaultFunctionsGrouped.forEach { (packageName, functions) ->
            val fileSpecBuilder =
                FileSpec.builder(packageName, "DefaultListenersKt")
                    .addImport(listenerFunctionClass.packageName, listenerFunctionClass.simpleName)

            functions
                .flatMap { it.parameters }
                .forEach { param ->
                    val paramTypeName = param.type.toTypeName() as ClassName
                    fileSpecBuilder.addImport(
                        paramTypeName.enclosingClassName()?.canonicalName
                            ?: paramTypeName.packageName,
                        paramTypeName.simpleName,
                    )
                }

            val moduleGetterSpec =
                FunSpec.getterBuilder()
                    .addStatement(
                        "return listOf(%L)",
                        functions.joinToString { fn ->
                            fn.parameters.joinToString { param ->
                                "ListenerFunction<${param.type.toTypeName()}>(::$fn)"
                            }
                        },
                    )

            val listenerModuleBuilder =
                PropertySpec.builder(
                        "defaultListeners",
                        List::class.asClassName()
                            .plusParameter(
                                listenerFunctionClass.plusParameter(
                                    ListenerArgument::class.asTypeName()
                                )
                            ),
                    )
                    .getter(moduleGetterSpec.build())

            fileSpecBuilder.addProperty(listenerModuleBuilder.build())

            fileSpecBuilder.build().writeTo(codeGenerator = codeGenerator, aggregating = false)
        }

        return emptyList()
    }
}
