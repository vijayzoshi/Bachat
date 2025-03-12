package com.example.nearstore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nearstore.R
import com.example.nearstore.data.Transaction
import com.example.nearstore.data.TransactionType
import com.example.nearstore.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

class TransactionAdapter(
    private var transactions: List<Transaction> = emptyList(),
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit,
    private val onItemClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            val context = binding.root.context
            val isCredit = transaction.type == TransactionType.CREDIT
            
            // Set icon and background based on transaction type
            if (isCredit) {
                binding.transactionTypeIcon.setImageResource(R.drawable.baseline_arrow_downward_24)
                binding.transactionTypeIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.credit_container))
                binding.transactionTypeIcon.setColorFilter(ContextCompat.getColor(context, R.color.on_credit_container))
                binding.amountTextView.setTextColor(ContextCompat.getColor(context, R.color.credit_green))
            } else {
                binding.transactionTypeIcon.setImageResource(R.drawable.baseline_arrow_upward_24)
                binding.transactionTypeIcon.setBackgroundColor(ContextCompat.getColor(context, R.color.debit_container))
                binding.transactionTypeIcon.setColorFilter(ContextCompat.getColor(context, R.color.on_debit_container))
                binding.amountTextView.setTextColor(ContextCompat.getColor(context, R.color.debit_red))
            }

            // Format amount without sign, we'll use colors instead
            val formattedAmount = String.format("₹%.2f", transaction.amount.absoluteValue)
            binding.amountTextView.text = formattedAmount
            
            binding.descriptionTextView.text = transaction.description
            
            // Format date but don't show time
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.dateTextView.text = sdf.format(Date(transaction.timestamp))
            
            binding.editButton.setOnClickListener { onEditClick(transaction) }
            binding.deleteButton.setOnClickListener { onDeleteClick(transaction) }
            binding.root.setOnClickListener { onItemClick(transaction) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}