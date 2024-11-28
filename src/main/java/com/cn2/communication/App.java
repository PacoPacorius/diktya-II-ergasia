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
		// listen for new messages here, call it from the main do ... while maybe?
	}
	

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
			
			// The "Send" button was clicked
			
			// TODO: Your code goes here...
			
			/* erase text in input field and show it in text area */
			String text = App.inputTextField.getText();
			App.inputTextField.setText("");
			App.textArea.append("Me: " + text);
			
			/* Initialize udp datagram packet with text string */
			InetAddress local;
			try {
				local = InetAddress.getByName("127.0.0.1");
			} catch (UnknownHostException e1) {
				System.out.println("Cannot get localhost address, quitting...");
				return;
			}
			InetAddress desk;
			try {
				desk = InetAddress.getByName("192.168.1.15");
			} catch (UnknownHostException e1) {
				System.out.println("Cannot get localhost address, quitting...");
				return;
			}
			System.out.println("Sending to: " + desk);
			
			/* ready the udp datagram */ 
			byte[] payload = text.getBytes();
			
			DatagramPacket test_send = new DatagramPacket(payload, payload.length, desk, 26557);
			
			/* create a udp socket */
			DatagramSocket socket;
			try {
				socket = new DatagramSocket(26555);
			} catch (SocketException e1) {
				System.out.println("Cannot open socket, quitting...");
				return;
			}
			
			/* send the datagram through the socket */
			try {
				socket.send(test_send);
			} catch (IOException e1) {
				System.out.println("Cannot send datagram through socket for whatever reason");
				return;
			}
//			socket.close();
		}else if(e.getSource() == callButton){
			
			// The "Call" button was clicked
			
			// TODO: Your code goes here...
			
			
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
