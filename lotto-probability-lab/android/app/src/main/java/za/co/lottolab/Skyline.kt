package za.co.lottolab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/** Independently rendered scenic artwork; the UI is never a flattened screenshot. */
@Composable fun Skyline(modifier:Modifier=Modifier){
 Canvas(modifier){
 val w=size.width;val h=size.height
 drawRect(Brush.verticalGradient(listOf(Color(0xFF062344),Color(0xFF0C5763),Color(0xFFDD893C),Color(0xFF051C2A))))
 val ridges=Path().apply{
 moveTo(0f,h*.72f);lineTo(w*.09f,h*.60f);lineTo(w*.15f,h*.63f)
 lineTo(w*.23f,h*.46f);lineTo(w*.33f,h*.49f);lineTo(w*.39f,h*.67f)
 lineTo(w*.51f,h*.69f);lineTo(w*.66f,h*.55f);lineTo(w*.78f,h*.56f)
 lineTo(w*.91f,h*.63f);lineTo(w,h*.59f);lineTo(w,h);lineTo(0f,h);close()}
 drawPath(ridges,Brush.verticalGradient(listOf(Color(0xFF0C2B40),Color(0xFF071624))))
 val table=Path().apply{
 moveTo(w*.02f,h*.78f);lineTo(w*.13f,h*.69f);lineTo(w*.17f,h*.44f)
 lineTo(w*.32f,h*.445f);lineTo(w*.39f,h*.68f);lineTo(w*.47f,h*.8f)
 lineTo(w*.47f,h);lineTo(0f,h);close()}
 drawPath(table,Color(0xFF091B2C))
 for(i in 0 until 165){
  val x=((i*83+11)%167)/167f*w
  val y=(.76f+((i*29)%20)/100f)*h
  val radius=if(i%11==0) 1.8.dp.toPx() else .8.dp.toPx()
  drawCircle(if(i%5==0) Color(0xFFFFF1B6) else Color(0xFFFFBF57),radius,Offset(x,y),alpha=.58f+(i%4)*.11f)
 }
 drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color(0xB8031A28))),topLeft=Offset(0f,h*.65f),size=Size(w,h*.35f))
 drawLine(Color(0x9DFFC85B),Offset(0f,h*.87f),Offset(w,h*.87f),1.dp.toPx())
 }
}

@Composable fun AmbientBackdrop(modifier:Modifier=Modifier){
 Canvas(modifier.background(Brush.verticalGradient(listOf(Color(0xFF061B2E),Color(0xFF043B36),Color(0xFF02131F))))){
 val w=size.width;val h=size.height
 drawCircle(Brush.radialGradient(listOf(Green.copy(alpha=.20f),Color.Transparent)),radius=w*.63f,center=Offset(-w*.17f,h*.36f))
 drawCircle(Brush.radialGradient(listOf(Blue.copy(alpha=.23f),Color.Transparent)),radius=w*.7f,center=Offset(w*1.1f,h*.72f))
 drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha=.15f),Color.Transparent)),radius=w*.6f,center=Offset(w*.5f,h*.19f))
 for(i in 0 until 30){
  val x=((i*59+7)%101)/101f*w;val y=((i*43+13)%97)/97f*h
  drawCircle(Gold.copy(alpha=.09f+(i%5)*.04f),(.5f+i%3).dp.toPx(),Offset(x,y))
 }
 }
}
