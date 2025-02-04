package com.walkertribe.ian.protocol.core

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

class CorePacketTypeKonsistTest :
    DescribeSpec({
      describe("CorePacketType") {
        Konsist.scopeFromModule("IAN/packets")
            .objects()
            .withRepresentedTypeOf(CorePacketType::class)
            .flatMap { it.properties() }
            .forEach { decl ->
              val name = decl.name

              var uppercase = false
              val expectedValue =
                  name.toCharArray().joinToString("") {
                    when {
                      uppercase -> {
                        uppercase = false
                        it.toString()
                      }
                      it == '_' -> {
                        uppercase = true
                        ""
                      }
                      else -> it.lowercase()
                    }
                  }

              it("$name = \"$expectedValue\"") {
                decl.assertTrue { it.hasConstModifier && it.isVal && it.value == expectedValue }
              }
            }
      }
    })
