package com.example.nearstore.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val amount: Double,
    val description: String,
    val type: TransactionType,
    val selectedTimeId: String = "Morning",
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    CREDIT, DEBIT
}