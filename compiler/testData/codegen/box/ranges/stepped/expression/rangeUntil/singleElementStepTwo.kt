// Auto-generated by GenerateSteppedRangesCodegenTestData. Do not edit!
// WITH_STDLIB
// LANGUAGE: +RangeUntilOperator
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.test.*

fun box(): String {
    val intList = mutableListOf<Int>()
    val intProgression = 1..<2
    for (i in intProgression step 2) {
        intList += i
    }
    assertEquals(listOf(1), intList)

    val longList = mutableListOf<Long>()
    val longProgression = 1L..<2L
    for (i in longProgression step 2L) {
        longList += i
    }
    assertEquals(listOf(1L), longList)

    val charList = mutableListOf<Char>()
    val charProgression = 'a'..<'b'
    for (i in charProgression step 2) {
        charList += i
    }
    assertEquals(listOf('a'), charList)

    return "OK"
}