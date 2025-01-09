package com.walkertribe.ian.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import com.walkertribe.ian.iface.ListenerArgument
import com.walkertribe.ian.iface.ListenerFunction
import com.walkertribe.ian.iface.ListenerModule

class ListenerArgumentVisitor(private val codeGenerator: CodeGenerator) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val containingFile = classDeclaration.containingFile ?: return
        val argClassName =
            (classDeclaration.parentDeclaration ?: classDeclaration).simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val argModuleName = "${argClassName}ListenerModule"

        val argClassNameLowercase = argClassName.let { it[0].lowercase() + it.substring(1) }
        val listPropertyName = "${argClassNameLowercase}Listeners"

        val listenerArgumentTypeName =
            classDeclaration.toClassName().let {
                if (classDeclaration.typeParameters.isEmpty()) it
                else it.parameterizedBy(classDeclaration.typeParameters.map { STAR })
            }

        val overrideFunBuilder =
            FunSpec.builder("on$argClassName")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("arg", ListenerArgument::class.asClassName())
                .beginControlFlow("if (arg is %T)", listenerArgumentTypeName)
                .addStatement("%L.forEach { it.offer(arg) }", listPropertyName)
                .endControlFlow()

        val listenerModuleBuilder =
            TypeSpec.interfaceBuilder(argModuleName)
                .addSuperinterface(ListenerModule::class)
                .addProperty(
                    listPropertyName,
                    List::class.asClassName()
                        .plusParameter(
                            ListenerFunction::class.asClassName()
                                .plusParameter(
                                    WildcardTypeName.producerOf(listenerArgumentTypeName)
                                )
                        ),
                )
                .addFunction(overrideFunBuilder.build())
                .addOriginatingKSFile(containingFile)

        FileSpec.builder(packageName, argModuleName)
            .addType(listenerModuleBuilder.build())
            .build()
            .writeTo(codeGenerator, aggregating = false)
    }
}
