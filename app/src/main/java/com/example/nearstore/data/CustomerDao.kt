package com.example.nearstore.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY timestamp DESC")
    suspend fun getAllCustomers(): List<Customer>

    @Insert
    suspend fun insertCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Int): Customer?

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int

    @Query("""
        SELECT SUM(
            CASE 
                WHEN t.type = 'CREDIT' THEN t.amount 
                ELSE t.amount 
            END
        ) 
        FROM customers c 
        LEFT JOIN transactions t ON c.id = t.customerId
    """)
    suspend fun getTotalBalance(): Double?
}