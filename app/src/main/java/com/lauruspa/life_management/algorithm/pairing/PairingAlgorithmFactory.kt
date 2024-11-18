package com.lauruspa.life_management.algorithm.pairing

class PairingAlgorithmFactory {
	fun create(type: PairingAlgorithmType): PairingAlgorithm {
		return when (type) {
			PairingAlgorithmType.SZUDZIK -> SzudzikPairingAlgorithm()
		}
	}
}