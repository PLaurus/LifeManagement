package com.lauruspa.life_management.notifications

import com.lauruspa.life_management.algorithm.pairing.SzudzikPairingAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class NotificationIdsManagerUnitTest {
	@Test
	fun uniqueId_isCorrect() {
		val algorithm = SzudzikPairingAlgorithm()
		val manager = NotificationIdsManager(algorithm)
		
		val id00 = manager.getId(0, 0)
		val id01 = manager.getId(0, 1)
		val id02 = manager.getId(0, 2)
		val id03 = manager.getId(0, 3)
		val id10 = manager.getId(1, 0)
		val id11 = manager.getId(1, 1)
		val id12 = manager.getId(1, 2)
		val id13 = manager.getId(1, 3)
		val id20 = manager.getId(2, 0)
		val id21 = manager.getId(2, 1)
		val id22 = manager.getId(2, 2)
		val id23 = manager.getId(2, 3)
		val id30 = manager.getId(3, 0)
		val id31 = manager.getId(3, 1)
		val id32 = manager.getId(3, 2)
		val id33 = manager.getId(3, 3)
		
		val ids = listOfNotNull(
			id00,
			id01,
			id02,
			id03,
			id10,
			id11,
			id12,
			id13,
			id20,
			id21,
			id22,
			id23,
			id30,
			id31,
			id32,
			id33
		)
		
		val sortedIds = ids.sorted()
		
		println(sortedIds)
		
		val uniqueIds = sortedIds.toSet()
		
		assertEquals(ids.size, uniqueIds.size)
	}
}