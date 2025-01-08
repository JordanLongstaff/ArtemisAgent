package com.walkertribe.ian.vesseldata

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withoutName
import com.lemonappdev.konsist.api.verify.assertTrue
import hasTypeOf
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import korlibs.io.serialization.xml.Xml

class VesselDataKonsistTest :
    DescribeSpec({
      val vesseldata =
          Konsist.scopeFromPackage("com.walkertribe.ian.vesseldata", "IAN/vesseldata", "main")
      val classes = vesseldata.classes().withoutName("Error")

      describe("Vessel data element classes have Xml constructor") {
        withData(nameFn = { it.name }, classes) { cls ->
          cls.assertTrue {
            it.hasConstructor { constr ->
              constr.numParameters == 1 &&
                  constr.hasAllParameters { param -> param.hasTypeOf<Xml>() }
            }
          }
        }
      }
    })
