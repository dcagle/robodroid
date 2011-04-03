package robotics.android;

public interface WifiListener
{
	public void messageReceived(byte[] args);
	public void wifiConnected();
	public void wifiDisconnected();
}
