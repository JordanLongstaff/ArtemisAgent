package com.walkertribe.ian.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class PacketVisitor(private val codeGenerator: CodeGenerator) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val containingFile = classDeclaration.containingFile ?: return
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val parentName = classDeclaration.parentDeclaration?.run { simpleName.asString() }
        val fileName = PacketProcessor.makeFactoryClassName(classDeclaration)
        val fullPacketName = listOfNotNull(parentName, className).joinToString(".")

        val factoryClassName = ClassName(PacketProcessor.MAIN_PACKAGE, "PacketFactory")
        val packetClassName = ClassName(packageName, fullPacketName.split("."))
        val packetReaderClassName = ClassName("com.walkertribe.ian.iface", "PacketReader")

        val objectBuilder =
            TypeSpec.objectBuilder(fileName)
                .addModifiers(KModifier.INTERNAL)
                .addSuperinterface(factoryClassName.plusParameter(packetClassName))
                .addOriginatingKSFile(containingFile)

        val factoryClassBuilder =
            PropertySpec.builder(
                    "factoryClass",
                    KClass::class.asTypeName().plusParameter(packetClassName),
                    KModifier.OVERRIDE,
                )
                .initializer("%T::class", packetClassName)

        val buildFunBuilder =
            FunSpec.builder("build")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("reader", packetReaderClassName)
                .returns(packetClassName)
                .addStatement("return %T(reader)", packetClassName)

        FileSpec.builder(packageName, fileName)
            .addType(
                objectBuilder
                    .addProperty(factoryClassBuilder.build())
                    .addFunction(buildFunBuilder.build())
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = false)
    }
}
