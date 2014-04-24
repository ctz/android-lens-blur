package io.jbp.depthimagetest;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class Util
{
  private static final String TAG = "Util";
  
  public static String readInputStream(InputStream is)
  {
    StringBuffer sb = new StringBuffer();
    byte[] buf = new byte[1024];
    
    long start = System.currentTimeMillis();

    try
    {
      while (true)
      {
        int r = is.read(buf, 0, buf.length);
        if (r == -1)
          break;
        sb.append(new String(buf, 0, r, "ISO-8859-1"));
      }
    } catch (IOException e)
    {
      throw new Error("cannot read input stream", e);
    }
    
    Log.v(TAG, "loading file took " + (System.currentTimeMillis() - start) + "ms");
    
    return sb.toString();
  }
}
