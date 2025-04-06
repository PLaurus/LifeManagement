package com.lauruspa.life_management.core.ui.component.graph

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.lauruspa.life_management.core.ui.utils.parceler.IntRectParceler
import com.lauruspa.life_management.core.ui.utils.parceler.IntSizeParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Immutable
@Parcelize
@TypeParceler<IntSize, IntSizeParceler>()
@TypeParceler<IntRect, IntRectParceler>()
data class GraphLayoutInfo(
	val itemRectList: List<IntRect>,
	val containerSize: IntSize,
	val movableArea: IntRect,
	val contentPaddingLeft: Int,
	val contentPaddingTop: Int,
	val contentPaddingRight: Int,
	val contentPaddingBottom: Int
) : Parcelable {
	companion object {
		@Stable
		internal val Zero: GraphLayoutInfo
			get() = GraphLayoutInfo(
				itemRectList = emptyList(),
				containerSize = IntSize.Zero,
				movableArea = IntRect.Zero,
				contentPaddingLeft = 0,
				contentPaddingTop = 0,
				contentPaddingRight = 0,
				contentPaddingBottom = 0
			)
	}
}