package robotics.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WifiWidget extends RelativeLayout implements OnClickListener
{
	private static WifiEvent event = new WifiEvent();
	private static ArrayList<WifiWidget.CommThread> threads = new ArrayList<WifiWidget.CommThread>();
	private String dstName = null;
	private int dstPort = 0;
	
	private Button wifiConnect;
	private EditText portNum;
	private EditText portName;
	private TextView connStatus;
	
	private static TextView lastTransmitted;
	
	private static final String CONNECT = "Connect";
	private static final String DISCONNECT = "Disconnect";
	private static final String CANCEL = "Cancel";
	
	private static class WifiEvent
	{
		public ArrayList<WifiEventNode> objects = new ArrayList<WifiEventNode>();
		
		public class WifiEventNode
		{
			public WifiListener object;
			public byte notifier;
			public int msgSize;
			
			public WifiEventNode(WifiListener object, byte notifier, int msgSize)
			{
				this.object = object;
				this.notifier = notifier;
				this.msgSize = msgSize;
			}
		}
		
		public void addListener(WifiListener object, byte notifier, int msgSize)
		{
			objects.add(new WifiEventNode(object, notifier, msgSize));
		}
		
		public void removeListener(WifiListener object)
		{
			for (int n = 0; n < objects.size(); n++)
			{
				if (objects.get(n).object.equals(object))
				{
					objects.remove(n);
					n--;
				}
			}
		}
		
		public void fireMessageReceived(byte id, byte[] args)
		{
			for (int n = 0; n < objects.size(); n++)
			{
				if (objects.get(n).notifier == id)
				{
					objects.get(n).object.messageReceived(args);
				}
			}
		}
	}
		
	private class CommThread extends Thread
	{
		// Status constants to be reported back through a Handler object
		public static final int CONN_STATUS = 0;
			public static final int CONN_START = 0;
			public static final int CONN_FINISH = 1;
			public static final int CONN_FAILURE = 0;
			public static final int CONN_SUCCESS = 1;
		public static final int CONN_MESSAGE = 0x2a;
		
		// Potential constants received from remote device
		/*
		public static final byte SONAR_FRONT = 'F';
		public static final byte SONAR_LEFT = 'L';
		public static final byte SONAR_MIDDLE = 'M';
		public static final byte SONAR_RIGHT = 'R';
		*/
		
		private String dstName;
		private int dstPort;
		private Socket socket;
		private Handler handler;
		private InputStream inp;
		private OutputStream out;
		
		private boolean cont = true;
		
		public CommThread(String dstName, int dstPort, Handler handler)
		{
			this.dstName = dstName;
			this.dstPort = dstPort;
			this.handler = handler;
			
			Log.v("WifiWidget.CommThread", "dstName = " + dstName + ": dstPort = " + dstPort);
		}
		
		public String getDstName()
		{
			return dstName;
		}
		
		public int getDstPort()
		{
			return dstPort;
		}
		
		public void run()
		{
			try
			{
				handler.obtainMessage(CONN_STATUS, CONN_START, 0).sendToTarget();
				socket = new Socket(dstName, dstPort);
				inp = socket.getInputStream();
				out = socket.getOutputStream();
				handler.obtainMessage(CONN_STATUS, CONN_FINISH, CONN_SUCCESS).sendToTarget();
				Log.v("Wifi", "Successfully connected to " + dstName + " on port " + dstPort);
				
				/*
				//Infinitely loop until cont is set to false from outside the thread
				byte[] message = new byte[3];
				while (cont)
				{
					//TODO: Are the return value of -1 and IOException cases from 
					//  InputStream.read(byte[]) enough to check if this thread should end?
					if (-1 == inp.read(message))
						break;
					
					if (message[0])
				}
				*/
				
				handler.obtainMessage(CONN_STATUS, CONN_FINISH, CONN_SUCCESS).sendToTarget();
				Log.v("Wifi", "Successfully disconnected from " + dstName + " on port " + dstPort);
			}
			catch (java.net.UnknownHostException uhe)
			{
				socket = null;
				handler.obtainMessage(CONN_STATUS, CONN_FINISH, CONN_FAILURE).sendToTarget();
				Log.e("Wifi", "Could not find " + dstName + " on " + dstPort);
				Log.e("Wifi", uhe.getMessage());
			}
			catch (IOException ioe)
			{
				socket = null;
				handler.obtainMessage(CONN_STATUS, CONN_FINISH, CONN_FAILURE).sendToTarget();
				Log.e("Wifi", "IOException occured when opening " + dstName + " on " + dstPort);
				Log.e("Wifi", ioe.getMessage());
			}
			
			//Continuously read and parse sonar data
			//TODO: Optimize this code for when only two integers are needed (decide when to use
			//  the simpler handler.obtainMessage(int, int, int) form instead)
			int type;
			try
			{
				while (-1 != (type = inp.read()))
				{
					for (WifiEvent.WifiEventNode object : event.objects)
					{
						if (object.notifier == (byte)type)
						{
							byte[] message = new byte[object.msgSize];
							if (-1 == inp.read(message))
								break;
							
							//Package the message up into a nice little bundle and send it to the
							//  main thread.
							android.os.Bundle bundle = new android.os.Bundle();
							bundle.putByte("Type", (byte)type);
							bundle.putByteArray("Data", message);
							android.os.Message msg = handler.obtainMessage(CONN_MESSAGE, 0, 0);
							msg.setData(bundle);
							msg.sendToTarget();
							Log.v("CommThread", "Received a message");
						}
					}
					/*
					if (message[0] == SONAR_FRONT)
					{
						if (-1 == inp.read(message, 1, 1))
							break;
						
						if (message[1] == 'L')
						{
							if (-1 == inp.read(message, 2, 1))
								break;
							Log.v("Wifi", "Requesting front-left sonar value to be " + message[2]);
						}
						else if (message[1] == 'M')
						{
							if (-1 == inp.read(message, 2, 1))
								break;
							Log.v("Wifi", "Requesting front-middle sonar value to be " + message[2]);
						}
						else if (message[1] == 'R')
						{
							if (-1 == inp.read(message, 2, 1))
								break;
							Log.v("Wifi", "Requesting front-right sonar value to be " + message[2]);
						}
						
					}
					*/
				}
			}
			catch (IOException ioe)
			{
				Log.v("Wifi", "Disconnected");
				handler.obtainMessage(CONN_STATUS, CONN_FINISH, CONN_SUCCESS).sendToTarget();
			}
			
			//while (true)
			//	; // Do nothing
			
			/*
	        StringBuffer sb = new StringBuffer();
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int bytes; // bytes returned from read()
	        String message;
	        int idx;
	        HashMap<String, String> hm;
	        String[] chunks;
	        
	        while (true) 
	        {
	            try 
	            {
	                // Read from the InputStream
	                bytes = inp.read(buffer);
	                sb.append(new String(buffer, 0, bytes));
	                while ((idx = sb.indexOf("\r\n\r\n")) > -1) 
	                {
	                    message = sb.substring(0, idx);
	                    sb.replace(0, idx+4, "");
	                    hm = new HashMap<String, String>();
	                    for (String line : message.split("\n")) 
	                    {
	                    	chunks = line.trim().split("=", 2);
	                    	if (chunks.length != 2) 
	                    		continue;
	                    	hm.put(chunks[0], chunks[1]);
	                    }
	                    handler.obtainMessage(CONN_MESSAGE, hm).sendToTarget();
	                }
	            } 
	            catch (IOException e) 
	            {
	            	Log.e("CommThread", "Connection broken! " + e.getMessage());
	                break;
	            }
	        }
	        */
		}
		
		/**
		 * Writes an array of bytes to a remote device.
		 * 
		 * @param bytes The array of bytes to write to the output stream.
		 * @throws IllegalStateException If the socket has not yet connected.
		 */
		public void write(byte[] bytes) throws IllegalStateException
		{
			if (socket == null)
			{
				throw new IllegalStateException("Socket not yet connected!");
			}
			
			if (out != null)
			{
				try
				{
					out.write(bytes);
				}
				catch (IOException ioe)
				{
					Log.e("Wifi", "IOException occured while writing");
				}
			}
		}
		
		/**
		 * Shuts down the socket. After disconnecting, this thread can no longer be used (a new
		 *   instance must be created)
		 */
		public void disconnect()
		{
			try
			{
				Log.v("Wifi", "Shutting down sockets...");
				/* (These apparently do nothing according to the documentation...)
				if (inp != null)
					inp.close();
				if (out != null)
					out.close();
				*/
				if (socket != null)
					socket.close();
				cont = false;
				Log.v("Wifi", "Shut down successful!");
			}
			catch (IOException ioe)
			{
				cont = false;
				Log.e("Wifi", "Error shutting down socket! " + ioe.getMessage());
			}
		}
	}
	
	public WifiWidget(Context context, AttributeSet attr)
	{
		super(context, attr);
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.wifi_widget, this);
		//setBackgroundResource(R.drawable.fragment_background2);
		
		wifiConnect = (Button)findViewById(R.id.wifi_connect);
		wifiConnect.setOnClickListener(this);
		portName = (EditText)findViewById(R.id.wifi_portName);
		portNum = (EditText)findViewById(R.id.wifi_portNum);
		connStatus = (TextView)findViewById(R.id.wifi_connStatus);
		
		lastTransmitted = (TextView)getRootView().findViewById(R.id.last_transmit);
	}
	
	private void connecting()
	{
		connStatus.setText("Connecting...");
		
		wifiConnect.setText(CANCEL);
		connStatus.setVisibility(VISIBLE);
		portName.setVisibility(GONE);
		portNum.setVisibility(GONE);
	}
	
	private void connected()
	{
		connStatus.setText("Connected to " + dstName + " on " + dstPort);
		Toast.makeText(getContext(), "Connected to a device!", Toast.LENGTH_SHORT);
		Output.writeMessage("Connected to " + dstName + " on " + dstPort);
		wifiConnect.setText(DISCONNECT);
	}
	
	private void disconnected()
	{
		wifiConnect.setText(CONNECT);
		connStatus.setVisibility(GONE);
		portName.setVisibility(VISIBLE);
		portNum.setVisibility(VISIBLE);
	}
	
	/**
	 * Returns the IP Address this widget is communicating with
	 * 
	 * @return The IP Address this widget is communicating with
	 */
	public String getConnectedName()
	{
		return dstName;
	}
	
	/**
	 * Returns the port this widget is communicating on.
	 * 
	 * @return The port this widget is communicating on.
	 */
	public int getConnectedPort()
	{
		return dstPort;
	}
	
	/**
	 * Establishes a new communications thread with a remote device.
	 * 
	 * @param dstName The host name of the remote device to connect to.
	 * @param dstPort The port on the host device to connect to.
	 */
	public void connect(String dstName, int dstPort)
	{
		Handler handler = new Handler()
		{
			public void handleMessage(android.os.Message message)
			{
				//TODO: handle any messages sent back from the new communications thread
				//Log.v("WifiWidget", "Callback handler received a message");
				if (message.what == WifiWidget.CommThread.CONN_STATUS)
				{
					if (message.arg1 == WifiWidget.CommThread.CONN_START)
					{
						connecting();
					}
					else if (message.arg1 == WifiWidget.CommThread.CONN_FINISH)
					{
						if (message.arg2 == WifiWidget.CommThread.CONN_SUCCESS)
						{
							connected();
						}
						else if (message.arg2 == WifiWidget.CommThread.CONN_FAILURE)
						{
							disconnected();
							Toast.makeText(getContext(), "Could not connect to requested device!", 
									Toast.LENGTH_SHORT);
						}
					}
				}
				else if (message.what == WifiWidget.CommThread.CONN_MESSAGE)
				{
					//Decode the Bundle into a byte and byte[] and fire off an event.
					android.os.Bundle bundle = message.getData();
					bundle.getByteArray("Data");
					event.fireMessageReceived((byte)bundle.getInt("Type"), 
							bundle.getByteArray("Data"));
				}
			}
		};
		
		connecting();
		this.dstName = dstName;
		this.dstPort = dstPort;
		CommThread thread = new CommThread(dstName, dstPort, handler);
		threads.add(thread);
		thread.start();
	}
	
	/**
	 * Writes a message to all remote devices currently connected to this program. If the 
	 *   application only needs to connect to one device, then there's no need for the
	 *   write(String, int, byte[]) method, this one will work just fine.
	 * 
	 * @param bytes The message to send to every remote device currently connected.
	 */
	public static void write(byte[] bytes)
	{
		if (bytes.length > 0)
		{
			for (CommThread thread : threads)
			{
				try
				{
					thread.write(bytes);
					
					String msg = "Last message transmitted ";
					int b;
					for (b = 0; b < bytes.length - 1; b++)
						msg += b + ":";
					msg += b;
					Log.v("Wifi", msg);
					
					if (lastTransmitted != null)
						lastTransmitted.setText(msg);
				}
				catch (IllegalStateException ise)
				{
					Log.e("Wifi", "Port " + thread.getDstPort() + " on \"" + thread.getDstName() + 
							"\" is not yet enabled!");
				}
			}
		}
	}
	
	/**
	 * Writes a message to a specific remote device currently connected to this program. If the
	 *   application only needs to connect to one remote device, then write(byte[]) is preferred
	 *   over this method.
	 * 
	 * @param dstName The host name of the device that will receive the message.
	 * @param dstPort The port on the host device that will receive the message.
	 * @param bytes The message to send to the remote device.
	 */
	public static void write(String dstName, int dstPort, byte[] bytes)
	{
		for (CommThread thread : threads)
		{
			if (thread.getDstPort() == dstPort && thread.getDstName().equals(dstName))
			{
				try
				{
					thread.write(bytes);
				}
				catch (IllegalStateException ise)
				{
					Log.e("Wifi", "Port " + thread.getDstPort() + " on \"" + thread.getDstName() + 
							"\" is not yet enabled!");
				}
			}
		}
	}
	
	/**
	 * Disconnects from the remote device.
	 */
	public void disconnect()
	{
		Log.v("Wifi", "Attempting a disconnect...");
		if (dstName != null && dstPort > 0)
		{
			for (int n = 0; n < threads.size(); n++)
			{
				CommThread thread = threads.get(n);
				if (thread.getDstPort() == dstPort && thread.getDstName().equals(dstName))
				{
					Log.v("Wifi", "Starting disconnect of thread " + n);
					thread.disconnect();
					Log.v("Wifi", "Disconnect successful");
					threads.remove(n);
					
					dstPort = 0;
					dstName = null;
					disconnected();
					return;
				}
			}
		}
	}
	
	/**
	 * Returns true if any thread is active
	 * 
	 * @return True if any thread is active, false otherwise
	 */
	public static boolean isActive()
	{
		return threads.size() > 0;
	}
	
	/**
	 * Adds specified WifiListener object to the list of objects to be notified when a relevant
	 *   message is received from a remote device. When a byte appears that matches the byte passed
	 *   in by this method, this class will begin constructing a message of size "size" and pass it
	 *   to the listening object via messageReceived().
	 *   
	 * @param object The object to notify when a relevant message appears.
	 * @param notifier A byte that tells this class to begin constructing the message.
	 * @param size The size of the message to expect.
	 */
	public static void addListener(WifiListener object, byte notifier, int size)
	{
		event.addListener(object, notifier, size);
	}
	
	/**
	 * Removes specified WifiListener object from the list of objects to be notified when a relevant
	 *   message is received from a remote device.
	 *   
	 * @param object The object to remove from the list of objects to notify when a relevant message
	 *   appears.
	 */
	public static void removeListener(WifiListener object)
	{
		event.removeListener(object);
	}

	@Override
	public void onClick(View v)
	{
		if (wifiConnect.getText().equals(CONNECT))
		{
			//connecting();
			connect(portName.getText().toString(), new Integer(portNum.getText().toString()));
		}
		else
		{
			Output.writeMessage("Disconnecting from " + dstName + " on port " + dstPort);
			disconnect();
		}
		
		/*
		if (wifiConnect.getText().equals(CONNECT))
		{
			wifiConnect.setText(DISCONNECT);
			connStatus.setVisibility(VISIBLE);
			portName.setVisibility(GONE);
			portNum.setVisibility(GONE);
		}
		else
		{
			wifiConnect.setText(CONNECT);
			connStatus.setVisibility(GONE);
			portName.setVisibility(VISIBLE);
			portNum.setVisibility(VISIBLE);
		}
		*/
	}
}
