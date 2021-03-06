/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.apache.log4j.Logger;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import protocole.UDPPacket;
import utils.Marshallizer;
/**
 *
 * @author JUASP-G73-Android
 */
public class receptionHandler implements Runnable{
     private UDPPacket connectionPacket;
    private DatagramSocket connectionSocket = null;
    private DatagramPacket packetReceive;
    private File theFile = new File("hd.jpg"); // Static, nous allons toujours utlisé le même fichier pour la transmission
    
    private static final Logger logger = Logger.getLogger(receptionHandler.class);
    
    /*************************************************************/
    /********************   CONSTRUCTOR   ************************/
    /*************************************************************/
    
    public receptionHandler(DatagramSocket connectionSocket) {
        logger.info("receptionHandler: (client) new runnable");
        this.connectionSocket = connectionSocket;
    }

    /*************************************************************/
    /*****************   GETTER AND SETTER   *********************/
    /*************************************************************/
    
    public DatagramSocket getConnectionSocket() {
        return connectionSocket;
    }

    public void setConnectionSocket(DatagramSocket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    public DatagramPacket getPacketReceive() {
        return packetReceive;
    }

    public void setPacketReceive(DatagramPacket packetReceive) {
        this.packetReceive = packetReceive;
    }



    public File getTheFile() {
        return theFile;
    }

    public void setTheFile(File theFile) {
        this.theFile = theFile;
    }

    
    /*************************************************************/
    /*****************        METHODS        *********************/
    /*************************************************************/
          
    private void sendPacket(UDPPacket udpPacket) {
        try {
               logger.info("receptionHandler: (client) sendPacket");
               logger.info("receptionHandler: (client) envoi du paquet suivant:" + udpPacket.toString());               
                byte[] packetData = Marshallizer.marshallize(udpPacket);
                DatagramPacket datagram = new DatagramPacket(packetData,
                                packetData.length, 
                                udpPacket.getDestination(),
                                udpPacket.getDestinationPort());
                connectionSocket.send(datagram); // émission non-bloquante
        } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
        }
    }
    
     private UDPPacket buildPacket(int seq, int ack, int fin, byte[] data) {
                logger.info("receptionHandler: (client) buildPacket");
                UDPPacket packet = new UDPPacket(connectionPacket.getType(),connectionSocket.getInetAddress(),connectionSocket.getPort(),connectionPacket.getSourceAdr(),connectionPacket.getSourcePort());
                packet.setData(data);
                packet.setSeq(seq);
                packet.setAck(ack);
                packet.setFin(fin);
                logger.debug(packet.toString());
                return packet;
     }
    
     public UDPPacket getConnectionPacket() {
        return connectionPacket;
    }

    public void setConnectionPacket(UDPPacket udpPacket) {
        this.connectionPacket = udpPacket;
    }
    
    public void start() {		
        try {
		//on set le pckt a recevoir
		byte[] buffer = new byte[1500];
		DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                logger.info("receptionHandler: (client) en attente d'un datagram pour le hankshake");
		//reception bloquante du paquet seq=1
		connectionSocket.receive(datagram);
                logger.info("receptionHandler: (client) datagram reçu");
		connectionPacket = (UDPPacket)Marshallizer.unmarshall(datagram);
                
                //insersion des informations sources du datagram
                connectionPacket.setSourceAdr(datagram.getAddress());
                connectionPacket.setSourcePort(datagram.getPort());
                
                logger.info("receptionHandler: (client) datagram reçu:" + connectionPacket.toString());

		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU SEQ=1 ACK=1
		UDPPacket confirmConnectionPacket = buildPacket(1,1,0,new byte[1024]);
		timer.scheduleAtFixedRate(new TimerTask() 
		{
			public void run() 
			{
                            logger.info("receptionHandler: (client) sending a handshake confirmation.");
                            sendPacket(confirmConnectionPacket);
			}
		}, 0, 10000);

		//PREMIERE RECEPTION DE DATA
		int seqAttendu = 1;
		int ackRetour=0;

                BufferedOutputStream bos;             
                bos = new BufferedOutputStream(new FileOutputStream("serverToClientDownload.jpg",true)); 
                //bos = new BufferedOutputStream(new FileOutputStream("serverToClientDownload.txt",true));               
		do
		{
                        boolean nonVide = false;
                        
                        logger.info("receptionHandler: (client) en attente de datagram avec du data");
			connectionSocket.receive(datagram);
			logger.info("receptionHandler: (client) datagram-data reçu");
                        
			//CREATION PAQUET A RECEVOIR ET ACK A RENVOYER
			UDPPacket UDPReceive = (UDPPacket) Marshallizer.unmarshall(datagram);
                        logger.info("receptionHandler: (client) datagram-data reçu:" + UDPReceive.toString());
			UDPPacket receveACK = buildPacket(seqAttendu, ackRetour,0,new byte[1024] );
                        logger.info("receptionHandler: (client) ack qu'on va envoyer:" + receveACK.toString());
			
			if(UDPReceive.getSeq()==1){
                           for (byte b : UDPReceive.getData()) {
                               if (b != 0) {
                                   nonVide = true;
                                   break;
                               }
                           }
                           timer.cancel();
                        }
                        else nonVide = true;

			
                       
                        //SI SEQ RECUE =SEQ ATTENDUE
			if (UDPReceive.getSeq()==seqAttendu && nonVide)
			{
                                logger.info("receptionHandler: (client) datagram contient la séquence attendu");
				//ON ECRIT LES DONNES RECUES DANS LE FICHIER
                                
                                //DEBUGGING TOOL
                                //String doc=new String(UDPReceive.getData(), "UTF-8");
                                //logger.info("recu et ecrit:" + doc.toString());
                                
				bos.write(UDPReceive.getData());                                
                                bos.flush();
                                logger.info("receptionHandler: (client) on write la séquence de byte pour seq=:" + UDPReceive.getSeq());
				//ACK CONFIRME RECEPTION DU PAQUET ATTENDU
				ackRetour = seqAttendu;
				if(UDPReceive.getFin() == 0) seqAttendu +=UDPReceive.getData().length;
			}
                        sendPacket(receveACK);
			if(UDPReceive.getFin() == 1 && UDPReceive.getSeq() == seqAttendu )
			{
				logger.info("receptionHandler: (client) fermeture de la connexion");
                                closeConnection(seqAttendu,ackRetour) ;
				bos.close();
			}
                        

		}while(true);
                
        } catch (SocketException e) {
                System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
        }
        finally {
                logger.info("end of transmission");
                stop();
        }
	}
    public void closeConnection(int seqNum, int ackNum) throws IOException
	{
		Timer timer = new Timer(); //Timer pour les timeouts
		//ENVOI DU ACK DE FIN
		UDPPacket endPqt = buildPacket(seqNum, ackNum, 1, new byte[1024]);
		timer.scheduleAtFixedRate(new TimerTask() 
		{
			public void run() 
			{
				sendPacket(endPqt);
			}
		}, 0, 1000);
                Timer fermerTimer = new Timer();
                fermerTimer.schedule(new TimerTask() 
		{
			public void run() 
			{
				timer.cancel();
                                connectionSocket.close();
                                stop();
			}
		}, 10000);		
	}
    public void stop(){
        Thread.currentThread().interrupt();
    }
    @Override
	public void run() {
		start();	
	}
}
