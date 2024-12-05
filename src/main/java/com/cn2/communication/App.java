/* TODO: 
 * 1. make voice sending a separate thread -- done
 * 2. make clicking the call button again terminate the call -- done
 * 3. dump captured packets to a file, test what is being captured -- done
 * 4. send packets over udp, packet length 1024 -- well, it's sending something alright! 
 * 		a. still need to flush the BAOS probably!
 */
package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

import javax.sound.sampled.*;
import javax.sound.sampled.spi.AudioFileWriter;
import javax.sound.sampled.AudioFileFormat;


public class App extends Frame implements WindowListener, ActionListener, Runnable {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;				
	
	// TODO: Please define and initialize your variables here...

	static int voip_dest_port = 26565;
	static int voip_src_port = 26567;
	String dest_addr = "192.168.1.15";
	
	/* ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧
	 * ▧ ▧ vars for voip send ▧ ▧
	 * ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ */
	AudioFormat sample_format; /* encoding technique, channels, bps, byte order, signed */
	boolean isCalling = false;
	Thread voipCaptureThread = null;
	TargetDataLine captureLine = null;
	int packet_length = 1024;
	ByteArrayOutputStream out = null;
	
	/* keeps eclipse from complaining */
	public void run() {}
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");			
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("CN2 - AUTH");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
		
		do{		
			// TODO: Your code goes here...
			
		}while(true);
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
	

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			System.out.println("I'm sending!");
			
			
		}else if(e.getSource() == callButton){
			
			// The "Call" button was clicked
			if(isCalling == false) {
				/* audio capture new thread */
				System.out.println("Starting audio capture thread...");
				isCalling = true;
				voipCaptureThread = new Thread( () -> handleVoIP());
				voipCaptureThread.start();
			}
			else if (isCalling == true) {
				/* close line, drain audio buffer and send remaining bytes */
				isCalling = false;
				System.out.println("Audio thread should be over now");
				/*
				for(int i = 0; i < out.toByteArray().length; i++) {
					System.out.println(out.toByteArray()[i]);
				}*/
			}
		}
			

	}

	void handleVoIP() {
		/* open sockets first, then start capturing audio */
		InetAddress dest;
		DatagramSocket voice_send_socket;
		DatagramPacket voice_send = null;
		int packet_number = 1;	/* we are actually not numbering packets, this is to help iterate through the BAOS
		
		/* create a udp socket	*/ 
		
		/* Initialize destination address */
		try {
			dest = InetAddress.getByName(dest_addr);
		} catch (UnknownHostException e1) {
			System.out.println("Cannot get localhost address, quitting...");
			return;
		}
		System.out.println("Sending to: " + dest);
		try {
			voice_send_socket = new DatagramSocket(voip_src_port);
		} catch (SocketException e1) {
			System.out.println("Cannot open call socket, quitting...");
			return;
		}

		/* start audio capture */
		int buffer_size = 1024;
		/* endianness should not matter here since we're using PCM, but we're using big endian */
		AudioFormat format = new AudioFormat(8000, 8, 1, true, true);	
		
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
		if (AudioSystem.isLineSupported(info) == false) {
			System.out.println("Line is not supported! Quitting...");
			return;
		}
		/* Obtain and open the line */
		try {
			captureLine = (TargetDataLine) AudioSystem.getLine(info);
			captureLine.open(format, buffer_size);
		} catch (LineUnavailableException ex) {
			System.out.println("Line unavailable, quitting...");
			return;
		}
		
		/* set audio buffer and BAOS to handle our bytes later */
		out  = new ByteArrayOutputStream();
		int numBytesRead;		
		byte[] data = new byte[buffer_size / 5];

		captureLine.start();
		
		byte[] audio_data = null;
		int audio_data_length = 0;
		/* stop when we press call button again */
		int i = 0;	
		while (isCalling == true) {
		   /* Read the next chunk of data from the TargetDataLine to audio buffer */
		   numBytesRead =  captureLine.read(data, 0, data.length);
		   /* Save this chunk of data to BAOS */
		   out.write(data, 0, numBytesRead);
		   System.out.println("out: " + out.toByteArray()[i]);
		   
		   /* send udp datagram */
		    audio_data = out.toByteArray();
			audio_data_length = audio_data.length;
			
			if(audio_data_length >= (packet_number) * packet_length) {
				voice_send = new DatagramPacket(audio_data, (packet_number - 1) * packet_length, packet_length, dest, voip_dest_port);
//				text_sender = new DatagramPacket(payload, i * packet_length, payload.length - i * packet_length, dest, text_dest_port);
//				text_sender = new DatagramPacket(payload, i * packet_length, packet_length, dest, text_dest_port);
				System.out.println("Sending packet " + (packet_number - 1) + " (" + packet_length + " bytes)");
				System.out.println("Audio data total length: " + audio_data_length);
				packet_number = packet_number + 1;
				/* send the datagram through the socket */
				try {
					voice_send_socket.send(voice_send);
				} catch (IOException e1) {
					System.out.println("Cannot send voice datagram through socket for whatever reason");
					return;
				}
						
			}
			i++;
//		   sendVoicePackets(packet_number, voice_send, voice_send_socket, dest);
//		   if(i % 4 == 0) {
//		   		System.out.println("out size: " + out.toByteArray().length);
//		   }
		}
		voice_send_socket.close();
		captureLine.close();
		
		/* debug what is being captured */
		File file = new File("/home/pacopacorius/test.wav");
		try {
			file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error creating file");
		}
		ByteArrayInputStream audio_input = new ByteArrayInputStream(audio_data);
		AudioInputStream total_collected_audio = new AudioInputStream(audio_input, format, audio_data_length);			
		int bytes_written_to_file; 
		try {
			bytes_written_to_file = AudioSystem.write(total_collected_audio, AudioFileFormat.Type.WAVE, file);
		} catch (IOException e) {
			System.out.println("Couldn't write to file");
			return;
		}
		try {
			total_collected_audio.close();
		} catch (IOException e) {
			System.out.println("error in closing audioinputstream");
		}
		System.out.println("Written " + bytes_written_to_file + " bytes to file ~/test.wav");
		
	}
	
	void sendVoicePackets(int packet_number, DatagramPacket voice_send, DatagramSocket voice_send_socket, InetAddress dest) {
		
	}
	
	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
	
	
}
