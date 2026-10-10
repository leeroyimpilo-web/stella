package za.co.lottolab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun HeaderBar(onMenu:()->Unit,onAlert:()->Unit) {
 Row(Modifier.fillMaxWidth().height(43.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
  IconButton(onClick=onMenu){Icon(Icons.Default.Menu,contentDescription="Open menu",tint=Color.White)}
  IconButton(onClick=onAlert){
   BadgedBox(badge={Badge(containerColor=Color(0xFFFF3854),modifier=Modifier.size(7.dp)){}}){
    Icon(Icons.Default.NotificationsNone,contentDescription="Notifications",tint=Color.White)
   }
  }
 }
}

@Composable fun HeroBrand(){
 Box(Modifier.fillMaxWidth().height(156.dp),contentAlignment=Alignment.Center){
  Skyline(Modifier.matchParentSize())
  Column(horizontalAlignment=Alignment.CenterHorizontally){
   Crown(Modifier.width(70.dp).height(45.dp))
   Spacer(Modifier.height(4.dp))
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Lotto",fontSize=39.sp,fontWeight=FontWeight.Black,color=Gold,
     style=TextStyle(shadow=Shadow(Gold.copy(alpha=.6f),Offset(0f,2f),8f)))
    Text("Lab",fontSize=39.sp,fontWeight=FontWeight.Black,color=Color(0xFFF3F7FF))
    Text("™",fontSize=10.sp,color=Color.White,modifier=Modifier.align(Alignment.Top))
   }
   Text("BIG NUMBERS. BRIGHTER TOMORROWS.",fontSize=9.sp,
    fontWeight=FontWeight.Bold,letterSpacing=1.sp,color=Color.White,textAlign=TextAlign.Center)
  }
 }
}

@Composable fun GeneratorCard(game:GamePick,numbers:List<Int>,onGenerate:()->Unit,onPlay:()->Unit){
 Glass(modifier=Modifier.fillMaxWidth(),tint=Gold){
  Column(Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=17.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text("GENERATE LUCKY NUMBERS",fontSize=18.sp,lineHeight=21.sp,
    fontWeight=FontWeight.ExtraBold,color=Color.White,textAlign=TextAlign.Center)
   Spacer(Modifier.height(7.dp))
   Text("Let Lotto Lab create your next\nlucky combination.",fontSize=12.sp,lineHeight=17.sp,
    color=Color(0xFFD5E4EE),textAlign=TextAlign.Center)
   Spacer(Modifier.height(6.dp))
   BoxWithConstraints(Modifier.fillMaxWidth().height(111.dp)){
    Orbit(Modifier.matchParentSize())
    val diameter=((maxWidth-15.dp)/numbers.size.toFloat()).coerceAtMost(58.dp)
    Row(Modifier.align(Alignment.Center),horizontalArrangement=Arrangement.spacedBy(2.dp),
     verticalAlignment=Alignment.CenterVertically){
     numbers.forEachIndexed { i,n -> Ball(n,when(i%3){0->Gold;1->Green;else->Blue},diameter,seed=i) }
    }
   }
   Spacer(Modifier.height(6.dp))
   val shape=RoundedCornerShape(40.dp)
   Row(Modifier.fillMaxWidth().height(60.dp)
    .shadow(10.dp,shape,ambientColor=Gold,spotColor=Gold)
    .background(Brush.verticalGradient(listOf(Color(0xFFFFF2A4),Gold,Color(0xFFD89717))),shape)
    .border(1.5.dp,Color(0xFFFFF2BA),shape)
    .clickable(onClick=onGenerate)
    .padding(horizontal=22.dp),
    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
    Icon(Icons.Default.AutoAwesome,contentDescription=null,tint=Color(0xFF0C2637),modifier=Modifier.size(27.dp))
    Text("Generate",fontWeight=FontWeight.Black,fontSize=25.sp,color=Color(0xFF0E1C2B))
    Icon(Icons.Default.ChevronRight,contentDescription="Generate new numbers",tint=Color(0xFF0E1C2B))
   }
   TextButton(onClick=onPlay,modifier=Modifier.padding(top=2.dp)){
    Text("OPEN " + game.id.uppercase() + " STUDIO  ›",fontWeight=FontWeight.Bold,color=Color(0xFFFFE58E),fontSize=10.sp)
   }
  }
 }
}
