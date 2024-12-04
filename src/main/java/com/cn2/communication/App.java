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
	/* ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧
	 * ▧ ▧ vars for voip send ▧ ▧
	 * ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ */
	AudioFormat sample_format; /* encoding technique, channels, bps, byte order, signed */
	boolean stopped = false;
	
	
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
						
			
			
		}else if(e.getSource() == callButton){
			
			// The "Call" button was clicked
			
			// TODO: Your code goes here...
			/* TargetDataLine <- Mixer <- SourceDataLine <- Microphone*/
			int buffer_size = 1024;
			/* endianness should not matter here since we're using PCM, but we're using big endian */
			AudioFormat format = new AudioFormat(8000, 8, 1, true, true);	
			
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
			if (AudioSystem.isLineSupported(info) == false) {
				System.out.println("Line is not supported! Quitting...");
				return;
			}
			// Obtain and open the line.
			TargetDataLine line = null;
			try {
    			line = (TargetDataLine) AudioSystem.getLine(info);
    			line.open(format, buffer_size);
			} catch (LineUnavailableException ex) {
				System.out.println("Line unavailable, quitting...");
				return;
			}
			
			// Assume that the TargetDataLine, line, has already
			// been obtained and opened.
			ByteArrayOutputStream out  = new ByteArrayOutputStream();
			int numBytesRead;		
			byte[] data = new byte[buffer_size / 5];

			line.start();
			
			// Here, stopped is a global boolean set by another thread.
			int i = 0;
			while (stopped != true) {
			   // Read the next chunk of data from the TargetDataLine.
			   numBytesRead =  line.read(data, 0, data.length);
			   // Save this chunk of data.
			   out.write(data, 0, numBytesRead);
			   System.out.println("out: " + out.toByteArray()[i]);
			   i++;
			}     
			
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
