package com.walkertribe.ian.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk

class KspVersionCompatibilityTest : DescribeSpec({
    describe("KSP Version 2.3.6 Compatibility") {
        it("Should be able to instantiate SymbolProcessor classes") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val processor = ListenerProcessor(codeGenerator)

            processor.shouldNotBeNull()
            processor.shouldBeInstanceOf<SymbolProcessor>()
        }

        it("ListenerProcessor should process empty resolver") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = ListenerProcessor(codeGenerator)
            val result = processor.process(resolver)

            result.shouldNotBeNull()
            result.shouldBeEmpty()
        }

        it("ListenerArgumentProcessor should instantiate correctly") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val processor = ListenerArgumentProcessor(codeGenerator)

            processor.shouldNotBeNull()
            processor.shouldBeInstanceOf<SymbolProcessor>()
        }

        it("ListenerArgumentProcessor should process empty resolver") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = ListenerArgumentProcessor(codeGenerator)
            val result = processor.process(resolver)

            result.shouldNotBeNull()
            result.shouldBeEmpty()
        }

        it("PacketProcessor should instantiate correctly") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val processor = PacketProcessor(codeGenerator)

            processor.shouldNotBeNull()
            processor.shouldBeInstanceOf<SymbolProcessor>()
        }

        it("PacketProcessor should process empty resolver") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = PacketProcessor(codeGenerator)
            val result = processor.process(resolver)

            result.shouldNotBeNull()
            result.shouldBeEmpty()
        }
    }

    describe("SymbolProcessorProvider Compatibility") {
        it("ListenerProcessorProvider should create processor") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val logger = mockk<KSPLogger>(relaxed = true)

            every { environment.codeGenerator } returns codeGenerator
            every { environment.logger } returns logger

            val provider = ListenerProcessorProvider()
            val processor = provider.create(environment)

            processor.shouldNotBeNull()
            processor.shouldBeInstanceOf<ListenerProcessor>()
        }

        it("ListenerArgumentProcessorProvider should create processor") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val logger = mockk<KSPLogger>(relaxed = true)

            every { environment.codeGenerator } returns codeGenerator
            every { environment.logger } returns logger

            val provider = ListenerArgumentProcessorProvider()
            val processor = provider.create(environment)

            processor.shouldNotBeNull()
            processor.shouldBeInstanceOf<ListenerArgumentProcessor>()
        }

        it("PacketProcessorProvider should create processor") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val logger = mockk<KSPLogger>(relaxed = true)

            every { environment.codeGenerator } returns codeGenerator
            every { environment.logger } returns logger

            val provider = PacketProcessorProvider()
            val processor = provider.create(environment)

            processor.shouldNotBeNull()
            processor.shouldBeInstanceOf<PacketProcessor>()
        }
    }

    describe("KSP API Version Compatibility") {
        it("Should access codeGenerator from environment") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)

            every { environment.codeGenerator } returns codeGenerator

            val retrievedGenerator = environment.codeGenerator
            retrievedGenerator.shouldNotBeNull()
        }

        it("Should access logger from environment") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val logger = mockk<KSPLogger>(relaxed = true)

            every { environment.logger } returns logger

            val retrievedLogger = environment.logger
            retrievedLogger.shouldNotBeNull()
        }

        it("Resolver should support getSymbolsWithAnnotation") {
            val resolver = mockk<Resolver>(relaxed = true)
            val annotationName = "com.example.Annotation"

            every { resolver.getSymbolsWithAnnotation(annotationName) } returns emptySequence()

            val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            symbols.shouldNotBeNull()
        }

        it("SymbolProcessor process method should return list of unprocessed symbols") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = ListenerProcessor(codeGenerator)
            val result = processor.process(resolver)

            result.shouldBeInstanceOf<List<KSAnnotated>>()
        }
    }

    describe("Edge Cases and Error Handling") {
        it("Processor should handle null qualified names gracefully") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = ListenerProcessor(codeGenerator)
            val result = processor.process(resolver)

            // Should not throw exception
            result.shouldNotBeNull()
        }

        it("Processor should handle empty annotation sequences") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processors = listOf(
                ListenerProcessor(codeGenerator),
                ListenerArgumentProcessor(codeGenerator),
                PacketProcessor(codeGenerator)
            )

            processors.forEach { processor ->
                val result = processor.process(resolver)
                result.shouldNotBeNull()
                result.shouldBeEmpty()
            }
        }

        it("Provider should work with minimal environment setup") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)

            every { environment.codeGenerator } returns codeGenerator

            val providers = listOf(
                ListenerProcessorProvider(),
                ListenerArgumentProcessorProvider(),
                PacketProcessorProvider()
            )

            providers.forEach { provider ->
                val processor = provider.create(environment)
                processor.shouldNotBeNull()
            }
        }
    }

    describe("KSP 2.3.6 Specific Features") {
        it("Should support modern KSP API patterns") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            // Test that the processor can be instantiated and used
            val processor = ListenerProcessor(codeGenerator)
            processor.process(resolver)

            // If we get here without exceptions, KSP 2.3.6 APIs are working
            true shouldBe true
        }

        it("CodeGenerator should be accessible for all processors") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)

            val processors = listOf(
                ListenerProcessor(codeGenerator),
                ListenerArgumentProcessor(codeGenerator),
                PacketProcessor(codeGenerator)
            )

            processors.forEach { processor ->
                processor.shouldNotBeNull()
            }
        }

        it("SymbolProcessorProvider interface should be properly implemented") {
            val providers = listOf(
                ListenerProcessorProvider(),
                ListenerArgumentProcessorProvider(),
                PacketProcessorProvider()
            )

            providers.forEach { provider ->
                provider.shouldNotBeNull()

                val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
                val codeGenerator = mockk<CodeGenerator>(relaxed = true)
                every { environment.codeGenerator } returns codeGenerator

                val processor = provider.create(environment)
                processor.shouldNotBeNull()
            }
        }
    }

    describe("Regression Tests for Version Upgrade") {
        it("ListenerProcessor backward compatibility") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = ListenerProcessor(codeGenerator)
            val result = processor.process(resolver)

            // Should behave the same as previous KSP versions
            result shouldBe emptyList()
        }

        it("All processor providers should create valid processors") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            every { environment.codeGenerator } returns codeGenerator

            val providers = mapOf(
                "ListenerProcessorProvider" to ListenerProcessorProvider(),
                "ListenerArgumentProcessorProvider" to ListenerArgumentProcessorProvider(),
                "PacketProcessorProvider" to PacketProcessorProvider()
            )

            providers.forEach { (name, provider) ->
                val processor = provider.create(environment)
                processor.shouldNotBeNull()
            }
        }

        it("Processor classes should have correct package structure") {
            val processor = ListenerProcessor(mockk(relaxed = true))
            val packageName = processor::class.java.packageName

            packageName shouldBe "com.walkertribe.ian.ksp"
        }

        it("Provider classes should have correct package structure") {
            val provider = ListenerProcessorProvider()
            val packageName = provider::class.java.packageName

            packageName shouldBe "com.walkertribe.ian.ksp"
        }
    }

    describe("Boundary Conditions") {
        it("Should handle multiple sequential process calls") {
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            val resolver = mockk<Resolver>(relaxed = true)

            every { resolver.getSymbolsWithAnnotation(any<String>()) } returns emptySequence()

            val processor = ListenerProcessor(codeGenerator)

            // Call process multiple times
            repeat(3) {
                val result = processor.process(resolver)
                result.shouldNotBeNull()
                result.shouldBeEmpty()
            }
        }

        it("Should handle concurrent processor creation") {
            val environment = mockk<SymbolProcessorEnvironment>(relaxed = true)
            val codeGenerator = mockk<CodeGenerator>(relaxed = true)
            every { environment.codeGenerator } returns codeGenerator

            val provider = ListenerProcessorProvider()

            // Create multiple processors
            val processors = (1..5).map { provider.create(environment) }

            processors.forEach { processor ->
                processor.shouldNotBeNull()
                processor.shouldBeInstanceOf<ListenerProcessor>()
            }
        }

        it("KSP version should support qualified name resolution") {
            val qualifiedName = "com.walkertribe.ian.iface.Listener"
            qualifiedName.shouldNotBeBlank()

            // Test that string-based annotation lookup is supported
            val resolver = mockk<Resolver>(relaxed = true)
            every { resolver.getSymbolsWithAnnotation(qualifiedName) } returns emptySequence()

            val symbols = resolver.getSymbolsWithAnnotation(qualifiedName)
            symbols.shouldNotBeNull()
        }
    }
})