package za.co.lottolab

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Gold = Color(0xFFFFD352)
internal val Green = Color(0xFF12D974)
internal val Blue = Color(0xFF258EFF)
internal val Navy = Color(0xFF061625)

@Composable fun Glass(modifier:Modifier=Modifier, tint:Color=Green, content:@Composable BoxScope.()->Unit) {
 val shape=RoundedCornerShape(24.dp)
 Box(modifier.shadow(10.dp,shape,ambientColor=tint.copy(alpha=.26f))
 .background(Brush.verticalGradient(listOf(tint.copy(alpha=.32f),Color(0xE4083038),Color(0xEF071823))),shape)
 .border(1.dp,Brush.linearGradient(listOf(Color.White.copy(alpha=.58f),tint.copy(alpha=.86f),Gold.copy(alpha=.6f))),shape),content=content)
}

@Composable fun Ball(number:Int,color:Color,size:Dp,seed:Int=0) {
 val motion=rememberInfiniteTransition(label="floating-ball")
 val move by motion.animateFloat(1f,-2.5f,infiniteRepeatable(tween(2300+seed*117,easing=FastOutSlowInEasing),RepeatMode.Reverse,StartOffset(seed*150)),label="drift")
 val dark=when(color){Gold->Color(0xFF965500);Green->Color(0xFF006437);else->Color(0xFF073571)}
 Box(Modifier.size(size).graphicsLayer{translationY=move.dp.toPx()}
 .shadow(6.dp,CircleShape,ambientColor=color,spotColor=color)
 .background(Brush.radialGradient(listOf(Color.White,color,color,dark)),CircleShape)
 .border(1.dp,Color.White.copy(alpha=.7f),CircleShape),contentAlignment=Alignment.Center){
 Box(Modifier.fillMaxSize(.61f)
 .background(Brush.verticalGradient(listOf(Color.White,Color(0xFFE1ECF7))),CircleShape)
 .border(.5.dp,Color.Black.copy(alpha=.3f),CircleShape),contentAlignment=Alignment.Center) {
 Text(number.toString(),fontSize=(size.value*.32f).sp,fontWeight=FontWeight.Black,color=Navy,maxLines=1)
 }
 Canvas(Modifier.fillMaxSize()){drawArc(Color.White.copy(alpha=.62f),205f,85f,false,Offset(size.width*.12f,size.height*.09f),Size(size.width*.73f,size.height*.38f),style=Stroke(width=size.width*.058f))}
 }
}

@Composable fun Orbit(modifier:Modifier=Modifier){
 Canvas(modifier){
 val w=size.width;val h=size.height
 drawArc(Brush.sweepGradient(listOf(Color.Transparent,Gold,Color.White,Gold,Color.Transparent)),15f,325f,false,Offset(w*.03f,h*.34f),Size(w*.94f,h*.56f),style=Stroke(2.6.dp.toPx()))
 drawArc(Gold.copy(alpha=.37f),175f,170f,false,Offset(w*.10f,h*.44f),Size(w*.80f,h*.45f),style=Stroke(1.5.dp.toPx()))
 for(i in 0..8){drawCircle(Gold.copy(alpha=.55f),((i%3)+1).dp.toPx(),Offset(((i*37+5)%91)/91f*w,(.52f+((i*13)%31)/100f)*h))}
 }
}

@Composable fun Crown(modifier:Modifier=Modifier){
 Canvas(modifier){
 val w=size.width;val h=size.height
 val p=Path().apply{moveTo(w*.06f,h*.22f);lineTo(w*.29f,h*.57f);lineTo(w*.5f,h*.12f);lineTo(w*.7f,h*.57f);lineTo(w*.94f,h*.22f);lineTo(w*.83f,h*.89f);lineTo(w*.17f,h*.89f);close()}
 drawPath(p,Brush.verticalGradient(listOf(Color(0xFFFFFFBA),Gold,Color(0xFFC3851B))))
 for(x in listOf(.06f,.5f,.94f)){drawCircle(Color(0xFFFFF2AF),w*.065f,Offset(w*x,h*.18f))}
 }
}
