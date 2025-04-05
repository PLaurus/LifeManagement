package com.lauruspa.life_management.core.ui.utils.parceler

import android.os.Parcel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.parcelize.Parceler

object ColorParceler : Parceler<Color> {
    override fun create(parcel: Parcel): Color {
        return Color(parcel.readInt())
    }

    override fun Color.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(toArgb())
    }
}