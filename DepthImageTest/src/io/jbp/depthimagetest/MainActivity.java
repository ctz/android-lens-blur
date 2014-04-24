package io.jbp.depthimagetest;

import java.io.FileNotFoundException;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener
{
  private static final String TAG = "MainActivity";

  private GLSurfaceView surface;
  private DepthRenderer renderer;
  private SensorManager sensors;
  private Sensor rotationSensor;
  private boolean gyroEnabled;
  private boolean needImage;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    surface = (GLSurfaceView) findViewById(R.id.surface);
    surface.setVisibility(View.INVISIBLE);

    sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
    rotationSensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

    for (Sensor s : sensors.getSensorList(Sensor.TYPE_ALL))
    {
      Log.v(TAG, "sensor " + s.getName() + " vendor " + s.getVendor() + " type " + s.getType());
    }
    
    needImage = true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu m)
  {
    getMenuInflater().inflate(R.menu.main, m);

    m.findItem(R.id.action_gyro).setEnabled(rotationSensor != null);

    return true;
  }

  @Override
  public void onResume()
  {
    super.onResume();
    if (gyroEnabled)
      startSensors();
    if (needImage)
    {
      pickImage();
      needImage = false;
    }
  }

  @Override
  public void onPause()
  {
    super.onPause();
    if (gyroEnabled)
      stopSensors();
  }

  private void startSensors()
  {
    if (rotationSensor != null)
      sensors.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
  }

  private void stopSensors()
  {
    sensors.unregisterListener(this);
  }
  
  private void pickImage()
  {
    Toast.makeText(this, R.string.open_prompt, Toast.LENGTH_SHORT).show();
    
    Intent i = new Intent();
    i.setType("image/jpeg");
    i.setAction(Intent.ACTION_GET_CONTENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i, 0);
  }
  
  @Override
  public void onActivityResult(int requestCode, int result, Intent data)
  {
    if (requestCode == 0 && result == Activity.RESULT_OK)
    {
      try
      {
        String imageData = Util.readInputStream(getContentResolver().openInputStream(data.getData()));
        showImage(DepthImage.loadImage(imageData));
      } catch (FileNotFoundException e)
      {
        throw new Error("cannot open received image", e);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId())
    {
    case R.id.action_blur:
      item.setChecked(renderer.toggleBlur());
      return true;
      
    case R.id.action_show_depth:
      item.setChecked(renderer.toggleShowDepth());
      return true;

    case R.id.action_gyro:
      if (gyroEnabled)
        stopSensors();
      else
        startSensors();
      gyroEnabled = !gyroEnabled;
      item.setChecked(gyroEnabled);
      return true;
    }

    return false;
  }

  private void showImage(DepthImage img)
  {
    if (!img.isValid())
    {
      AlertDialog ad = new AlertDialog.Builder(this)
        .setMessage(R.string.no_depth_info)
        .setTitle(R.string.no_depth_info_title)
        .setNegativeButton(R.string.no_depth_info_cancel, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            finish();
          }
        })
        .setPositiveButton(R.string.no_depth_info_choose, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            pickImage();
          }
        })
        .show();
      Log.e(TAG, "loaded DepthImage is not valid");
      return;
    }

    renderer = new DepthRenderer(img.getColourBitmap(), img.getDepthBitmap(), getResources());

    surface.setEGLContextClientVersion(2);
    surface.setRenderer(renderer);
    surface.setVisibility(View.VISIBLE);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    float x = event.getRawX();
    float y = event.getRawY();
    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    /* event will be 0..1, adjust to -1..1 */
    if (renderer != null)
      renderer.setSkew((x / metrics.widthPixels) * 2f - 1f, (y / metrics.heightPixels) * 2f - 1f);

    return true;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy)
  {

  }

  @Override
  public void onSensorChanged(SensorEvent event)
  {
    if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR ||
        event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
      processRot(event);
  }

  private void processRot(SensorEvent ev)
  {
    float x = ev.values[0];
    float y = ev.values[1];
    float z = ev.values[2];
    Log.d(TAG, String.format("rot sensor %.2f,%.2f,%.2f", x, y, z));

    if (renderer != null)
      renderer.setSkew(y * 4f, x * -4f);
  }
}
