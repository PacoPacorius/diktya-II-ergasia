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
	public void run() {
		
	}
	String input_text;	
	InetAddress dest;
	DatagramPacket text_sender;
	static DatagramSocket text_sender_socket;
	Thread voipThread = null;
	
	static int text_dest_port = 26557;
	static int text_src_port = 26555;		// these two ports probably don't have to be separate, the OS should handle port traffic
											// pantws emena den anoigei tautoxrona se duo kanei quitting
	static int voip_dest_port = 26565;
	static int voip_src_port = 26567;
	static String dest_addr = "127.0.0.1";
	static int packet_length = 1024; // replace all buffers with this in the end
	static boolean isCalling = false;
	
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
	public static void main(String[] args) {
	    // 1. Create the app's window
	    App app = new App("CN2 - AUTH - TEAM AE");
	    app.setSize(500, 250);
	    app.setVisible(true);

	    // 2. Start text thread - always running
	    byte[] buffer = new byte[1024];
	    
	    Thread textThread = new Thread(() -> handleText());
	    textThread.start();	    

	}
	
	// Text Handling Method
	private static void handleText() {
		try (DatagramSocket text_receiver_socket = new DatagramSocket(text_src_port) ) {
			System.out.println("Listening for messages on port " + text_src_port);
			
			// Continuously listen for incoming text messages
		    byte[] buffer = new byte[1024];

		    while (true) {
		        try {
		            // Handle text messages
		            DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
		            text_receiver_socket.receive(incomingPacket);

		            String receivedMessage = new String(incomingPacket.getData(), 0, incomingPacket.getLength());
		            App.textArea.append("Friend: " + receivedMessage + newline);


		        } catch (IOException e) {
		            System.out.println("Error receiving message: " + e.getMessage());
		        }
		    }
		    
			
		} catch (Exception e) {
	        System.out.println("Error in text handling: " + e.getMessage());
	    }
		
		
	}
	
	// VoIP Handling Method
	private static void handleVoIP() {
		System.out.println("inside the handle function");
	    try (DatagramSocket voipSocket = new DatagramSocket(voip_src_port)) {
	        InetAddress dest = InetAddress.getByName(dest_addr);

	        byte[] sendBuffer = new byte[1024];
	        byte[] receiveBuffer = new byte[1024];
	        DatagramPacket sendPacket;
	        DatagramPacket receivePacket;

	        while (isCalling) {
	            // Sending audio (example: mock audio data)
	            sendBuffer = getMockAudioData();
	            sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, dest, voip_dest_port);
	            voipSocket.send(sendPacket);

	            // Receiving audio
	            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
	            voipSocket.receive(receivePacket);
	            processReceivedAudio(receivePacket.getData());
	        }
	    } catch (Exception e) {
	        System.out.println("Error in VoIP handling: " + e.getMessage());
	    }
	}

	// Mock Audio Data (Replace with real audio capture logic)
	private static byte[] getMockAudioData() {
	    return "mock audio data".getBytes(); // Replace with actual audio capture
	}

	// Process Received Audio (Replace with real audio playback logic)
	private static void processReceivedAudio(byte[] data) {
	    System.out.println("Received audio packet: " + new String(data)); // Replace with audio playback
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
			
			// The "Send" button was clicked
			String input_text;	
			InetAddress dest;
			DatagramPacket text_sender;
			byte[] payload;
			int multiplier; int modulo;
			
			
			/* get text from text area */
			if(App.inputTextField.getText().length() > 0) {
				input_text = App.inputTextField.getText();
				
				/* create a udp socket */
				try {
					text_sender_socket = new DatagramSocket(text_dest_port);
				} catch (SocketException e1) {
					System.out.println("Failed to open socket: " + e1.getMessage());
					return;
				}
				
				/* Initialize udp datagram packet */
				try {
					dest = InetAddress.getByName(dest_addr);
				} catch (UnknownHostException e1) {
					System.out.println("Cannot get localhost address, quitting...");
					return;
				}
				System.out.println("Sending to: " + dest);
				
				
				/* prepare to divide text string into packets of 1024 */ 
				payload = input_text.getBytes();
				multiplier = payload.length / 1024 + 1;
				modulo = payload.length % 1024;
				// System.out.println("Multiplier = " + (multiplier - 1) + ", modulo = " + modulo);
				for (int i = 0; i < multiplier; i++) {
					// System.out.println("i = " + i);
					
					/* load up the ith packet of 1024 bytes, or the final (multiplier - 1)th packet of modulo bytes. 
					 * if the ith packet is loaded, send 1024 bytes. if the final packet is loaded, send any remaining
					 * bytes (this is always <= 1024).
					 * */
					if(i == multiplier - 1) {
						text_sender = new DatagramPacket(payload, i * packet_length, payload.length - i * packet_length, dest, text_dest_port);
					} 
					else {
						text_sender = new DatagramPacket(payload, i * packet_length, packet_length, dest, text_dest_port);	
					}
					
					/* send the datagram through the socket */
					try {
						text_sender_socket.send(text_sender);
					} catch (IOException e1) {
						System.out.println("Cannot send datagram through socket for whatever reason");
						return;
					}
					
				}
				
				/* erase text in input field and show it in text area */
				App.inputTextField.setText("");
				App.textArea.append("Me: " + input_text + newline);
			
				/* close socket, prevent resource leak */
				text_sender_socket.close();
			}
			else {
				System.out.println("You need to type something genius!");
			}
			
			
			
		}else if(e.getSource() == callButton){
			// The "Call" button was clicked
			// this starts the receiving VoIP thread 
			if (!isCalling) {
				System.out.println("Starting VoIP thread...");
		        voipThread = new Thread(() -> handleVoIP());
		        voipThread.start();		
			}

			isCalling = !isCalling;
			
			
	        
			
		}
			

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
