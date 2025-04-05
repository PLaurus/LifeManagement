package com.lauruspa.life_management.core.ui.component.graph

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntRect
import com.lauruspa.life_management.core.ui.utils.parceler.IntRectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

//@Immutable
//@Parcelize
//@TypeParceler<IntRect, IntRectParceler>()
//data class GraphLayoutInfo(
//	val itemKeyToRectMap: Map<Int, IntRect>,
//	val viewportRect: IntRect,
//) : Parcelable {
//	companion object {
//		@Stable
//		internal val Zero: GraphLayoutInfo
//			get() = GraphLayoutInfo(
//				itemKeyToRectMap = emptyMap(),
//				viewportRect = IntRect.Zero
//			)
//	}
//}