package com.walkertribe.ian.iface

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withTopLevel
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData

class ListenerKonsistTest :
    DescribeSpec({
      val listenerScope = Konsist.scopeFromProject("IAN/listener", "main")
      val allTypes = listenerScope.classes() + listenerScope.interfaces() + listenerScope.objects()

      describe("All type names begin with Listener") {
        withData(nameFn = { it.name }, allTypes.withTopLevel()) { cls ->
          cls.assertTrue { it.hasNameStartingWith("Listener") }
        }
      }
    })
