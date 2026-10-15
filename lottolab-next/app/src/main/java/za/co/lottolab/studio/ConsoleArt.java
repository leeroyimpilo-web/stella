package za.co.lottolab.studio;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Lightweight vector-native art: no screen-sized bitmap or WebView. */
public final class ConsoleArt {
    public static final int BG=Color.rgb(5,23,34), INK=Color.rgb(2,12,20), STEEL=Color.rgb(36,126,143), GREEN=Color.rgb(186,246,99), ORANGE=Color.rgb(255,139,49), TEXT=Color.rgb(238,245,245), MUTED=Color.rgb(157,193,204);
    public static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    public static final class Panel extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final float density;private final boolean orange;private final boolean selected;
        public Panel(Context c,boolean orange,boolean selected){density=c.getResources().getDisplayMetrics().density;this.orange=orange;this.selected=selected;}
        @Override public void draw(Canvas c){
            Rect r=getBounds();float inset=2*density,x=r.left+inset,y=r.top+inset,w=r.width()-inset*2,h=r.height()-inset*2,k=Math.min(10*density,h/4);
            Path path=new Path();path.moveTo(x+k,y);path.lineTo(x+w-k,y);path.lineTo(x+w,y+k);path.lineTo(x+w,y+h-k);path.lineTo(x+w-k,y+h);path.lineTo(x+k,y+h);path.lineTo(x,y+h-k);path.lineTo(x,y+k);path.close();
            int a=orange?Color.rgb(255,169,48):Color.rgb(15,67,82),b=orange?Color.rgb(244,105,25):Color.rgb(6,30,43);
            p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,y,0,y+h,a,b,Shader.TileMode.CLAMP));c.drawPath(path,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*density);p.setColor(INK);c.drawPath(path,p);
            p.setStrokeWidth(density);p.setColor(orange?Color.rgb(255,193,75):selected?GREEN:STEEL);c.drawPath(path,p);
            p.setColor(orange?Color.rgb(255,207,100):Color.rgb(37,104,121));p.setStrokeWidth(density);c.drawLine(x+k+4*density,y+4*density,x+w-k-4*density,y+4*density,p);
            p.setStyle(Paint.Style.FILL);p.setColor(INK);float rr=1.6f*density;for(float sx:new float[]{x+7*density,x+w-7*density})for(float sy:new float[]{y+8*density,y+h-8*density})if(h>37*density)c.drawCircle(sx,sy,rr,p);
        }
        @Override public void setAlpha(int a){p.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    public static final class Grid extends Drawable {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final float d;
        public Grid(Context c){d=c.getResources().getDisplayMetrics().density;}
        @Override public void draw(Canvas c){Rect r=getBounds();c.drawColor(BG);p.setColor(Color.rgb(13,43,59));p.setStrokeWidth(d*.6f);for(float x=0;x<r.width();x+=28*d)c.drawLine(x,0,x,r.height(),p);for(float y=0;y<r.height();y+=28*d)c.drawLine(0,y,r.width(),y,p);}
        @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.OPAQUE;}
    }
    public static final class Icon extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final String kind;private int color;
        public Icon(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();float size=Math.min(getWidth(),getHeight());canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/24f,size/24f);p.setColor(color);p.setStrokeWidth(1.9f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);Path a=new Path();
            switch(kind){
                case "home": a.moveTo(3,11);a.lineTo(12,3);a.lineTo(21,11);a.moveTo(5,10);a.lineTo(5,21);a.lineTo(10,21);a.lineTo(10,15);a.lineTo(14,15);a.lineTo(14,21);a.lineTo(19,21);a.lineTo(19,10);break;
                case "history": canvas.drawCircle(12,12,9,p);a.moveTo(12,6);a.lineTo(12,12);a.lineTo(16,15);break;
                case "vault": for(int x:new int[]{4,14})for(int y:new int[]{4,14})canvas.drawRoundRect(x,y,x+6,y+6,1,1,p);break;
                case "planner": canvas.drawRoundRect(3,5,21,21,2,2,p);a.moveTo(3,10);a.lineTo(21,10);a.moveTo(7,3);a.lineTo(7,7);a.moveTo(17,3);a.lineTo(17,7);break;
                case "menu": a.moveTo(4,6);a.lineTo(20,6);a.moveTo(4,12);a.lineTo(20,12);a.moveTo(4,18);a.lineTo(20,18);break;
                case "chevron":a.moveTo(9,5);a.lineTo(16,12);a.lineTo(9,19);break;
                case "plus":a.moveTo(12,4);a.lineTo(12,20);a.moveTo(4,12);a.lineTo(20,12);break;
                default:a.moveTo(9,3);a.lineTo(15,3);a.moveTo(10,3);a.lineTo(10,10);a.lineTo(4,20);a.quadTo(3,22,6,22);a.lineTo(18,22);a.quadTo(21,22,20,20);a.lineTo(14,10);a.lineTo(14,3);a.moveTo(7,16);a.lineTo(17,16);break;
            }canvas.drawPath(a,p);canvas.restore();
        }
    }
    public static final class Histogram extends View {
        private final int[] values;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        public Histogram(Context c,int[] values){super(c);this.values=values.clone();setContentDescription("Main-number frequencies. The full accessible counts are listed below the chart.");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight(),left=22*d,bottom=23*d,top=8*d;int max=1;for(int x:values)max=Math.max(max,x);
            float cw=w-left-5*d,ch=h-bottom-top;p.setTypeface(Typeface.MONOSPACE);p.setTextSize(10*d);
            for(int i=0;i<3;i++){float y=top+ch*i/2;p.setColor(Color.rgb(22,60,57));p.setStrokeWidth(d*.5f);c.drawLine(left,y,w,y,p);p.setColor(MUTED);c.drawText(Integer.toString(Math.round(max*(2-i)/2f)),0,y+4*d,p);}
            float step=cw/(values.length-1);p.setColor(GREEN);for(int n=1;n<values.length;n++){float x=left+(n-1)*step;p.setColor(GREEN);c.drawRect(x+step*.17f,top+ch-ch*values[n]/max,x+step*.83f,top+ch,p);if(n==1||n%10==0||n==values.length-1){p.setColor(MUTED);p.setTextAlign(Paint.Align.CENTER);c.drawText(Integer.toString(n),x+step/2,h-6*d,p);p.setTextAlign(Paint.Align.LEFT);}}
        }
    }
}
