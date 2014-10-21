package be.uantwerpen.systemY.client;

import java.net.DatagramPacket;
import java.rmi.RemoteException;
import java.util.Observable;
import java.util.Observer;

import be.uantwerpen.systemY.interfaces.BootstrapManagerInterface;
import be.uantwerpen.systemY.networkservices.MulticastObserver;
import be.uantwerpen.systemY.shared.Node;

public class DiscoveryManager
{
	private Client client;
	
	public DiscoveryManager(MulticastObserver observer, Client client)
	{
		this.client = client;
		
		observer.addObserver(new Observer()
		{
			public void update(Observable source, Object object)
			{
				multicastReceived((DatagramPacket)object);
			}
		});
	}
	
	private void multicastReceived(DatagramPacket datagram)
	{
		String message = new String(datagram.getData());
		
		if(message.trim().split(" ").length == 2)
		{
			String clientname = message.trim().split(" ", 2)[0];
			String ipAddress = message.trim().split(" ", 2)[1];
			
			if(!(clientname.equals(client.getHostname()) && ipAddress.equals(client.getIP())))							//Drop own discovery datagrampacket
			{
				Node oldNext = client.updateLinks(new Node(clientname, ipAddress));
				if(oldNext != null)																						//New node is the new nextNode
				{
					sendNetworkInfo(clientname, ipAddress, new Node(client.getHostname(), client.getIP()), oldNext);	//Send prevNode (this node) and nextNode (oldNext) to the new node
				}
			}
		}
	}
	
	private boolean sendNetworkInfo(String clientname, String ip, Node prevNode, Node nextNode)
	{
		String bindLocation = "//" + ip + "/Bootstrap_" + clientname;
		
		BootstrapManagerInterface bInterface = (BootstrapManagerInterface)client.getRMIInterface(bindLocation);
		
		if(bInterface != null)
		{
			try
			{
				bInterface.setLinkedNodes(prevNode, nextNode);
			}
			catch(RemoteException e)
			{
				System.err.println("RMI message to: " + ip + " failed!");
				this.client.nodeConnectionFailure(clientname);
				return false;
			}
			return true;
		}
		return false;
	}
}
