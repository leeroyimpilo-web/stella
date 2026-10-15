package za.co.lottolab.studio;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.util.*;

public final class PdfReport {
  public static void write(OutputStream out, String text) throws IOException {
    PdfDocument pdf = new PdfDocument();
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
    p.setTextSize(10);
    java.util.List<String> wrapped = new ArrayList<>();
    for (String raw : text.split("\n", -1)) {
      String line = "";
      for (String word : raw.split(" ")) {
        String candidate = line.isEmpty() ? word : line + " " + word;
        if (p.measureText(candidate) > 497 && !line.isEmpty()) {
          wrapped.add(line);
          line = word;
        } else line = candidate;
        // A source URL or unbroken note must stay inside the A4 text column too.
        while (p.measureText(line) > 497) {
          int cut = p.breakText(line, true, 497, null);
          if (cut < 1) cut = 1;
          wrapped.add(line.substring(0, cut));
          line = line.substring(cut);
        }
      }
      wrapped.add(line);
    }
    int at = 0, pageNo = 1;
    while (at < wrapped.size()) {
      PdfDocument.Page page =
          pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNo).create());
      Canvas c = page.getCanvas();
      c.drawColor(Color.WHITE);
      p.setColor(ConsoleArt.BG);
      p.setTextSize(23);
      p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
      c.drawText("LottoLab / Research report", 44, 52, p);
      p.setTextSize(10);
      p.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
      float y = 84;
      while (at < wrapped.size() && y < 780) {
        c.drawText(wrapped.get(at++), 44, y, p);
        y += 15;
      }
      p.setColor(Color.DKGRAY);
      p.setTextSize(8);
      c.drawText("LottoLab 0.2 | Research history does not predict future draws", 44, 815, p);
      c.drawText(Integer.toString(pageNo++), 540, 815, p);
      pdf.finishPage(page);
    }
    try {
      pdf.writeTo(out);
    } finally {
      pdf.close();
    }
  }
}
