package com.cn2.communication;

import java.io.*;
import java.net.*;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
	
	// Cryptography variables
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5PADDING";
    private static final byte[] AES_KEY = "1234567890123456".getBytes(); // Example 16-byte key
    private static SecretKey secretKey;
    private static Cipher cipher;

    static {
        try {
            secretKey = new SecretKeySpec(AES_KEY, "AES");
            cipher = Cipher.getInstance(AES_ALGORITHM);
        } catch (Exception e) {
            System.err.println("Error initializing cryptography: " + e.getMessage());
        }
    }
    
	// Variable initialization
	
	// Ports and address
	static int text_dest_port = 26555;
	static int text_src_port = 26557;											
	static int voip_dest_port = 26567;
	static int voip_src_port = 26565;
	static String dest_addr = "192.168.1.15";
	// static String dest_addr = "127.0.0.1"; //local host
	
	// Variables for receiving VoIP - public to handle cleanup and avoid leaving socket or play-back open
	static DatagramSocket voice_receive_socket = null;
	static SourceDataLine playbackLine = null;
	
	// Threads
	static Thread textReceiveThread = null;
	Thread voipCaptureThread = null;
	Thread voipReceiveThread = null;
	
	/* ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧
	 * ▧ ▧ vars for voip send ▧ 
	 * ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ ▧ */
	AudioFormat sample_format; /* encoding technique, channels, bps, byte order, signed */
	static boolean isCalling = false;
	TargetDataLine captureLine = null;
	static int packet_length = 1500;
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
	 * The main method of the application. It starts the text receiving thread.
	 */
	public static void main(String[] args) {
	    // 1. Create the app's window
	    App app = new App("CN2 - AUTH - TEAM AE");
	    app.setSize(500, 250);
	    app.setVisible(true);

	    // 2. Start text thread - infinite loop
	    textReceiveThread = new Thread( () -> receiveText());
	    textReceiveThread.start();	    

	}
	
	// Text Handling Method from the receiving point
	private static void receiveText() {
	    InetAddress dest;
	    try (DatagramSocket text_receiver_socket = new DatagramSocket(text_dest_port)) {
	        System.out.println("Listening for messages on port " + text_dest_port);

	        // Continuously listen for incoming text messages
	        byte[] buffer = new byte[packet_length];

	        while (true) {
	            try {
	                dest = InetAddress.getByName(dest_addr);

	                // Handle text messages
	                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
	                text_receiver_socket.receive(incomingPacket);

	                byte[] encryptedData = new byte[incomingPacket.getLength()];
	                System.arraycopy(incomingPacket.getData(), 0, encryptedData, 0, incomingPacket.getLength());
	                
	                byte[] decryptedMessage = decrypt(encryptedData);
	                String receivedMessage = new String(decryptedMessage).trim();

	                App.textArea.append("Friend: " + receivedMessage + newline);

	            } catch (IOException e) {
	                System.out.println("Error receiving message: " + e.getMessage());
	            } catch (Exception e) {
	                System.out.println("Decryption error: " + e.getMessage());
	            }
	        }

	    } catch (Exception e) {
	        System.out.println("Error in text handling: " + e.getMessage());
	    }
	}
	
	// VoIP Handling Method from the receiving point
	private static void receiveVoIP() {    
	    try {
	        // Open the receive socket
	        voice_receive_socket = new DatagramSocket(voip_dest_port); 
	        System.out.println("VoIP receiving thread started on port " + voip_dest_port);
	    } catch (SocketException e) {
	        System.out.println("Cannot open receive socket: " + e.getMessage());
	        return;
	    }

	    // Set up audio play-back
	    AudioFormat format = new AudioFormat(8000, 8, 1, true, true);
	    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

	    try {
	        playbackLine = (SourceDataLine) AudioSystem.getLine(info);
	        playbackLine.open(format);
	        playbackLine.start();
	    } catch (LineUnavailableException e) {
	        System.out.println("Line unavailable for playback: " + e.getMessage());
	        return;
	    }

	    byte[] buffer = new byte[packet_length];
	    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);


        while (isCalling) { // This will be interrupted by stopReceiveVoIP function to handle cleanup gracefully
            try {
                // Receive the VoIP packet
                voice_receive_socket.receive(incomingPacket);
                byte[] receivedData = incomingPacket.getData();
                int length = incomingPacket.getLength();

                // Write the audio data to the play-back line
                playbackLine.write(receivedData, 0, length);
            } catch (IOException e) {
            	// Cleanup function called in case of error
            	receiveVoIPCleanup();	 
            }
        }
	    
	}
	
	private static void receiveVoIPCleanup() {
		// Clean up resources
        if (playbackLine != null) {
            playbackLine.drain();
            playbackLine.close();
        }
        if (voice_receive_socket != null && !voice_receive_socket.isClosed()) {
            voice_receive_socket.close();
        }
	}
			
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		 // Check which button was clicked.

		if (e.getSource() == sendButton){            
		    // The "Send" button was clicked
		    String input_text;    
		    InetAddress dest;
		    DatagramPacket text_sender;
		    DatagramSocket text_sender_socket;
		    byte[] payload;
		    int multiplier; int modulo;

		    /* get text from text area */
		    if(App.inputTextField.getText().length() > 0) {
		        input_text = App.inputTextField.getText();

		        /* create a udp socket */
		        try {
		            text_sender_socket = new DatagramSocket(text_src_port);
		        } catch (SocketException e1) {
		            System.out.println("Cannot open socket, quitting...");
		            System.out.print(e1.getMessage());
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
		        try {
		            payload = encrypt(input_text.getBytes());
		        } catch (Exception e1) {
		            System.out.println("Error encrypting message: " + e1.getMessage());
		            return;
		        }

		        multiplier = payload.length / packet_length + 1;
		        modulo = payload.length % packet_length;
		        // System.out.println("Multiplier = " + (multiplier - 1) + ", modulo = " + modulo);
		        for (int i = 0; i < multiplier; i++) {
		            // System.out.println("i = " + i);

		            /* load up the ith packet of 1024 bytes, or the final (multiplier - 1)th packet of modulo bytes. 
		             * if the ith packet is loaded, send 1024 bytes. if the final packet is loaded, send any remaining
		             * bytes (this is always <= 1024).
		             */
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
		    } else {
		        System.out.println("You need to type something genius!");
		    }
		

			
			
		}else if(e.getSource() == callButton) {
		    // The "Call" button was clicked				
		    if(isCalling == false) { // Start call

		        isCalling = true;
		        
		        voipCaptureThread = new Thread(() -> sendVoIP());
		        voipCaptureThread.start();
		       
		        voipReceiveThread = new Thread(() -> receiveVoIP());
		        voipReceiveThread.start();
		    }
		    else if (isCalling == true) { // Stop call
		        // Close line, drain audio buffer, and send remaining bytes
		        isCalling = false;
		        try {
		            // Stop the threads gracefully by interrupting them
		            voipReceiveThread.interrupt(); 
		            voipCaptureThread.interrupt(); 
		            
		            // Wait for the threads to finish with proper timeout
		            voipReceiveThread.join(1000); 
		            voipCaptureThread.join(1000); 
		            
		            receiveVoIPCleanup();
		            
		        } catch (Exception ex) {
		            System.out.println("Error during thread join: " + ex.getMessage());
		            // In case of an interruption, set isCalling to false and stop all threads
		            isCalling = false;	           
		      	}
		    }
		}

	}

	void sendVoIP() {
		/* open sockets first, then start capturing audio */
		InetAddress dest;
		DatagramSocket voice_send_socket = null;
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

		/* set audio format */
		int buffer_size = 1024;
		/* endianness should not matter here since we're using 8-bit PCM, but we're using big endian */
		AudioFormat format = new AudioFormat(8000, 8, 1, true, true);	
		/* automatically find the most suitable TargetDataLine for this format */
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); 
		if (AudioSystem.isLineSupported(info) == false) {
			System.out.println("Line is not supported! Quitting...");
			voice_send_socket.close();
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
		while (isCalling == true) {
		   /* Read the next chunk of data from the TargetDataLine to audio buffer */
		   numBytesRead =  captureLine.read(data, 0, data.length);
		   /* Save this chunk of data to BAOS */
		   out.write(data, 0, numBytesRead);
		   //System.out.println("out: " + out.toByteArray()[i]);
		   
		   /* send udp datagram */
		    audio_data = out.toByteArray();
			audio_data_length = audio_data.length;
			
			if(audio_data_length >= (packet_number) * packet_length) {
				voice_send = new DatagramPacket(audio_data, (packet_number - 1) * packet_length, packet_length, dest, voip_dest_port);
				System.out.println("Sending packet " + (packet_number - 1) + " (" + packet_length + " bytes)");
				System.out.println("Audio data total length: " + audio_data_length);
				packet_number = packet_number + 1;
				/* send the datagram through the socket */
				try {
					voice_send_socket.send(voice_send);
				} catch (IOException e1) {
					System.out.println("Cannot send voice datagram through socket for whatever reason");
					captureLine.close();
					voice_send_socket.close();
					return;
				}
						
			}
/*		   if(i % 4 == 0) {
		   		System.out.println("out size: " + out.toByteArray().length);
		   }*/
		}
		voice_send_socket.close();
		captureLine.close();
		
		/* debug what is being captured */
		//File file = new File("D:\\Σχολή\\9ο εξάμηνο\\Δίκτυα Υπολογιστών ΙΙ\\Εργασία\\test.wav");
		File file = new File("/home/pacopacorius/test.wav");
		try {
			file.createNewFile();
		} catch (IOException e) {
			System.out.println("Error creating file, continuing execution nonetheless");
		}
		ByteArrayInputStream audio_input = new ByteArrayInputStream(audio_data);
		AudioInputStream total_collected_audio = new AudioInputStream(audio_input, format, audio_data_length);			
		int bytes_written_to_file = 0; 
		try {
			bytes_written_to_file = AudioSystem.write(total_collected_audio, AudioFileFormat.Type.WAVE, file);
		} catch (IOException e) {
			System.out.println("Couldn't write to file, continuing execution nonetheless");
			//return;
		}
		try {
			total_collected_audio.close();
		} catch (IOException e) {
			System.out.println("error in closing audioinputstream");
		}
		System.out.println("Written " + bytes_written_to_file + " bytes to file ~/test.wav");
		
	}
	private static byte[] encrypt(byte[] data) throws Exception {
	    // Generate a new IV
	    byte[] iv = new byte[16];
	    SecureRandom random = new SecureRandom();
	    random.nextBytes(iv);

	    // Initialize cipher with IV
	    IvParameterSpec ivSpec = new IvParameterSpec(iv);
	    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

	    // Encrypt the data
	    byte[] encryptedData = cipher.doFinal(data);

	    // Prepend IV to the encrypted data
	    byte[] combined = new byte[iv.length + encryptedData.length];
	    System.arraycopy(iv, 0, combined, 0, iv.length);
	    System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

	    return combined;
	}

	private static byte[] decrypt(byte[] data) throws Exception {
	    // Extract the IV (first 16 bytes)
	    byte[] iv = new byte[16];
	    System.arraycopy(data, 0, iv, 0, 16);
	    
	    // Extract the actual encrypted data
	    byte[] encryptedData = new byte[data.length - 16];
	    System.arraycopy(data, 16, encryptedData, 0, encryptedData.length);

	    // Initialize cipher with IV
	    IvParameterSpec ivSpec = new IvParameterSpec(iv);
	    cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
	    
	    return cipher.doFinal(encryptedData);
	}

	
/* mock audio generation function used for testing
	private static void mockVoIPSender() {
	    DatagramSocket mockSendSocket = null;
	    try {
	        mockSendSocket = new DatagramSocket();
	    } catch (SocketException e) {
	        System.out.println("Cannot open mock sender socket: " + e.getMessage());
	        return;
	    }

	    InetAddress dest;
	    try {
	        dest = InetAddress.getByName("localhost"); // Send to localhost for testing
	    } catch (UnknownHostException e) {
	        System.out.println("Cannot resolve localhost: " + e.getMessage());
	        return;
	    }

	    AudioFormat format = new AudioFormat(8000, 8, 1, true, true);
	    int sampleRate = (int) format.getSampleRate();
	    int packetLength = 1024; // Same as used in the actual implementation
	    byte[] packetData = new byte[packetLength];

	    double frequency = 440.0; // A4 note frequency for testing
	    double increment = (2 * Math.PI * frequency) / sampleRate;
	    double angle = 0.0;

	    try {
	        while (isCalling && !Thread.currentThread().isInterrupted()) { // Check for interruption and isCalling flag
	            // Generate a sine wave
	            for (int i = 0; i < packetLength; i++) {
	                packetData[i] = (byte) (Math.sin(angle) * 127); // Scale to byte range
	                angle += increment;
	                if (angle > (2 * Math.PI)) {
	                    angle -= (2 * Math.PI);
	                }
	            }

	            // Send the generated packet
	            DatagramPacket packet = new DatagramPacket(packetData, packetLength, dest, voip_dest_port);
	            mockSendSocket.send(packet);

	            // Sleep for the duration of the packet to mimic real-time streaming
	            Thread.sleep((int) (1000.0 * packetLength / sampleRate));
	        }
	    } catch (IOException | InterruptedException e) {
	        if (!(e instanceof InterruptedException)) {
	        	System.out.println("Error in mock sender: " + e.getMessage());
	        }
	    } finally {
	        // Ensure the socket is properly closed
	        if (mockSendSocket != null && !mockSendSocket.isClosed()) {
	            mockSendSocket.close();
	        }
	    }
	}
*/
	
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