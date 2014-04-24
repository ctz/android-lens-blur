package io.jbp.depthimagetest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

public class DepthImage
{
  private static final String TAG = "DepthImage";

  public double blurAtInfinity;
  public double focalDistance;
  public double focalPointX;
  public double focalPointY;

  public String depthFormat;
  public String depthMime;
  public double depthNear;
  public double depthFar;
  public byte[] depthImage;

  public String colourMime;
  public byte[] colourImage;

  public boolean isValid()
  {
    return (blurAtInfinity != Double.NaN &&
        focalDistance != Double.NaN &&
        focalPointX != Double.NaN &&
        focalPointY != Double.NaN &&
        depthFormat != null &&
        depthMime != null &&
        depthNear != Double.NaN &&
        depthFar != Double.NaN &&
        depthImage != null &&
        colourMime != null && colourImage != null);
  }

  private Bitmap readBitmap(byte[] which, String label)
  {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inScaled = false;
    Bitmap bm = BitmapFactory.decodeByteArray(which, 0, which.length, opts);
    Log.v(TAG, String.format("%s bitmap is %dx%d pixels", label, opts.outWidth, opts.outHeight));
    return bm;
  }

  public Bitmap getColourBitmap()
  {
    return readBitmap(colourImage, "colour");
  }

  public Bitmap getDepthBitmap()
  {
    return readBitmap(depthImage, "depth");
  }
  
  private static String readStringAttr(String img, String attr)
  {
    Pattern pat = Pattern.compile(attr + "=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    Matcher m = pat.matcher(img);

    if (!m.find())
      return null;
    return m.group(1);
  }

  private static double readFloatAttr(String img, String attr)
  {
    String s = readStringAttr(img, attr);
    if (s == null)
      return Double.NaN;
    return Double.parseDouble(s);
  }

  private static byte[] readBase64Attr(String img, String attr)
  {
    String v = readStringAttr(img, attr);
    if (v == null)
      return null;
    return Base64.decode(v, Base64.DEFAULT);
  }

  private static String removeBlockHeaders(String img)
  {
    final String marker = readStringAttr(img, "xmpNote:HasExtendedXMP");
    if (marker == null)
      return img;

    Log.v(TAG, "XMP marker is " + marker);

    StringBuffer sb = new StringBuffer();

    Pattern pat = Pattern.compile("....http://ns.adobe.com/xmp/extension/." + marker + "........");
    Matcher m = pat.matcher(img);

    while (m.find())
      m.appendReplacement(sb, "");
    m.appendTail(sb);

    return sb.toString();
  }
  
  public static DepthImage loadImage(String img)
  {
    Log.v(TAG, "loadImage got " + img.length() + " bytes");
    img = removeBlockHeaders(img);
    Log.v(TAG, "removed block headers (" + img.length() + " bytes)");

    DepthImage di = new DepthImage();
    
    di.colourMime = readStringAttr(img, "GImage:Mime");
    if (di.colourMime == null)
      return di; /* Early fail */

    di.blurAtInfinity = readFloatAttr(img, "GFocus:BlurAtInfinity");
    di.focalDistance = readFloatAttr(img, "GFocus:FocalDistance");
    di.focalPointX = readFloatAttr(img, "GFocus:FocalPointX");
    di.focalPointY = readFloatAttr(img, "GFocus:FocalPointY");

    di.depthFormat = readStringAttr(img, "GDepth:Format");
    di.depthMime = readStringAttr(img, "GDepth:Mime");
    di.depthNear = readFloatAttr(img, "GDepth:Near");
    di.depthFar = readFloatAttr(img, "GDepth:Far");

    di.colourImage = readBase64Attr(img, "GImage:Data");
    di.depthImage = readBase64Attr(img, "GDepth:Data");

    return di;
  }
}
