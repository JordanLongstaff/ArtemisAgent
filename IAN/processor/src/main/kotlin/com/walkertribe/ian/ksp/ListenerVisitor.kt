package com.walkertribe.ian.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.walkertribe.ian.iface.ListenerArgument

class ListenerVisitor(
    private val codeGenerator: CodeGenerator,
    private val functions: List<KSFunctionDeclaration>,
) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val containingFile = classDeclaration.containingFile ?: return
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val fileName = "${className}Listeners"

        val listenerFunctionClass = ClassName("com.walkertribe.ian.iface", "ListenerFunction")

        val listenerModuleBuilder = PropertySpec.builder(
            "listeners",
            List::class.asClassName().plusParameter(
                listenerFunctionClass.plusParameter(
                    WildcardTypeName.producerOf(ListenerArgument::class),
                ),
            ),
        ).receiver(classDeclaration.toClassName()).addOriginatingKSFile(containingFile)

        val moduleGetterSpec = FunSpec.getterBuilder()
            .addStatement(
                "return listOf(%L)",
                functions.joinToString { fn ->
                    fn.parameters.joinToString { param ->
                        val paramClassName = param.type.resolve().declaration.simpleName.asString()
                        "\nListenerFunction($paramClassName::class, this::$fn)"
                    }
                },
            )

        val fileSpecBuilder = FileSpec.builder(packageName, fileName)
            .addProperty(listenerModuleBuilder.getter(moduleGetterSpec.build()).build())

        functions.flatMap { it.parameters }.forEach { param ->
            val paramTypeName = param.type.toTypeName() as ClassName
            fileSpecBuilder.addImport(
                paramTypeName.enclosingClassName()?.canonicalName ?: paramTypeName.packageName,
                paramTypeName.simpleName,
            )
        }

        fileSpecBuilder.build().writeTo(codeGenerator, aggregating = false)
    }
}
