package robotics.android;

import java.util.ArrayList;

import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Transmitter
{
	private static BluetoothAdapter adapter;
	private static DeviceListener deviceListener;
	private static DeviceList devices;
	private static CommThread commThread;
	
	//Flags to indicate the status of the phone's current bluetooth situation
	private static boolean btActive = false;
	
	/**
	 * Creates a list that aids the user in selecting a device to connect to.
	 */
	public static class DeviceList extends ListView implements OnItemClickListener
	{
		private ArrayAdapter<String> items;
		private ArrayList<BluetoothDevice> devices;
		
		public DeviceList(Context context, android.util.AttributeSet attr)
		{
			super(context, attr);
			
			items = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice);
			setAdapter(items);
			setOnItemClickListener(this);
			devices = new ArrayList<BluetoothDevice>();
			setChoiceMode(CHOICE_MODE_SINGLE);
		}
		
		public void clear()
		{
			items.clear();
			devices.clear();
		}
		
		public void addDevice(BluetoothDevice device)
		{
			items.add(device.getName() + " (" + device.getAddress() + ")");
			this.devices.add(device);
		}
		
		public void clearSelection()
		{
			clearChoices();
		}
		
		@Override
		public void onItemClick(AdapterView<?> av, View view, int pos, long id)
		{
			cancelDiscovery();
			Log.v("DeviceList", "Attempting to connect to " + devices.get(pos).getName());
			connectDevice(devices.get(pos));
		}
	}
	
	/**
	 * Listens for feedback from the system about important bluetooth related events.
	 */
	private static class DeviceListener extends BroadcastReceiver
	{
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			
			if (BluetoothDevice.ACTION_FOUND.equals(action)) //When discovery finds a device
			{
				//Get the BluetoothDevice object from the Intent and pass it to the device 
				//  selection dialog
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				devices.addDevice(device);
			}
			
			else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) 
			{
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				if (state == BluetoothAdapter.STATE_TURNING_OFF)
				{
					bluetoothDisabled();
				}
				else if (state == BluetoothAdapter.STATE_ON)
				{
					bluetoothEnabled();
				}
			}
			
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
			{
				cancelDiscovery();
				BluetoothWidget.discoveryFinished();
			}
		}
	}
	
	/**
	 * Initializes and sets up the Bluetooth transmitter features for a parent Activity.
	 * Only one Activity is supported at this time.
	 * 
	 * Requires the following xml elements: (Transmitter.DeviceList)bt_devices
	 * @param context The Context requesting the Bluetooth transmitter features.
	 * @param list The list of potential devices in range.
	 */
	public static void initialize(Context context, DeviceList list)
	{
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null)
		{
			deviceListener = new DeviceListener();
			devices = list;
			
			//Listens for discoverable devices
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			context.registerReceiver(deviceListener, filter);
			
			//Listens for bluetooth status changes
			filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
			context.registerReceiver(deviceListener, filter);
			
			//Let the program know when the discovery process has completed
			filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			context.registerReceiver(deviceListener, filter);
			
			if (adapter.isEnabled())
			{
				//If bluetooth was already enabled, call bluetoothEnabled() immediately
				bluetoothEnabled();
			}
		}
		else
		{
			//There is no bluetooth device on this phone, so can't do anything
			Toast.makeText(context, "Device doesn't support bluetooth!", Toast.LENGTH_LONG).show();
		}
	}
	
	public static void bluetoothEnabled()
	{
		btActive = true;
	}
	
	public static void bluetoothDisabled()
	{
		if (btActive)
		{
			if (commThread != null)
			{
				if (commThread.isAlive())
				{
					commThread.cancel();
				}
			}
			btActive = false;
		}
	}
	
	public static void startDiscovery()
	{
		if (btActive)
		{
			if (adapter.isDiscovering())
			{
				adapter.cancelDiscovery();
			}
			
			devices.clear();
			adapter.startDiscovery();
		}
	}
	
	public static void cancelDiscovery()
	{
		if (btActive)
		{
			if (adapter.isDiscovering())
			{
				adapter.cancelDiscovery();
			}
		}
	}
	
	public static void send(byte[] code)
	{
		if (commThread != null)
		{
			if (commThread.isAlive())
			{
				Log.i("Transmitter", "Attempting to send byte...");
				commThread.write(code);
			}
		}
	}
	
	private static void connectDevice(BluetoothDevice device)
	{
		Output.writeMessage("Connecting to " + device.getName() + " (" + device.getAddress() + ")");
		Log.i("Transmitter", "Connecting to " + device.getName() + " (" + device.getAddress() + ")");
		
		Handler handler = new Handler() 
		{
			public void handleMessage(Message message)
			{
				if (message.what == CommThread.CON_STATUS)
				{
					if (message.arg1 == CommThread.CON_START)
					{
						BluetoothWidget.connectionStarted();
					}
					else
					{
						BluetoothWidget.connectionFinished();
						if (message.arg2 == CommThread.CON_ERROR)
						{
							Output.writeMessage("Error while connecting to device!");
							devices.clearSelection();
						}
						else
						{
							Output.writeMessage("Connected!");
						}
					}
				}
				else
				{
					Output.writeMessage("Incoming Data: " + message);
				}
			}
		};
		commThread = new CommThread(device, handler);
		commThread.start();
	}
	
	/**
	 * Should be called whenever the activity is about to be destroyed.
	 * 
	 * @param context The context that is about to be destroyed.
	 */
	public static void onDestroy(Context context)
	{
		bluetoothDisabled(); //Clean up any existing connections
		context.unregisterReceiver(deviceListener);
	}
}
