package com.lauruspa.life_management.notifications

import com.lauruspa.life_management.algorithm.pairing.PairingAlgorithm

internal class NotificationIdsManager(
	private val pairingAlgorithm: PairingAlgorithm
) {
	fun getId(notificationType: Int, dataId: Int = 0): Int? {
		return pairingAlgorithm.pair(x = notificationType, y = dataId)
	}
}