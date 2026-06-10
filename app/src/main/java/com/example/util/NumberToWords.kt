package com.example.util

object NumberToWords {
    private val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
    private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    fun convert(amount: Double): String {
        val n = amount.toLong()
        val paisa = Math.round((amount - n) * 100).toInt()
        
        var words = if (n == 0L) "Zero" else convertToWords(n).trim()
        words += " Rupees"
        
        if (paisa > 0) {
            words += " and " + convertToWords(paisa.toLong()).trim() + " Paisa"
        }
        
        return "$words Only"
    }

    private fun convertToWords(n: Long): String {
        if (n < 20) return units[n.toInt()]
        if (n < 100) return tens[(n / 10).toInt()] + " " + units[(n % 10).toInt()]
        if (n < 1000) return units[(n / 100).toInt()] + " Hundred " + convertToWords(n % 100)
        if (n < 100000) return convertToWords(n / 1000) + " Thousand " + convertToWords(n % 1000)
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh " + convertToWords(n % 100000)
        return convertToWords(n / 10000000) + " Crore " + convertToWords(n % 10000000)
    }
}
