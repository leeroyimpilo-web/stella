package za.co.lottolab.studio;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only, explicitly granted access to individual user-created share files. */
public final class ShareProvider extends ContentProvider {
  @Override
  public boolean onCreate() {
    return true;
  }

  private File file(Uri uri) throws FileNotFoundException {
    String name = uri.getLastPathSegment();
    if (name == null || name.startsWith(".") || !name.matches("[A-Za-z0-9._-]+"))
      throw new FileNotFoundException();
    File f = new File(new File(getContext().getCacheDir(), "share"), name);
    if (!f.isFile()) throw new FileNotFoundException();
    return f;
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
    return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
    try {
      File f = file(uri);
      String[] cols =
          projection == null
              ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
              : projection;
      MatrixCursor c = new MatrixCursor(cols);
      Object[] row = new Object[cols.length];
      for (int i = 0; i < cols.length; i++)
        row[i] =
            OpenableColumns.DISPLAY_NAME.equals(cols[i])
                ? f.getName()
                : OpenableColumns.SIZE.equals(cols[i]) ? f.length() : null;
      c.addRow(row);
      return c;
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @Override
  public String getType(Uri uri) {
    return uri.toString().endsWith(".pdf") ? "application/pdf" : "text/csv";
  }

  @Override
  public Uri insert(Uri u, ContentValues v) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Uri u, String s, String[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Uri u, ContentValues v, String s, String[] a) {
    throw new UnsupportedOperationException();
  }
}
