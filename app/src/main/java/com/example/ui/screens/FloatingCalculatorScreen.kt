package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.*

data class CalcHistoryItem(
    val equation: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun FloatingCalculatorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var equation by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf<List<CalcHistoryItem>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    var isScientific by remember { mutableStateOf(false) }

    fun evaluate(expr: String): String {
        if (expr.isBlank()) return "0"
        return try {
            val sanitized = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "${Math.PI}")
                .replace("e", "${Math.E}")
            val eval = SimpleMathEvaluator.evaluate(sanitized)
            if (eval.isNaN() || eval.isInfinite()) {
                "Error"
            } else {
                val df = DecimalFormat("#,##0.######")
                df.format(eval)
            }
        } catch (_: Exception) {
            "..."
        }
    }

    fun onButtonClick(label: String) {
        when (label) {
            "C" -> {
                equation = ""
                resultText = "0"
            }
            "⌫" -> {
                if (equation.isNotEmpty()) {
                    equation = equation.dropLast(1)
                    resultText = if (equation.isNotEmpty()) evaluate(equation) else "0"
                }
            }
            "=" -> {
                val finalResult = evaluate(equation)
                if (finalResult != "..." && finalResult != "Error") {
                    history = listOf(CalcHistoryItem(equation, finalResult)) + history.take(25)
                    equation = finalResult.replace(",", "")
                    resultText = ""
                }
            }
            "±" -> {
                if (equation.startsWith("-")) {
                    equation = equation.substring(1)
                } else if (equation.isNotEmpty()) {
                    equation = "-$equation"
                }
                resultText = evaluate(equation)
            }
            "sin", "cos", "tan", "√", "^" -> {
                equation += when (label) {
                    "√" -> "sqrt("
                    else -> "$label("
                }
                resultText = evaluate(equation)
            }
            else -> {
                equation += label
                resultText = evaluate(equation)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp)
    ) {
        // Top Toolbar: History toggle, Scientific toggle, Copy Result
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { showHistory = !showHistory },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = if (showHistory) Icons.Default.Calculate else Icons.Default.History,
                        contentDescription = "History",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showHistory) "ক্যালকুলেটর" else "হিস্ট্রি (${history.size})", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = { isScientific = !isScientific },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(if (isScientific) "বেসিক" else "সায়েন্টিফিক (fx)", fontSize = 11.sp)
                }
            }

            IconButton(
                onClick = {
                    val textToCopy = if (resultText.isNotEmpty() && resultText != "0" && resultText != "...") {
                        resultText
                    } else equation.ifEmpty { "0" }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Calculation", textToCopy)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "রেজাল্ট কপি করা হয়েছে: $textToCopy", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Result",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (showHistory) {
            // History View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("পূর্ববর্তী হিসাবসমূহ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = { history = emptyList() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("সব মুছুন", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("কোনো হিসাবের হিস্ট্রি নেই", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { item ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        equation = item.result.replace(",", "")
                                        resultText = ""
                                        showHistory = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = item.equation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "= ${item.result}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Calculator Screen Display
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = equation.ifEmpty { "0" },
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (resultText.isNotEmpty() && resultText != "...") "= $resultText" else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Keypad Grid
            val basicButtons = listOf(
                listOf("C", "±", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "⌫", "=")
            )

            val scientificRow = listOf("sin", "cos", "tan", "√", "^", "π")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isScientific) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scientificRow.forEach { sym ->
                            CalcButton(
                                text = sym,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = { onButtonClick(sym) }
                            )
                        }
                    }
                }

                basicButtons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { label ->
                            val isOperator = label in listOf("÷", "×", "-", "+", "=")
                            val isSpecial = label in listOf("C", "±", "%", "⌫")

                            val btnColor = when {
                                label == "=" -> MaterialTheme.colorScheme.primary
                                isOperator -> MaterialTheme.colorScheme.primaryContainer
                                isSpecial -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            val textColor = when {
                                label == "=" -> MaterialTheme.colorScheme.onPrimary
                                isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                isSpecial -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            CalcButton(
                                text = label,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                containerColor = btnColor,
                                contentColor = textColor,
                                onClick = { onButtonClick(label) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (text == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = if (text.length > 2) 13.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Lightweight recursive descent parser for basic math expressions
 */
object SimpleMathEvaluator {
    fun evaluate(expression: String): Double {
        val tokens = tokenize(expression)
        val parser = Parser(tokens)
        return parser.parseExpression()
    }

    private fun tokenize(expr: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c in "+-*/^%()" -> {
                    list.add(c.toString())
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    list.add(sb.toString())
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expr.length && expr[i].isLetter()) {
                        sb.append(expr[i])
                        i++
                    }
                    list.add(sb.toString())
                }
                else -> i++
            }
        }
        return list
    }

    private class Parser(private val tokens: List<String>) {
        private var pos = 0

        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < tokens.size) {
                when (tokens[pos]) {
                    "+" -> {
                        pos++
                        result += parseTerm()
                    }
                    "-" -> {
                        pos++
                        result -= parseTerm()
                    }
                    else -> break
                }
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parseFactor()
            while (pos < tokens.size) {
                when (tokens[pos]) {
                    "*" -> {
                        pos++
                        result *= parseFactor()
                    }
                    "/" -> {
                        pos++
                        val divisor = parseFactor()
                        result /= divisor
                    }
                    "%" -> {
                        pos++
                        result %= parseFactor()
                    }
                    else -> break
                }
            }
            return result
        }

        private fun parseFactor(): Double {
            var result = parsePrimary()
            if (pos < tokens.size && tokens[pos] == "^") {
                pos++
                val exp = parseFactor()
                result = result.pow(exp)
            }
            return result
        }

        private fun parsePrimary(): Double {
            if (pos >= tokens.size) return 0.0
            val token = tokens[pos]

            if (token == "-") {
                pos++
                return -parsePrimary()
            }
            if (token == "+") {
                pos++
                return parsePrimary()
            }
            if (token == "(") {
                pos++
                val res = parseExpression()
                if (pos < tokens.size && tokens[pos] == ")") pos++
                return res
            }

            // Functions: sin, cos, tan, sqrt
            if (token in listOf("sin", "cos", "tan", "sqrt")) {
                pos++
                val arg = if (pos < tokens.size && tokens[pos] == "(") {
                    pos++
                    val a = parseExpression()
                    if (pos < tokens.size && tokens[pos] == ")") pos++
                    a
                } else parsePrimary()

                return when (token) {
                    "sin" -> sin(Math.toRadians(arg))
                    "cos" -> cos(Math.toRadians(arg))
                    "tan" -> tan(Math.toRadians(arg))
                    "sqrt" -> sqrt(arg)
                    else -> arg
                }
            }

            pos++
            return token.toDoubleOrNull() ?: 0.0
        }
    }
}
