package com.lauruspa.life_management.core.ui.component.honey_combs

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.lauruspa.life_management.core.ui.utils.parceler.OffsetParceler

@Immutable
@Parcelize
@TypeParceler<Offset, OffsetParceler>()
data class CameraPosition(
    val zoom: Float,
    val rotation: Float,
    val pan: Offset
) : Parcelable

