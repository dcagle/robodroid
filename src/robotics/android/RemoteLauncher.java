package robotics.android;

/*
import robotics.android.lib.GPSWidget;
import robotics.android.lib.BluetoothWidget;
import robotics.android.lib.WifiWidget;
import robotics.android.lib.MotorDevice;
import robotics.android.lib.Joystick;
import robotics.android.lib.Output;
*/
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * 
 * @author dpcagle
 * 
 * TODO: Task List
 *	/ Draw images that will be shown on screen.
 *  / Incorporate bluetooth support
 *  - Incorporate wifi support
 *  x Figure out best way to transfer information to/from external device. (Wifi)
 *  x Provide tab layout with 3 tabs - ??? (motors + sonars), Navigation (GPS + compass), Output
 *  	???
 *  		x Incorporate Accelerometer support
 *  		x Create functional d-pad that uses either button press or accelerometer + sends events
 *  		x Create sonar information and update using events
 *  	Navigation
 *  		- Interpret latitude/longitude from event (use Location.bearingTo(Location) to debug)
 *  		- Create Waypoint Editor dialog (similar to current one in C# code [BarbieCarGPS])
 *  		- Determine correct bearing from the initial location and target location (I think this doesn't work in C# code)
 *  		- Create working compass from event
 *  	Output
 *  		- Create filtering options (Motors/Sonars/GPS/Compass/Input/Output/All/etc...)
 *  		- Create an optimized way to display information on the screen.
 *	- Create connection dialog to display if auto-connection settings do not work properly.
 */
public class RemoteLauncher extends TabActivity implements WifiListener
{
	private GPSWidget gps;
	//private BluetoothWidget bluetooth;
	private WifiWidget wifi;
	private MotorDevice motor;
	//private Joystick motor;
	private WakeLock lock;
	
	private CheckBox remCheck;
	private ListView prevDevices;
	private String prevDst;
	private int prevPort;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        new Output(this);
        motor = (MotorDevice)findViewById(R.id.MotorDevice);
        //motor = (Joystick)findViewById(R.id.joystick);
        gps = (GPSWidget)findViewById(R.id.gps_module);
        //bluetooth = (BluetoothWidget)findViewById(R.id.bt_module);
        //bluetooth.setParentActivity(this);
        wifi = (WifiWidget)findViewById(R.id.wifi_module);
        
        remCheck = (CheckBox)findViewById(R.id.rem_check);
        prevDevices = (ListView)findViewById(R.id.prev_connects);
        
        //Initialize the tabs
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        
        spec = tabHost.newTabSpec(BluetoothWidget.id);
        spec.setIndicator("Initialization", res.getDrawable(R.drawable.ic_tab_artists));
        spec.setContent(R.id.Init);
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(Movement.id);
        spec.setIndicator("Movement", res.getDrawable(R.drawable.ic_tab_artists));
        spec.setContent(R.id.Movement);
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(Navigation.id);
        spec.setIndicator("Navigation", res.getDrawable(android.R.drawable.ic_dialog_map));
        spec.setContent(R.id.Navigation);
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(Output.id);
        spec.setIndicator("Output", res.getDrawable(R.drawable.ic_tab_artists));
        spec.setContent(R.id.Output);
        tabHost.addTab(spec);
        
        tabHost.setCurrentTab(0);
        
        //Initialize the wake lock
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Remote");
        
        //TODO: Load any saved previous devices and display them for the user
        String[] testMsgs = { "Computer 1", "Computer 2", "Computer 3" };
        prevDevices.setAdapter(new android.widget.ArrayAdapter<String>(this, 
        		R.layout.list_item, testMsgs));
        prevDevices.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
        	public void onItemClick(android.widget.AdapterView<?> item, android.view.View view, 
        			int pos, long id)
        	{
        		Toast.makeText(getApplicationContext(), "Selected item " + pos, 
        				Toast.LENGTH_SHORT);
        	}
        });
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	//Don't allow the screen to turn off while broadcasting/receiving events.
        lock.acquire();
        
        //Resume communication if terminated due to application pausing.
        if (prevDst != null && prevPort != 0)
        {
        	wifi.connect(prevDst, prevPort);
        	Toast.makeText(this, "Resuming communication with remote device!", Toast.LENGTH_SHORT);
        	prevDst = null;
        	prevPort = 0;
        }
    	
        motor.onResume(this);
        gps.onResume(this);
        //bluetooth.onResume(this);
    }
    
    @Override
    public void onPause()
    {
    	motor.onPause(this);
    	gps.onPause(this);
    	//bluetooth.onPause(this);
    	
    	//Terminate the connection safely
    	wifi.disconnect();
    	Toast.makeText(this, "Suspending communication with remote device!", Toast.LENGTH_SHORT);
    	prevDst = wifi.getConnectedName();
    	prevPort = wifi.getConnectedPort();
    	
    	//The activity is no longer sending events, so allow sleeping.
    	lock.release();
    	
    	super.onPause();
    }
    
    @Override
    public void onDestroy()
    {
    	// bluetooth.onDestroy(this);
    	super.onDestroy();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	//Check if the user is not allowing bluetooth functionality
    	//bluetooth.onActivityResult(requestCode, resultCode, this);
    }

	@Override
	public void messageReceived(byte[] args) {}

	@Override
	public void wifiConnected()
	{
		prevDevices.setVisibility(android.view.View.GONE);
		remCheck.setVisibility(android.view.View.VISIBLE);
	}

	@Override
	public void wifiDisconnected()
	{		
		remCheck.setVisibility(android.view.View.GONE);
		prevDevices.setVisibility(android.view.View.VISIBLE);
	}
}