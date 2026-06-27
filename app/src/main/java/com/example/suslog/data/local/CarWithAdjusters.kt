package com.example.suslog.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class CarWithAdjusters(
    @Embedded val car: CarEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "carId"
    )
    val adjusters: List<AdjusterEntity>,
)
