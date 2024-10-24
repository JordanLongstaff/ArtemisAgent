package com.walkertribe.ian.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class ListenerArgumentProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private val visitor: ListenerArgumentVisitor by lazy { ListenerArgumentVisitor(codeGenerator) }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val argumentClasses = resolver.getNewFiles().flatMap { file ->
            file.declarations.filterIsInstance<KSClassDeclaration>().flatMap { cls ->
                listOf(cls) + cls.declarations.filterIsInstance<KSClassDeclaration>()
            }.filter { cls ->
                cls.superTypes.any { it.toString() == "ListenerArgument" }
            }
        }
        argumentClasses.forEach { it.accept(visitor, Unit) }

        return emptyList()
    }
}
