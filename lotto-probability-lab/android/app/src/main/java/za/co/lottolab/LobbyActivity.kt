package za.co.lottolab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Native premium lobby; the proven offline probability studio remains in MainActivity. */
class LobbyActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor=Navy.toArgb()
  window.navigationBarColor=Navy.toArgb()
  @Suppress("DEPRECATION")
  window.decorView.systemUiVisibility=0
  setContent {
   MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Navy,onBackground=androidx.compose.ui.graphics.Color.White)) {
    LottoHome { tab,game ->
     startActivity(Intent(this@LobbyActivity,MainActivity::class.java)
      .putExtra("lotto_tab",tab).putExtra("lotto_game",game))
    }
   }
  }
 }
}

enum class GamePick(val id:String,val count:Int,val max:Int,val cost:Int) {
 LOTTO("lotto",6,52,5), DAILY("daily",5,36,3), POWER("power",5,50,10)
}

private val secureRandom=SecureRandom()
private fun quickPick(game:GamePick):List<Int>{
 val list=(1..game.max).toMutableList()
 for(i in 0 until game.count){
  val j=i+secureRandom.nextInt(list.size-i)
  val old=list[i];list[i]=list[j];list[j]=old
 }
 return list.take(game.count).sorted()
}

@Composable
private fun LottoHome(openStudio:(String,String?)->Unit) {
 var game by remember { mutableStateOf(GamePick.LOTTO) }
 var chosen by remember { mutableStateOf(listOf(7,12,28,33,44,49)) }
 var lucky by remember { mutableStateOf(listOf(3,11,19,24,37,45)) }
 var menu by remember { mutableStateOf(false) }
 var dialog by remember { mutableStateOf<String?>(null) }
 val today=remember { SimpleDateFormat("d MMM yyyy",Locale.ENGLISH).format(Date()) }
 Box(Modifier.fillMaxSize()){
  AmbientBackdrop(Modifier.fillMaxSize())
  Column(Modifier.fillMaxSize()){
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=15.dp)){
    Box(Modifier.fillMaxWidth()){
     HeaderBar(onMenu={menu=true},onAlert={dialog="Lotto Lab is an offline number studio. Live draw notifications are not connected."})
     DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
      DropdownMenuItem(text={Text("Number studio")},onClick={menu=false;openStudio("numbers",game.id)})
      DropdownMenuItem(text={Text("Odds & coverage")},onClick={menu=false;openStudio("odds",game.id)})
      DropdownMenuItem(text={Text("Draw tools")},onClick={menu=false;openStudio("tools",game.id)})
      DropdownMenuItem(text={Text("Saved tickets")},onClick={menu=false;openStudio("saved",null)})
     }
    }
    HeroBrand()
    Spacer(Modifier.height(8.dp))
    GeneratorCard(game,chosen,onGenerate={chosen=quickPick(game)},onPlay={openStudio("numbers",game.id)})
    Spacer(Modifier.height(17.dp))
    FeatureShortcuts(
     onDaily={game=GamePick.DAILY;chosen=quickPick(game);openStudio("numbers",game.id)},
     onPower={game=GamePick.POWER;chosen=quickPick(game);openStudio("numbers",game.id)},
     onSaved={openStudio("saved",null)}
    )
    Spacer(Modifier.height(18.dp))
    LuckyPanel(lucky,today,onAgain={lucky=quickPick(GamePick.LOTTO)})
    Spacer(Modifier.height(12.dp))
    Text("Independent number generator • No outcome can be predicted",
     color=androidx.compose.ui.graphics.Color.White.copy(alpha=.55f),
     style=MaterialTheme.typography.labelSmall,textAlign=TextAlign.Center,
     modifier=Modifier.fillMaxWidth())
    Spacer(Modifier.height(14.dp))
   }
   BottomGlassNav(onTap={which->
    when(which){
     "Home"->{game=GamePick.LOTTO;chosen=quickPick(game)}
     "Play"->openStudio("numbers",game.id)
     "Tickets"->openStudio("saved",null)
     "Results"->openStudio("tools",game.id)
     else->dialog="Lotto Lab 1.1 • Offline number generation and mathematical coverage analysis. No account, live results or real-money ticket sales are connected."
    }
   })
  }
  if(dialog!=null) AlertDialog(
   onDismissRequest={dialog=null},
   title={Text("Lotto Lab",color=Gold)},
   text={Text(dialog ?: "")},
   confirmButton={TextButton(onClick={dialog=null}){Text("Close")}}
  )
 }
}
