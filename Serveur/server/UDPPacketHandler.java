package server;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.log4j.Logger;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import protocole.UDPPacket;
import utils.Marshallizer;

//Extact data from datagram
//send respons
public class UDPPacketHandler implements Runnable {

	private DatagramSocket serverSocket;
        private DatagramPacket packetReceive;
	private static final Logger logger = Logger.getLogger(UDPPacketHandler.class);
	

	public UDPPacketHandler(DatagramPacket packetReceive, DatagramSocket serverSocket) {
		super();
		this.packetReceive = packetReceive;
		this.serverSocket = serverSocket;
		logger.info("UDPPacketHandler:new runnable");
	}
	
	@Override
	public void run() {
		//extract data from packet		
		UDPPacket udpPacket = (UDPPacket) Marshallizer.unmarshall(packetReceive);
		logger.info("UDPPacketHandler: udpPacket analysed " + String.valueOf(udpPacket.getType()));
                
                //insert the source data into the packet
                udpPacket.setSourceAdr(packetReceive.getAddress());
                udpPacket.setSourcePort(packetReceive.getPort());
                
		if (udpPacket.isForDownload()) {
			//Setup the transmissionThread
			Thread transmissionThread = new Thread(new transmissionHandler(udpPacket));
                        transmissionThread.start();
			logger.info("UDPPacketHandler: TransmissionHandler started");                        
		}
                else{
                    //Setup the receptionHandlerThread
			Thread receptionHandlerThread = new Thread(new receptionHandler(udpPacket));
                        receptionHandlerThread.start();
			logger.info("UDPPacketHandler: ReceptionHandler started");
                }
		
	}
	
		


}
