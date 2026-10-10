package za.co.lottolab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Feature(
 title:String,small:String,icon:String,tint:Color,modifier:Modifier=Modifier,onClick:()->Unit
){
 val shape=RoundedCornerShape(23.dp)
 Column(modifier.height(155.dp)
  .background(Brush.verticalGradient(listOf(tint.copy(alpha=.66f),tint.copy(alpha=.27f),Color(0xDF071A26))),shape)
  .border(1.dp,Brush.linearGradient(listOf(Color.White.copy(alpha=.4f),tint,Color.White.copy(alpha=.16f))),shape)
  .clickable(onClick=onClick).padding(7.dp),
  horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){
  Text(icon,fontSize=31.sp,color=Color.White)
  Text(title,fontWeight=FontWeight.ExtraBold,fontSize=13.sp,lineHeight=16.sp,maxLines=2,
   textAlign=TextAlign.Center,color=Color.White)
  Text(small,fontWeight=FontWeight.Medium,fontSize=8.sp,lineHeight=10.sp,
   maxLines=2,textAlign=TextAlign.Center,color=Color(0xFFEAF5F9))
  Box(Modifier.size(26.dp).border(1.dp,Color.White.copy(alpha=.75f),CircleShape),
   contentAlignment=Alignment.Center){
   Icon(Icons.Default.ArrowForward,contentDescription="Open $title",tint=Color.White,modifier=Modifier.size(17.dp))
  }
 }
}

@Composable fun FeatureShortcuts(onDaily:()->Unit,onPower:()->Unit,onSaved:()->Unit){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
  Feature("Daily Lotto","PLAY TODAY\nCHANGE TOMORROW","♣",Green,Modifier.weight(1f),onDaily)
  Feature("PowerBall","BIGGER NUMBERS\nBIGGER DREAMS","★",Blue,Modifier.weight(1f),onPower)
  Feature("Saved Tickets","YOUR NUMBERS\nANYTIME","▱",Gold,Modifier.weight(1f),onSaved)
 }
}

@Composable fun LuckyPanel(numbers:List<Int>,date:String,onAgain:()->Unit){
 Glass(Modifier.fillMaxWidth(),tint=Gold){
  Column(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=15.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Icon(Icons.Default.EmojiEvents,contentDescription=null,tint=Gold,modifier=Modifier.size(31.dp))
    Spacer(Modifier.width(8.dp))
    Column(Modifier.weight(1f)){
     Text("Today's Lucky Numbers",color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=14.sp,lineHeight=17.sp)
     Text("Random selection • $date",fontSize=9.sp,color=Color(0xFFD5E4EE))
    }
    TextButton(
     onClick=onAgain,
     modifier=Modifier.border(1.dp,Color(0xFFB8E1E9),RoundedCornerShape(30.dp))
      .background(Color(0x60344E62),RoundedCornerShape(30.dp)),
     contentPadding=PaddingValues(horizontal=9.dp,vertical=4.dp)
    ){
     Icon(Icons.Default.Refresh,contentDescription=null,tint=Color.White,modifier=Modifier.size(16.dp))
     Spacer(Modifier.width(4.dp))
     Text("Try Again",fontSize=11.sp,color=Color.White)
    }
   }
   Spacer(Modifier.height(2.dp))
   BoxWithConstraints(Modifier.fillMaxWidth().height(93.dp)){
    Orbit(Modifier.matchParentSize())
    val diameter=((maxWidth-12.dp)/numbers.size.toFloat()).coerceAtMost(53.dp)
    Row(Modifier.align(Alignment.Center),horizontalArrangement=Arrangement.spacedBy(2.dp),
     verticalAlignment=Alignment.CenterVertically){
     numbers.forEachIndexed { i,n -> Ball(n,when(i%3){0->Green;1->Blue;else->Gold},diameter,seed=i+4) }
    }
   }
   Text("“New numbers. New possibilities.”",fontSize=10.sp,color=Color(0xFFE0EAF1),
    textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
  }
 }
}

@Composable fun BottomGlassNav(onTap:(String)->Unit){
 val entries=listOf(
  Triple("Home",Icons.Default.Home,Gold),
  Triple("Play",Icons.Default.CalendarToday,Color(0xFFAFCADD)),
  Triple("Tickets",Icons.Default.ConfirmationNumber,Color(0xFFAFCADD)),
  Triple("Results",Icons.Default.BarChart,Color(0xFFAFCADD)),
  Triple("Profile",Icons.Default.PersonOutline,Color(0xFFAFCADD))
 )
 Row(Modifier.fillMaxWidth().height(74.dp)
  .background(Brush.verticalGradient(listOf(Color(0xEA0D353B),Color(0xF6041424))))
  .border(1.dp,Color(0x72F8CC62),RoundedCornerShape(topStart=23.dp,topEnd=23.dp))
  .padding(horizontal=4.dp,vertical=8.dp),
  horizontalArrangement=Arrangement.SpaceEvenly){
  for((name,icon,tint) in entries) {
   Column(Modifier.weight(1f).fillMaxHeight().clickable{onTap(name)},
    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){
    Icon(icon,contentDescription=name,tint=tint,modifier=Modifier.size(25.dp))
    Text(name,color=if(name=="Home")Gold else Color(0xFFC2D5E2),fontSize=10.sp,
     fontWeight=if(name=="Home")FontWeight.Bold else FontWeight.Normal)
   }
  }
 }
}
