package za.co.impilodrilling.register
import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.pdf.PdfDocument;import android.os.Bundle;import android.view.*;import android.widget.*;import androidx.activity.OnBackPressedCallback;import androidx.appcompat.app.AppCompatActivity;import androidx.core.content.FileProvider;import androidx.activity.result.contract.ActivityResultContracts;import android.text.InputType;import org.json.JSONArray;import java.io.File;import java.text.SimpleDateFormat;import java.util.*
class MainActivity:AppCompatActivity(){
 private val prefs by lazy{getSharedPreferences("impilo_register",MODE_PRIVATE)}
 private val staff=mutableListOf<String>()
 private val archived=mutableListOf<String>()
 private val noteEdits=linkedMapOf<String,EditText>()
 private val originalNotes=linkedMapOf<String,String>()
 private lateinit var content:FrameLayout
 private var selectedDate=today()
 private var selectedStaff:String?=null
 private var copiedNote:String?=null
 private var pendingPdf:File?=null
 private var screen="home"
 private val green=Color.rgb(11,95,70)
 private val dark=Color.rgb(28,36,33)
 private val muted=Color.rgb(95,105,101)
 private val savePdfLauncher=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
  val source=pendingPdf
  pendingPdf=null
  if(uri!=null&&source!=null)try{
   val destination=contentResolver.openOutputStream(uri)?:throw java.io.IOException("No output stream")
   destination.use{output->source.inputStream().use{input->input.copyTo(output)}}
   msg("PDF saved successfully")
  }catch(e:Exception){msg("Unable to save PDF")}
 }
 override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);content=findViewById(R.id.content);loadStaff();selectedStaff=staff.firstOrNull();findViewById<Button>(R.id.navHome).setOnClickListener{guard{showHome()}};findViewById<Button>(R.id.navRegister).setOnClickListener{guard{showRegister()}};findViewById<Button>(R.id.navStaff).setOnClickListener{guard{showStaff()}};onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){if(dirty())unsaved{finish()}else if(screen!="home")showHome() else finish()}});showHome()}
 private fun base(title:String):LinearLayout{val sc=ScrollView(this);val root=LinearLayout(this);root.orientation=LinearLayout.VERTICAL;root.setPadding(dp(20),dp(18),dp(20),dp(28));sc.addView(root);content.removeAllViews();content.addView(sc);root.addView(tv("IMPILO DRILLING",13,true,green));root.addView(tv(title,30,true,dark).apply{setPadding(0,0,0,dp(16))});return root}
 private fun showHome(){
  screen="home";noteEdits.clear();originalNotes.clear()
  val r=base("Staff Register")
  val h=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  r.addView(tv(if(h<12)"Good Morning" else if(h<17)"Good Afternoon" else "Good Evening",22,true,dark))
  r.addView(tv(SimpleDateFormat("EEEE, d MMMM yyyy",Locale.getDefault()).format(Date()),15,false,muted).apply{setPadding(0,0,0,dp(14))})
  val td=today()
  val done=staff.count{!prefs.getString(key(it,td),"").isNullOrBlank()}
  card(r,"STAFF MEMBERS",staff.size.toString(),"Add, remove or restore workers"){showStaff()}
  card(r,"TODAY'S RECORDS",done.toString()+" / "+staff.size,"Workers with notes recorded today"){selectedDate=td;showRegister()}
  card(r,"DAILY REGISTER","OPEN CALENDAR","Update all workers for one date"){selectedDate=td;showRegister()}
  card(r,"COMBINED PDF","DAILY REPORT","One document containing all workers"){showRegister()}
 }
 private fun showRegister(){
  screen="register";noteEdits.clear();originalNotes.clear()
  val r=base("Daily Register")
  r.addView(tv("ONE CALENDAR · ALL WORKERS",12,true,green))
  r.addView(tv("Choose a date, update each worker, then save all notes together.",14,false,muted).apply{setPadding(0,dp(6),0,dp(8))})
  val calendar=CalendarView(this)
  calendar.date=(SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(selectedDate)?:Date()).time
  r.addView(calendar,LinearLayout.LayoutParams(-1,dp(305)))
  val heading=tv(pretty(selectedDate),19,true,dark)
  heading.setPadding(0,dp(12),0,dp(14))
  r.addView(heading)
  calendar.setOnDateChangeListener{_,year,month,day->
   val next=String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day)
   if(next!=selectedDate)guard{selectedDate=next;showRegister()}
  }
  if(staff.isEmpty()){
   r.addView(tv("No workers added yet.",17,true,dark))
   r.addView(btn("+ ADD WORKER"){showStaff()})
   return
  }
  r.addView(tv("WORKERS ("+staff.size+")",12,true,muted))
  staff.toList().forEach{name->
   val box=LinearLayout(this)
   box.orientation=LinearLayout.VERTICAL
   box.setPadding(dp(14),dp(12),dp(14),dp(14))
   box.setBackgroundColor(Color.WHITE)
   box.addView(tv(name,18,true,dark))
   val ed=EditText(this)
   ed.inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
   ed.gravity=Gravity.TOP
   ed.minLines=3
   ed.hint="Daily notes for "+name
   ed.setTextColor(dark)
   ed.setHintTextColor(muted)
   ed.setPadding(dp(8),dp(10),dp(8),dp(10))
   val saved=prefs.getString(key(name,selectedDate),"")?:""
   ed.setText(saved)
   noteEdits[name]=ed
   originalNotes[name]=saved
   box.addView(ed,LinearLayout.LayoutParams(-1,-2))
   val actions=LinearLayout(this)
   actions.orientation=LinearLayout.HORIZONTAL
   val copy=btn("COPY NOTE"){
    val text=ed.text.toString()
    if(text.isBlank())msg("Write a note before copying")
    else{copiedNote=text;msg("Copied note from "+name)}
   }
   val paste=btn("PASTE NOTE"){
    val text=copiedNote
    if(text==null)msg("Copy a worker's note first")
    else{ed.setText(text);ed.setSelection(ed.text.length);msg("Pasted into "+name)}
   }
   copy.textSize=12f;paste.textSize=12f
   actions.addView(copy,LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(0,0,dp(5),0)})
   actions.addView(paste,LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(5),0,0,0)})
   box.addView(actions)
   r.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(9),0,dp(5))})
  }
  r.addView(btn("SAVE ALL WORKERS"){saveAll()})
  r.addView(tv("Export saves the current day's notes first.",13,false,muted).apply{setPadding(0,dp(12),0,dp(3))})
  r.addView(btn("SAVE ONE PDF"){saveAll(false);createPdf()?.let{pendingPdf=it;savePdfLauncher.launch(it.name)}})
  r.addView(btn("SHARE ONE PDF"){saveAll(false);createPdf()?.let{sharePdf(it)}})
  r.addView(btn("MANAGE WORKERS"){guard{showStaff()}})
 }
 private fun showStaff(){
  screen="staff";noteEdits.clear();originalNotes.clear()
  val r=base("Staff Members")
  r.addView(tv("ACTIVE WORKERS ("+staff.size+")",12,true,green))
  if(staff.isEmpty())r.addView(tv("No active workers. Add a worker to start.",15,false,muted))
  staff.toList().forEach{name->
   val box=LinearLayout(this)
   box.orientation=LinearLayout.VERTICAL
   box.setPadding(dp(14),dp(12),dp(14),dp(12))
   box.setBackgroundColor(Color.WHITE)
   box.addView(tv(name,18,true,dark))
   val actions=LinearLayout(this)
   actions.orientation=LinearLayout.HORIZONTAL
   actions.addView(btn("EDIT"){staffDialog(name)},LinearLayout.LayoutParams(0,-2,1f))
   actions.addView(btn("REMOVE"){archiveStaff(name)},LinearLayout.LayoutParams(0,-2,1f))
   box.addView(actions)
   r.addView(box,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(9),0,dp(2))})
  }
  r.addView(btn("+ ADD WORKER"){staffDialog(null)})
  if(archived.isNotEmpty()){
   r.addView(tv("REMOVED WORKERS",12,true,muted).apply{setPadding(0,dp(18),0,dp(8))})
   r.addView(tv("Past entries are preserved. Restore workers when needed.",13,false,muted))
   archived.toList().forEach{name->
    val row=LinearLayout(this)
    row.orientation=LinearLayout.HORIZONTAL
    row.gravity=Gravity.CENTER_VERTICAL
    row.addView(tv(name,16,true,dark),LinearLayout.LayoutParams(0,-2,1f))
    row.addView(btn("RESTORE"){archived.remove(name);staff.add(name);saveStaff();showStaff()})
    r.addView(row,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,dp(7),0,dp(5))})
   }
  }
 }
 private fun staffDialog(old:String?){
  val input=EditText(this)
  input.setText(old?:"")
  input.hint="Worker name"
  input.setSingleLine(true)
  input.setTextColor(dark)
  input.setPadding(dp(16),dp(12),dp(16),dp(12))
  val dialog=AlertDialog.Builder(this)
   .setTitle(if(old==null)"ADD WORKER" else "EDIT WORKER")
   .setView(input)
   .setNegativeButton("CANCEL",null)
   .setPositiveButton("SAVE",null)
   .create()
  dialog.setOnShowListener{
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(green)
   dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(green)
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
    val name=input.text.toString().trim()
    when{
     name.isBlank()->msg("Enter a worker name")
     staff.any{it.equals(name,true)&&!it.equals(old,true)}->msg("Worker already exists")
     archived.any{it.equals(name,true)}->msg("Restore this worker from the removed list")
     old==null->{staff.add(name);saveStaff();selectedStaff=name;dialog.dismiss();showStaff()}
     else->{
      migrate(old,name)
      val index=staff.indexOf(old)
      if(index>=0)staff[index]=name
      if(selectedStaff==old)selectedStaff=name
      saveStaff()
      dialog.dismiss()
      showStaff()
     }
    }
   }
  }
  dialog.show()
 }
 private fun archiveStaff(name:String){
  AlertDialog.Builder(this)
   .setTitle("REMOVE "+name.uppercase(Locale.getDefault())+"?")
   .setMessage("Remove from the current worker list? Existing daily records will be kept for historical PDFs. You can restore this worker later.")
   .setNegativeButton("CANCEL",null)
   .setPositiveButton("REMOVE"){_,_->
    staff.remove(name)
    if(!archived.contains(name))archived.add(name)
    if(selectedStaff==name)selectedStaff=staff.firstOrNull()
    saveStaff()
    showStaff()
   }.show()
 }
 private fun dirty()=screen=="register"&&noteEdits.any{(name,ed)->ed.text.toString()!=originalNotes[name]}
 private fun guard(action:()->Unit){if(dirty())unsaved(action)else action()}
 private fun unsaved(action:()->Unit){
  AlertDialog.Builder(this).setTitle("UNSAVED CHANGES")
   .setMessage("Save all worker notes for "+pretty(selectedDate)+"?")
   .setPositiveButton("SAVE"){_,_->saveAll(false);action()}
   .setNeutralButton("DISCARD"){_,_->action()}
   .setNegativeButton("CANCEL",null).show()
 }
 private fun saveAll(showMessage:Boolean=true){
  if(noteEdits.isEmpty())return
  val editor=prefs.edit()
  noteEdits.forEach{(name,field)->
   val value=field.text.toString()
   editor.putString(key(name,selectedDate),value)
   originalNotes[name]=value
  }
  editor.apply()
  if(showMessage)msg("All worker notes saved")
 }
 private fun wrapPdfText(text:String,paint:Paint,width:Float):List<String>{
  val result=mutableListOf<String>()
  text.replace("\r","").split("\n").forEach{paragraph->
   if(paragraph.isBlank()){result.add("");return@forEach}
   var line=""
   paragraph.trim().split(Regex("\\s+")).forEach{word->
    var remaining=word
    while(remaining.isNotEmpty()){
     val candidate=if(line.isEmpty())remaining else line+" "+remaining
     if(paint.measureText(candidate)<=width){line=candidate;remaining=""}
     else if(line.isNotEmpty()){result.add(line);line=""}
     else{
      var length=1
      while(length<remaining.length&&paint.measureText(remaining.substring(0,length+1))<=width)length++
      result.add(remaining.substring(0,length))
      remaining=remaining.substring(length)
     }
    }
   }
   if(line.isNotEmpty())result.add(line)
  }
  return result
 }
 private fun createPdf():File?{
  val people=(staff+archived.filter{prefs.contains(key(it,selectedDate))}).distinct()
  if(people.isEmpty()){msg("Add a worker before exporting");return null}
  val doc=PdfDocument()
  try{
   val paint=Paint(Paint.ANTI_ALIAS_FLAG)
   var page:PdfDocument.Page?=null
   var canvas:Canvas?=null
   var y=0f
   var pageNumber=0
   fun finishPage(){
    page?.let{
     paint.color=muted
     paint.typeface=Typeface.DEFAULT
     paint.textSize=9f
     canvas!!.drawText("Impilo Drilling  |  Generated "+SimpleDateFormat("dd MMM yyyy HH:mm",Locale.getDefault()).format(Date())+"  |  Page "+pageNumber,44f,815f,paint)
     doc.finishPage(it)
     page=null
    }
   }
   fun nextPage(){
    finishPage()
    pageNumber++
    page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNumber).create())
    canvas=page!!.canvas
    y=53f
    paint.color=green
    paint.typeface=Typeface.DEFAULT_BOLD
    paint.textSize=20f
    canvas!!.drawText("IMPILO DRILLING",44f,y,paint)
    y+=25f
    paint.color=dark
    paint.textSize=14f
    canvas!!.drawText("DAILY STAFF REGISTER",44f,y,paint)
    y+=22f
    paint.color=muted
    paint.typeface=Typeface.DEFAULT
    paint.textSize=10f
    canvas!!.drawText(pretty(selectedDate),44f,y,paint)
    y+=32f
   }
   fun writeLine(text:String,size:Float=11f,bold:Boolean=false,color:Int=dark,gap:Float=7f){
    if(y+size+gap>785f)nextPage()
    paint.color=color
    paint.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    paint.textSize=size
    canvas!!.drawText(text,44f,y,paint)
    y+=size+gap
   }
   nextPage()
   val completed=people.count{!prefs.getString(key(it,selectedDate),"").isNullOrBlank()}
   writeLine("Workers: "+people.size+"     Notes entered: "+completed,11f,true)
   y+=12f
   people.forEachIndexed{index,name->
    if(y+55f>785f)nextPage()
    writeLine((index+1).toString()+". "+name,13f,true,green,9f)
    val notes=prefs.getString(key(name,selectedDate),"")?:""
    paint.typeface=Typeface.DEFAULT
    paint.textSize=11f
    val lines=wrapPdfText(if(notes.isBlank())"No note recorded" else notes,paint,505f)
    lines.forEachIndexed{lineIndex,line->
     if(y+17f>785f){
      nextPage()
      writeLine(name+" (continued)",11f,true,green)
     }
     writeLine(if(line.isEmpty())" " else line,11f,false,if(notes.isBlank())muted else dark,6f)
    }
    y+=12f
   }
   finishPage()
   val folder=File(cacheDir,"reports")
   folder.mkdirs()
   val file=File(folder,"Impilo_Daily_Register_"+selectedDate+".pdf")
   file.outputStream().use{doc.writeTo(it)}
   return file
  }catch(e:Exception){
   msg("Unable to create PDF: "+(e.localizedMessage?:"Unknown error"))
   return null
  }finally{doc.close()}
 }
 private fun sharePdf(file:File){
  try{
   val uri=FileProvider.getUriForFile(this,packageName+".provider",file)
   val share=Intent(Intent.ACTION_SEND).apply{
    type="application/pdf"
    putExtra(Intent.EXTRA_STREAM,uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
   }
   startActivity(Intent.createChooser(share,"Share daily register PDF"))
  }catch(e:Exception){msg("Unable to share PDF")}
 }
 private fun migrate(old:String,new:String){val e=prefs.edit();prefs.all.filterKeys{it.startsWith("note|"+old+"|")}.forEach{(k,v)->e.putString(k.replaceFirst("note|"+old+"|","note|"+new+"|"),v as? String?);e.remove(k)};e.apply()}
 private fun key(n:String,d:String)="note|"+n+"|"+d;private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());private fun pretty(d:String)=SimpleDateFormat("EEEE, d MMMM yyyy",Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d)?:Date())
 private fun loadStaff(){
  staff.clear();archived.clear()
  val active=JSONArray(prefs.getString("staff","[]"))
  for(i in 0 until active.length())staff.add(active.getString(i))
  val inactive=JSONArray(prefs.getString("archived_staff","[]"))
  for(i in 0 until inactive.length())if(!staff.contains(inactive.getString(i)))archived.add(inactive.getString(i))
 }
 private fun saveStaff(){
  val active=JSONArray();staff.forEach{active.put(it)}
  val inactive=JSONArray();archived.forEach{inactive.put(it)}
  prefs.edit().putString("staff",active.toString()).putString("archived_staff",inactive.toString()).apply()
 }
 private fun card(r:LinearLayout,title:String,value:String,sub:String,click:()->Unit){val b=LinearLayout(this);b.orientation=LinearLayout.VERTICAL;b.setPadding(dp(18),dp(16),dp(18),dp(16));b.setBackgroundColor(Color.WHITE);b.isClickable=true;b.setOnClickListener{click()};b.addView(tv(title,12,true,green));b.addView(tv(value,25,true,dark));b.addView(tv(sub,13,false,muted));r.addView(b,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))})}
 private fun tv(s:String,z:Int,b:Boolean,c:Int)=TextView(this).apply{text=s;textSize=z.toFloat();setTextColor(c);if(b)setTypeface(typeface,Typeface.BOLD)}
 private fun btn(s:String,click:()->Unit)=Button(this).apply{text=s;setTextColor(Color.WHITE);backgroundTintList=android.content.res.ColorStateList.valueOf(green);setOnClickListener{click()}}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();private fun msg(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}