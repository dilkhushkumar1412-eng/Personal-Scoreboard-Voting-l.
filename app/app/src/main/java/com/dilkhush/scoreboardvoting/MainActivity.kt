package com.dilkhush.scoreboardvoting

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ScoreboardProject(
    val id: String = "1",
    var question: String = "Who is the best player?",
    var p1Name: String = "Player 1",
    var p2Name: String = "Player 2",
    var p1Votes: Int = 0,
    var p2Votes: Int = 0
)

class ScoreboardViewModel : ViewModel() {
    var project by mutableStateOf(ScoreboardProject())

    fun loadData(context: Context) {
        try {
            val prefs = context.getSharedPreferences("scoreboard_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("data", "")
            if (!json.isNullOrEmpty()) {
                project = Gson().fromJson(json, ScoreboardProject::class.java)
            }
        } catch (e: Exception) {}
    }

    fun saveData(context: Context) {
        try {
            val prefs = context.getSharedPreferences("scoreboard_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("data", Gson().toJson(project)).apply()
        } catch (e: Exception) {}
    }

    fun addVote(player: Int, amount: Int) {
        var newP1 = project.p1Votes
        var newP2 = project.p2Votes
        if (player == 1) newP1 += amount else newP2 += amount
        if (newP1 < 0) newP1 = 0
        if (newP2 < 0) newP2 = 0
        project = project.copy(p1Votes = newP1, p2Votes = newP2)
    }

    fun resetVotes() {
        project = project.copy(p1Votes = 0, p2Votes = 0)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: ScoreboardViewModel = viewModel()
                val context = LocalContext.current
                LaunchedEffect(Unit) { viewModel.loadData(context) }
                DisposableEffect(Unit) { onDispose { viewModel.saveData(context) } }
                
                ScoreboardScreen(viewModel)
            }
        }
    }
}

@Composable
fun ScoreboardScreen(viewModel: ScoreboardViewModel) {
    val project = viewModel.project
    val totalVotes = project.p1Votes + project.p2Votes
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Text(
            text = project.question, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PlayerPanel(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1976D2)), name = project.p1Name, votes = project.p1Votes, onVote = { viewModel.addVote(1, it) })
            PlayerPanel(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD32F2F)), name = project.p2Name, votes = project.p2Votes, onVote = { viewModel.addVote(2, it) })
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Total Votes: $totalVotes", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.resetVotes() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("RESET ALL") }
        }
    }
}

@Composable
fun PlayerPanel(modifier: Modifier, name: String, votes: Int, onVote: (Int) -> Unit) {
    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(votes.toString(), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onVote(-1) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha=0.3f))) { Text("-1") }
            Button(onClick = { onVote(1) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.2f))) { Text("+1") }
            Button(onClick = { onVote(5) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.2f))) { Text("+5") }
        }
    }
}
