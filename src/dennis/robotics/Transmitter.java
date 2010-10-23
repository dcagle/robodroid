package dennis.robotics;

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.Button;
import android.widget.AdapterView.OnItemClickListener;

public class Transmitter
{
	private static BluetoothAdapter adapter;
	//private static TransmitterThread transmitter;
	//private static ConnectionThread connection;
	private static DeviceListener deviceListener;
	//private static DeviceDialog dialog;
	private static DeviceList devices;
	
	private static CommThread commThread;
	
	//Flags to indicate the status of the phone's current bluetooth situation
	private static boolean btActive = false;
	
	private static final int REQUEST_BLUETOOTH_EN = 0;
	//private static final UUID REMOTE_UUID = UUID.fromString("18e58230-af46-11df-94e2-0800200c9a66");
	private static final UUID REMOTE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	/*
	private static class TransmitterThread extends Thread
	{
		private BluetoothSocket socket;
		private OutputStream output;
		private String message;
		private boolean newData;
		
		public TransmitterThread(BluetoothSocket socket)
		{
			this.socket = socket;
			
			try
			{
				output = socket.getOutputStream();
			}
			catch (IOException ioe)
			{
				Log.e("Transmitter", "Error obtaining output stream!");
			}
		}
		
		public void sendMessage(String message)
		{
			this.message = message;
			newData = true;
			interrupt();
		}
		
		public synchronized void run()
		{
			while (true)
			{
				try
				{
					wait();
				}
				catch (InterruptedException ie) { }
				
				if (newData)
				{
					try
					{
						output.write(message.getBytes());
						Log.i("Transmitter", "  \"Sending\" a phony message.");
						newData = false;
					}
					catch (IOException ioe)
					{
						Log.e("Transmitter", "Error sending message!");
					}
				}
			}
		}
	}
	*/
	
	/**
	 * Creates a seperate thread to handle the expensive BluetoothSocket.connect() call.
	 * 
	 * Code from "http://developer.android.com/guide/topics/wireless/bluetooth.html"
	 */
	/*
	private static class ConnectionThread extends Thread
	{
		private BluetoothSocket socket;
		
		public ConnectionThread(BluetoothDevice device)
		{
			cancelDiscovery();
			
			try
			{
				socket = device.createRfcommSocketToServiceRecord(REMOTE_UUID);
				Output.writeMessage("Initialized connection");
			}
			catch (IOException ioe)
			{
				Log.e("Transmitter", "Connection Failed! -- Error while obtaining remote " +
						"BluetoothSocket");
			}
		}
		
		public void run()
		{
			if (socket != null)
			{
				cancelDiscovery();
				
				try
				{
					Output.writeMessage(socket.toString());
					socket.connect();
					Output.writeMessage("Succesfully connected to device!");
				}
				catch (IOException ioe)
				{
					try
					{
						Log.e("Transmitter", "Connection Failed! -- Error while connecting " +
								"BluetoothSocket");
						socket.close();
					}
					catch (IOException ioe2) { }
				}
				
				initializeTransmitter(socket);
			}
		}
		
		public void cancel()
		{
			try
			{
				socket.close();
			}
			catch (IOException ioe)
			{
				Output.writeMessage("Error occured while closing BluetoothSocket");
			}
		}
	}
	*/
	
	/**
	 * Creates a list that aids the user in selecting a device to connect to.
	 */
	public static class DeviceList extends ListView implements OnItemClickListener//, OnClickListener
	{
		private ArrayAdapter<String> items;
		private ArrayList<BluetoothDevice> devices;
		
		//private Button scanStart;
		//private ProgressBar scanStatus;
		
		public DeviceList(Context context, android.util.AttributeSet attr)
		{
			super(context, attr);
			//initialize(context);
			
			items = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice);
			setAdapter(items);
			setOnItemClickListener(this);
			devices = new ArrayList<BluetoothDevice>();
			
			//scanStart = (Button)findViewById(R.id.bt_scan);
			//scanStatus = (ProgressBar)findViewById(R.id.bt_progress);
			
			//TODO: Find out why this is null
			//if (scanStart != null)
			//	scanStart.setOnClickListener(this);
		}
		
		/*
		private void initialize(Context context)
		{
			items = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice);
			setAdapter(items);
			setOnItemClickListener(this);
			devices = new ArrayList<BluetoothDevice>();
			
			scanStart = (Button)findViewById(R.id.bt_scan);
			scanStatus = (ProgressBar)findViewById(R.id.bt_progress);
			scanStart.setOnClickListener(this);
		}
		*/
		
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
		
		/*
		public void startDiscovery()
		{
			scanStart.setEnabled(false);
			scanStatus.setVisibility(ProgressBar.VISIBLE);
			
			items.clear();
			devices.clear();
			beginDiscovery();
		}
		
		public void endDiscovery()
		{
			scanStart.setEnabled(true);
			scanStatus.setVisibility(ProgressBar.INVISIBLE);
		}
		*/

		/*
		@Override
		public void onClick(View view)
		{
			if (view.getId() == R.id.bt_scan)
			{
				startDiscovery();
			}
		}
		*/
		
		@Override
		public void onItemClick(AdapterView<?> av, View view, int pos, long id)
		{
			cancelDiscovery();
			Log.v("DeviceList", "Attempting to connect to " + devices.get(pos).getName());
			connectDevice(devices.get(pos));
		}
	}
	
	/**
	 * References the Bluetooth toggle button.
	 * 
	 * Requires the following xml elements:
	 *   - (Button) bt_toggle
	 */
	/*
	private static class BTToggleButton implements OnClickListener
	{
		private Button btToggle;
		private boolean enabled;
		
		public BTToggleButton(Activity parent)
		{
			btToggle = (Button)parent.findViewById(R.id.bt_toggle);
			enabled = false;
		}
		
		public boolean isActive()
		{
			return enabled;
		}
		
		@Override
		public void onClick(View view)
		{
			if (view.getId() == R.id.bt_toggle)
			{
				if (enabled)
				{
					enabled = false;
					btToggle.setText("Enable Bluetooth");
				}
				else
				{
					enabled = true;
					btToggle.setText("Disable Bluetooth");
				}
			}
		}
	}
	*/
	
	/*
	/**
	 * Creates a dialog that aids the user in selecting a device to connect to.
	 * TODO: Stop button from disappearing when too many devices are found.
	 */
	/*
	private static class DeviceDialog extends Dialog implements OnClickListener, OnItemClickListener
	{
		private ListView list;
		private ArrayAdapter<String> items;
		private ArrayList<BluetoothWidget> devices;
		
		public DeviceDialog(Context context)
		{
			super(context);
			requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
			setContentView(R.layout.device_dialog);
			
			list = (ListView)findViewById(R.id.foundDevices);
			items = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
			list.setAdapter(items);
			list.setOnItemClickListener(this);
			devices = new ArrayList<BluetoothWidget>();
			
			((Button)findViewById(R.id.startScan)).setOnClickListener(this);
		}
		
		public void addDevice(BluetoothWidget device)
		{
			items.add(device.getName() + " (" + device.getAddress() + ")");
			this.devices.add(device);
		}
		
		public void startDiscovery()
		{
			((Button)findViewById(R.id.startScan)).setEnabled(false);
			((ProgressBar)findViewById(R.id.scanning)).setVisibility(ProgressBar.VISIBLE);
			items.clear();
			devices.clear();
			beginDiscovery();
		}
		
		public void endDiscovery()
		{
			((Button)findViewById(R.id.startScan)).setEnabled(true);
			((ProgressBar)findViewById(R.id.scanning)).setVisibility(ProgressBar.INVISIBLE);
		}
		
		@Override
		public void show()
		{
			//TODO Populate the list with previously paired devices
			super.show();
		}
		
		@Override
		public void hide()
		{
			if (adapter.isDiscovering())
				adapter.cancelDiscovery();
			super.hide();
		}

		@Override
		public void onClick(View view)
		{
			if (view.getId() == R.id.startScan)
			{
				startDiscovery();
			}
		}
		
		@Override
		public void onItemClick(AdapterView<?> av, View view, int pos, long id)
		{
			endDiscovery();
			connectDevice(devices.get(pos));
			hide();
		}
	}
	*/
	
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
				//devices.endDiscovery();
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
			//dialog = new DeviceDialog(parent);
			//devices = new DeviceList(context);
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
			
			/*
			if (!adapter.isEnabled())
			{
				//Enable the bluetooth module if it hasn't been enabled already.
				Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				context.startActivityForResult(intent, REQUEST_BLUETOOTH_EN);
			}
			else
			{
				startService();
			}
			*/
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
	
	public static void sendMessage(String message)
	{
		if (commThread != null)
		{
			if (commThread.isAlive())
			{
				Log.i("Transmitter", "Attempting to send message...");
				commThread.write(message.getBytes());
			}
		}
	}
	
	public static void sendByte(byte[] code)
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
		Output.writeMessage("Connecting to " + device.getName() + "(" + device.getAddress() + ")");
		Log.i("Transmitter", "Connecting to " + device.getName() + "(" + device.getAddress() + ")");
		
		Handler handler = new Handler() 
		{
			public void handleMessage(Message message)
			{
				Output.writeMessage("Received: " + message);
			}
		};
		commThread = new CommThread(device, handler);
		commThread.start();
		
		/*
		connection = new ConnectionThread(device);
		connection.start();
		*/
	}
	
	/*
	private static void initializeTransmitter(BluetoothSocket socket)
	{
		transmitter = new Transmitter(socket);
		transmitter.start();
	}
	*/
	
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
	
	/*
	/**
	 * Should be called whenever an intent returns a value to the parent activity.
	 * 
	 * @param requestCode The integer code associated with the returning intent.
	 * @param resultCode The result reported by the returning intent.
	 * @param context The context of the parent activity.
	 */
	/*
	public static void onActivityResult(int requestCode, int resultCode, Context context)
	{
		if (requestCode == REQUEST_BLUETOOTH_EN)
		{
			if (resultCode != Activity.RESULT_OK)
			{
				//User is not allowing this program to turn bluetooth on
				Toast.makeText(context, "Bluetooth must be on!", Toast.LENGTH_LONG).show();
			}
		}
	}
	*/
}
