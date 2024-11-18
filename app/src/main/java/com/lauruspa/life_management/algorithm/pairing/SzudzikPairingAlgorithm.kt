package com.lauruspa.life_management.algorithm.pairing

import kotlin.math.max

internal class SzudzikPairingAlgorithm : PairingAlgorithm {
	override fun pair(x: Int, y: Int): Int? {
		if (x < 0) return null
		if (y < 0) return null
		val maxArg = max(x, y)
		return if (x == maxArg) x * x + x + y else y * y + x
	}
}