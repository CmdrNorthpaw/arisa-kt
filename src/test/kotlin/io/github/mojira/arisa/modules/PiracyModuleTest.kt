package io.github.mojira.arisa.modules

import arrow.core.left
import arrow.core.right
import io.github.mojira.arisa.modules.PiracyModule.Request
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class PiracyModuleTest : StringSpec({
    "should return OperationNotNeededModuleResponse when there is no description, summary or environment" {
        val module = PiracyModule(listOf("test"))
        val request = Request(null, null, null, { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when description, summary and environment are empty" {
        val module = PiracyModule(listOf("test"))
        val request = Request("", "", "", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when piracy signatures is empty" {
        val module = PiracyModule(emptyList())
        val request = Request("", "", "test", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should return OperationNotNeededModuleResponse when no signature matches" {
        val module = PiracyModule(emptyList())
        val request = Request("else", "nope", "something", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if description contains a piracy signature" {
        val module = PiracyModule(listOf("test"))
        val request = Request("", "", "test", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return OperationNotNeededModuleResponse if description contains a piracy signature but not as a full word" {
        val module = PiracyModule(listOf("test"))
        val request = Request("", "", "testusername", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft(OperationNotNeededModuleResponse)
    }

    "should resolve as invalid if summary contains a piracy signature" {
        val module = PiracyModule(listOf("test"))
        val request = Request("", "test", "", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid if environment contains a piracy signature" {
        val module = PiracyModule(listOf("test"))
        val request = Request("test", "", "", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should resolve as invalid if environment contains a piracy signature using whitespaces" {
        val module = PiracyModule(listOf("signature with whitespaces"))
        val request = Request("signature with whitespaces", "", "", { Unit.right() }, { Unit.right() })

        val result = module(request)

        result.shouldBeRight(ModuleResponse)
    }

    "should return FailedModuleResponse when resolving as invalid fails" {
        val module = PiracyModule(listOf("test"))
        val request = Request("test", "", "", { RuntimeException().left() }, { Unit.right() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }

    "should return FailedModuleResponse when adding comment fails" {
        val module = PiracyModule(listOf("test"))
        val request = Request("test", "", "", { Unit.right() }, { RuntimeException().left() })

        val result = module(request)

        result.shouldBeLeft()
        result.a should { it is FailedModuleResponse }
        (result.a as FailedModuleResponse).exceptions.size shouldBe 1
    }
})
