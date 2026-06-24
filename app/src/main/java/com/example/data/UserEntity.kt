package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val nationalCode: String,
    val phoneNumber: String,
    val address: String,
    val timestamp: Long = System.currentTimeMillis()
)
