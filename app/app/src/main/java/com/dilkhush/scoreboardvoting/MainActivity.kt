package com.dilkhush.scoreboardvoting

import android.content.Context
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
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
import java.util.UUID

data class ScoreboardProject(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Match 1",
    var question: String = "Who is the best player?",
    var p1Name: String = "Player 1",
    var p2Name: String = "Player 2",
    var p1Votes: Int = 0,
    var p2Votes: Int = 0,
    var isHindi: Boolean = false
)

class ScoreboardViewModel : ViewModel() {
    var projects by mutableStateOf<List<ScoreboardProject>>(emptyList())
    var currentProject by mutableStateOf(ScoreboardProject())
    private val historyStack = mutableListOf<ScoreboardProject>()

    fun loadData(context: Context) {
        try {
            val prefs = context.getSharedPreferences("scoreboard_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("projects_data", "[]")
            if (json == "[]") {
                setupDefault()
                return
            }
            val type = object : TypeToken<List<ScoreboardProject>>() {}.type
            val loaded: List<ScoreboardProject>? = Gson().fromJson(json, type)
            if (loaded.isNullOrEmpty()) {
                setupDefault()
            } else {
                projects = loaded
                currentProject = projects.first()
            }
        } catch (e: Exception) {
            setupDefault()
        }
    }

    private fun setupDefault() {
        val default = ScoreboardProject()
        projects = listOf(default)
        currentProject = default
    }

    fun saveData(context: Context) {
        try {
            val prefs = context.getSharedPreferences("scoreboard_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("projects_data", Gson().toJson(projects)).apply()
        } catch (e: Exception) {}
    }

    fun addVote(player: Int, amount: Int) {
        historyStack.add(currentProject.copy())
        var newP1 = currentProject.p1Votes
        var newP2 = currentProject.p2Votes
        if (player == 1) newP1 += amount else newP2 += amount
        if (newP1 < 0) newP1 = 0
        if (newP2 < 0) newP2 = 0
        currentProject = currentProject.copy(p1Votes = newP1, p2Votes = newP2)
        updateProjectInList()
    }

    fun undo() {
        if (historyStack.isNotEmpty()) {
            currentProject = historyStack.removeLast()
            updateProjectInList()
        }
    }

    fun resetVotes() {
        historyStack.add(currentProject.copy())
        currentProject = currentProject.copy(p1Votes = 0, p2Votes = 0)
        updateProjectInList()
    }
    
    private fun updateProjectInList() {
        projects = projects.map { if (it.id == currentProject.id) currentProject else it }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1E1E1E)) {
                        val viewModel: ScoreboardViewModel = viewModel()
                        val context = LocalContext.current
                        LaunchedEffect(Unit) { viewModel.loadData(context) }
                        DisposableEffect(Unit) { onDispose { viewModel.saveData(context) } }
                        ScoreboardScreen(viewModel)
                    }
                }
            }
        } catch (e: Exception) {
            // CRASH SCANNER: अगर कुछ भी फेल हुआ, तो ऐप बंद नहीं होगा, स्क्रीन पर लाल रंग में एरर बता देगा!
            val sv = ScrollView(this)
            val tv = TextView(this)
            tv.text = "CRASH DETECTED:\n\n${e.message}\n\n${e.stackTraceToString()}"
            tv.setTextColor(android.graphics.Color.RED)
            tv.setPadding(32, 32, 32, 32)
            sv.addView(tv)
            setContentView(sv)
        }
    }
}

@Composable
fun ScoreboardScreen(viewModel: ScoreboardViewModel) {
    val project = viewModel.currentProject
    val totalVotes = project.p1Votes + project.p2Votes
    
    val p1Percent = if (totalVotes <= 0) 50f else (project.p1Votes.toFloat() / totalVotes) * 100f
    val p2Percent = if (totalVotes <= 0) 50f else (project.p2Votes.toFloat() / totalVotes) * 100f

    val labels = if (project.isHindi) {
        mapOf("votes" to "वोट", "undo" to "वापस लें", "reset" to "रीसेट", "total" to "कुल वोट")
    } else {
        mapOf("votes" to "VOTES", "undo" to "Undo", "reset" to "Reset", "total" to "Total Votes")
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Text(
            text = project.question,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PlayerPanel(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1976D2)),
                name = project.p1Name, votes = project.p1Votes, voteLabel = labels["votes"]!!,
                onVote = { viewModel.addVote(1, it) }
            )
            PlayerPanel(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFD32F2F)),
                name = project.p2Name, votes = project.p2Votes, voteLabel = labels["votes"]!!,
                onVote = { viewModel.addVote(2, it) }
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${String.format("%.1f", p1Percent)}%", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold)
                Text("${labels["total"]}: $totalVotes", color = Color.White)
                Text("${String.format("%.1f", p2Percent)}%", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth().height(10.dp).padding(vertical = 4.dp)) {
                val w1 = if (p1Percent > 0f) p1Percent else 0.01f
                val w2 = if (p2Percent > 0f) p2Percent else 0.01f
                Box(modifier = Modifier.weight(w1).fillMaxHeight().background(Color(0xFF1976D2)))
                Box(modifier = Modifier.weight(w2).fillMaxHeight().background(Color(0xFFD32F2F)))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { viewModel.undo() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(labels["undo"]!!) }
                Button(onClick = { viewModel.resetVotes() }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(labels["reset"]!!) }
            }
        }
    }
}

@Composable
fun PlayerPanel(modifier: Modifier, name: String, votes: Int, voteLabel: String, onVote: (Int) -> Unit) {
    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(votes.toString(), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)
        Text(voteLabel, color = Color.LightGray, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VotingButton("-1", Color.Black.copy(alpha=0.3f)) { onVote(-1) }
            VotingButton("+1", Color.White.copy(alpha=0.2f)) { onVote(1) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VotingButton("+5", Color.White.copy(alpha=0.2f)) { onVote(5) }
            VotingButton("+10", Color.White.copy(alpha=0.2f)) { onVote(10) }
        }
    }
}

@Composable
fun VotingButton(label: String, bgColor: Color, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = bgColor), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(60.dp)) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
