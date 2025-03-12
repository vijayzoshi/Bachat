package com.example.nearstore.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY id DESC")
    suspend fun getTransactionsForCustomer(customerId: Int): List<Transaction>

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("""
        SELECT SUM(amount) 
        FROM transactions 
        WHERE customerId = :customerId
    """)
    suspend fun getBalanceForCustomer(customerId: Int): Double?

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteAllTransactionsForCustomer(customerId: Int)
}