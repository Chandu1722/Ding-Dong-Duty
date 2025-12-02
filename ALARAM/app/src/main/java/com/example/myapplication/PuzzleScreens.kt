package com.example.alarmpuzzle

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- PUZZLE 1: MATH PROBLEM (No changes, logic is sound) ---
@Composable
fun MathPuzzleScreen(onSolve: () -> Unit) {
    var num1 by remember { mutableStateOf(Random.nextInt(10, 100)) }
    var num2 by remember { mutableStateOf(Random.nextInt(10, 100)) }
    var userAnswer by remember { mutableStateOf("") }
    val correctAnswer = num1 + num2
    val context = LocalContext.current

    PuzzleContainer("Solve the math problem!") {
        Text(text = "$num1 + $num2 = ?", color = Color.White, fontSize = 48.sp, style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = userAnswer,
            onValueChange = { userAnswer = it.filter { char -> char.isDigit() } },
            label = { Text("Your answer") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.White, unfocusedLabelColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (userAnswer.toIntOrNull() == correctAnswer) onSolve()
                else {
                    Toast.makeText(context, "Wrong answer!", Toast.LENGTH_SHORT).show()
                    userAnswer = ""
                    num1 = Random.nextInt(10, 100)
                    num2 = Random.nextInt(10, 100)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("Submit", fontSize = 18.sp)
        }
    }
}

// --- PUZZLE 2: RETYPE TEXT (No changes, logic is sound) ---
@Composable
fun RetypeTextScreen(onSolve: () -> Unit) {
    val targetText = remember { (1..6).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("") }
    var userAnswer by remember { mutableStateOf("") }
    val context = LocalContext.current

    PuzzleContainer("Retype the text exactly!") {
        Text(
            text = targetText, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp).border(1.dp, Color.Gray).padding(16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = userAnswer, onValueChange = { userAnswer = it }, label = { Text("Type here") }, singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.White, unfocusedLabelColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (userAnswer == targetText) onSolve()
                else Toast.makeText(context, "Text does not match!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) { Text("Submit", fontSize = 18.sp) }
    }
}

// --- PUZZLE 3: SHAKE PHONE (Logic improved in PuzzleActivity) ---
@Composable
fun ShakePuzzleScreen(shakes: Int, targetShakes: Int) {
    PuzzleContainer("Shake your phone vigorously!") {
        Text(text = "$shakes / $targetShakes", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = shakes.toFloat() / targetShakes,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            color = Color.White,
            trackColor = Color.DarkGray
        )
    }
}

// --- PUZZLE 4: MEMORY PAIRS (REVAMPED) ---
data class MemoryCard(val id: Int, val icon: ImageVector, var isFaceUp: Boolean = false, var isMatched: Boolean = false)

@Composable
fun MemoryPairsScreen(onSolve: () -> Unit) {
    val icons = remember {
        listOf(
            Icons.Outlined.Pets, Icons.Outlined.FavoriteBorder, Icons.Outlined.Star,
            Icons.Outlined.ThumbUp, Icons.Outlined.Cloud, Icons.Outlined.Anchor
        ).let { (it + it).shuffled() }
    }
    val cards = remember { mutableStateListOf<MemoryCard>().apply { addAll(icons.mapIndexed { i, icon -> MemoryCard(i, icon) }) } }
    var selectedCards by remember { mutableStateOf<List<MemoryCard>>(emptyList()) }
    var checkingMatch by remember { mutableStateOf(false) }

    // This effect handles the logic for checking pairs
    LaunchedEffect(selectedCards) {
        if (selectedCards.size == 2) {
            checkingMatch = true
            delay(800)
            val (first, second) = selectedCards
            val firstIndex = cards.indexOf(first)
            val secondIndex = cards.indexOf(second)

            if (first.icon == second.icon) {
                // It's a match!
                cards[firstIndex] = first.copy(isMatched = true)
                cards[secondIndex] = second.copy(isMatched = true)
                // --- BUG FIX: Check for win condition *after* a match is confirmed ---
                if (cards.all { it.isMatched }) {
                    onSolve()
                }
            } else {
                // Not a match, flip back over
                cards[firstIndex] = first.copy(isFaceUp = false)
                cards[secondIndex] = second.copy(isFaceUp = false)
            }
            selectedCards = emptyList()
            checkingMatch = false
        }
    }

    PuzzleContainer("Find all the matching pairs!") {
        LazyVerticalGrid(columns = GridCells.Fixed(3)) {
            items(cards) { card ->
                val cardIndex = cards.indexOf(card)
                Card(
                    modifier = Modifier.padding(4.dp).aspectRatio(1f)
                        .clickable(enabled = !card.isFaceUp && !checkingMatch && !card.isMatched) {
                            if (selectedCards.size < 2) {
                                cards[cardIndex] = card.copy(isFaceUp = true)
                                selectedCards = selectedCards + cards[cardIndex]
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = if (card.isMatched) Color.DarkGray else Color(0xFF333333))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (card.isFaceUp) {
                            Icon(imageVector = card.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        } else {
                            Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Card Back", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
    }
}


// --- PUZZLE 5: COLOR MATCH (No changes, logic is sound) ---
@Composable
fun ColorMatchScreen(onSolve: () -> Unit) {
    val colors = remember { listOf("Red", "Green", "Blue", "Yellow") }
    val colorMap = mapOf("Red" to Color.Red, "Green" to Color.Green, "Blue" to Color.Blue, "Yellow" to Color.Yellow)

    var targetWord by remember { mutableStateOf(colors.random()) }
    var textColor by remember { mutableStateOf(colorMap[(colors - targetWord).random()] ?: Color.White) }
    val context = LocalContext.current

    PuzzleContainer("Tap the button with the correct color name!") {
        Text(text = targetWord.uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(32.dp))
        colors.shuffled().forEach { colorName ->
            Button(
                onClick = {
                    if (colorName == targetWord) onSolve()
                    else {
                        Toast.makeText(context, "Wrong color!", Toast.LENGTH_SHORT).show()
                        targetWord = colors.random()
                        textColor = colorMap[(colors - targetWord).random()] ?: Color.White
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorMap[colorName]!!),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(colorName, style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp))
            }
        }
    }
}

// --- Universal Puzzle Container ---
@Composable
fun PuzzleContainer(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 22.sp, color = Color.White, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))
            content()
        }
    }
}

