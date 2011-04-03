package robotics.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSWidget extends LinearLayout implements LocationListener, OnClickListener, 
	SensorEventListener
{
	private final static int REST_TIME = 100000;
	
	private Button setWaypoint;
	private Button viewMap;
	private EditText latitude;
	private EditText longitude;
	private TextView nextLat;
	private TextView nextLong;
	private TextView curBearing;
	private TextView tarBearing;
	private TextView rotation;
	private NavigationArrow arrow;
	
	private Location curLoc;
	private Location nextWaypt;
	private GeomagneticField geoField;
	private float[] accelerometer = new float[3];
	private float[] geomagnetic = new float[3];
	private MapWidget map;
	private Dialog dialog;
	
	public static class NavigationArrow extends ImageView
	{
		private int rotation;
		
		public NavigationArrow(Context context, AttributeSet attr)
		{
			super(context, attr);
			setImageResource(R.drawable.nav_arrow);
			rotation = 0;
		}
		
		public void setRotation(int newRotation)
		{
			rotation = newRotation;
			invalidate();
		}
		
		@Override
		public void onDraw(android.graphics.Canvas canvas)
		{
			canvas.save();
			canvas.rotate(rotation, getWidth() / 2, getHeight() / 2);
			super.onDraw(canvas);
			canvas.restore();
		}
	}
	
	public GPSWidget(Context context, AttributeSet attr)
	{
		super(context, attr);
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.gps_widget, this);
		
		setWaypoint = (Button)findViewById(R.id.waypoint_set);
		setWaypoint.setOnClickListener(this);
		viewMap = (Button)findViewById(R.id.view_map);
		viewMap.setOnClickListener(this);
		latitude = (EditText)findViewById(R.id.latitude);
		longitude = (EditText)findViewById(R.id.longitude);
		nextLat = (TextView)findViewById(R.id.next_lat);
		nextLong = (TextView)findViewById(R.id.next_long);
		curBearing = (TextView)findViewById(R.id.gps_cur_bearing);
		tarBearing = (TextView)findViewById(R.id.gps_tar_bearing);
		arrow = (NavigationArrow)findViewById(R.id.gps_nav_arrow);
		rotation = (TextView)findViewById(R.id.gps_arrow_rotation);
		
		map = new MapWidget(context);
		dialog = new Dialog(context);
		dialog.setContentView(map);
		dialog.setTitle("Map");
	}
	
	public void onResume(Activity parent)
	{
		LocationManager lManager = (LocationManager)parent.getSystemService(
				Context.LOCATION_SERVICE);
		lManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REST_TIME, 0, this);
		
		SensorManager sManager = (SensorManager)parent.getSystemService(Context.SENSOR_SERVICE);
		java.util.List<Sensor> sensors = sManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensors.size() > 0)
		{
			sManager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
		}
    	sensors = sManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
    	if (sensors.size() > 0)
    	{
    		sManager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
    	}
	}
	
	public void onPause(Activity parent)
	{
		LocationManager lManager = (LocationManager)parent.getSystemService(
				Context.LOCATION_SERVICE);
		lManager.removeUpdates(this);
		
    	SensorManager sManager = (SensorManager)parent.getSystemService(Context.SENSOR_SERVICE);
    	sManager.unregisterListener(this);
	}

	@Override
	public void onLocationChanged(Location location)
	{
		geoField = new GeomagneticField(
				Double.valueOf(location.getLatitude()).floatValue(),
				Double.valueOf(location.getLongitude()).floatValue(),
				Double.valueOf(location.getAltitude()).floatValue(),
				System.currentTimeMillis());
					
		curLoc = location;
		latitude.setText(Double.toString(location.getLatitude()).substring(0, 8));
		longitude.setText(Double.toString(location.getLongitude()).substring(0, 10));
		map.setLocation(location);
			
		//Update the current bearing field
		if (nextWaypt != null)
		{
			//tarBearing.setText("bearing to target: " + (int)curLoc.bearingTo(nextWaypt));
		}
	}

	@Override
	public void onProviderDisabled(String provider)
	{
		if (provider.equals(LocationManager.GPS_PROVIDER))
		{
			latitude.setEnabled(false);
			longitude.setEnabled(false);
		}
	}

	@Override
	public void onProviderEnabled(String provider)
	{ 
		if (provider.equals(LocationManager.GPS_PROVIDER))
		{
			latitude.setEnabled(true);
			longitude.setEnabled(true);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onClick(View v)
	{
		if (v.getId() == R.id.waypoint_set)
		{
			nextWaypt = curLoc;
			nextLat.setText(latitude.getText() + " / ");
			nextLong.setText(longitude.getText());
			map.setTarget(nextWaypt);
		}
		else if (v.getId() == R.id.view_map)
		{
			/* 
			Dialog dialog = new Dialog(v.getContext());
			dialog.setContentView(map);
			dialog.setTitle("Map");
			*/
			dialog.show();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) { }

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			accelerometer = event.values.clone();
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			geomagnetic = event.values.clone();
		}
		
		if (accelerometer != null && geomagnetic != null)
		{
			float[] r = new float[9];
			float[] i = new float[9];
			
			if (SensorManager.getRotationMatrix(r, i, accelerometer, geomagnetic))
			{
				float[] remap = new float[9];
				//SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X, SensorManager.AXIS_Y, 
				SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
						remap);
				float[] actualOrientation = new float[3];
				SensorManager.getOrientation(remap, actualOrientation);	
				
				curBearing.setText("current bearing: " + (int)(actualOrientation[0] * 180 / Math.PI));
				if (curLoc != null && nextWaypt != null && geoField != null)
				{
					float bearing = curLoc.bearingTo(nextWaypt);
					float heading = (float)(actualOrientation[0] * 180 / Math.PI) + 
										geoField.getDeclination();
					//heading = bearing - (bearing + heading);
					heading = heading - bearing;

					/*
					tarBearing.setText("bearing to target: " + (int)bearing);
					arrow.setRotation(Math.round(bearing % 360 + 180));
					rotation.setText("arrow rotation: " + Math.round(bearing % 360 + 180));
					*/
					tarBearing.setText("bearing to target: " + (int)heading);
					arrow.setRotation(Math.round(heading % 360));
					rotation.setText("arrow rotation: " + Math.round(heading % 360));
				}
				
				/*
				curBearing.setText("current bearing: " + (int)(actualOrientation[0] * 180 / 
						Math.PI));
				if (curLoc != null && nextWaypt != null)
				{
					int arrowRot = (int)(curLoc.bearingTo(nextWaypt) - actualOrientation[0] * 
							180 / Math.PI);
					tarBearing.setText("bearing to target: " + (int)curLoc.bearingTo(nextWaypt));
					//arrow.setRotation((int)(curLoc.bearingTo(nextWaypt) - actualOrientation[0] * 
					//		180 / Math.PI) - 90);
					arrow.setRotation(arrowRot);
					rotation.setText("arrow rotation: " + arrowRot);
				}
				*/
			}
		}
	}
}
