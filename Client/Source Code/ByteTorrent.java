import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.*;

import javax.swing.*;


public class ByteTorrent extends JFrame implements ActionListener{
	
    private static int PEER_UPDATE_INTERVAL = 15 * 1000;
	
	private JButton shareButton, downloadButton;
	private JLabel statusLabel;
    private JTextArea logArea;
    private JScrollPane logScrollPane;
    private TrackerClient tracker;
    private ConnectionManager manager;
    private Display display;
    private ConnectionMonitor monitor;
    
    int port;
     
	public ByteTorrent(){
		super("ByteTorrent");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				shutDown();
			}
		});

        shareButton = new JButton("Share File");
        shareButton.addActionListener(this);
      
        downloadButton = new JButton("Download File");
        downloadButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(shareButton);
        buttonPanel.add(downloadButton);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logScrollPane = new JScrollPane(logArea,
        						JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        					);
        statusLabel = new JLabel("Waiting to start...");
        add(buttonPanel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        
        display = new Display(){
			public void write(String message, int level){
				JScrollBar vs = logScrollPane.getVerticalScrollBar();
				boolean scroll = ((vs.getValue() + vs.getVisibleAmount()) == vs.getMaximum());
				//long s = System.currentTimeMillis();
				logArea.append(message+"\n" );
				//System.out.println(System.currentTimeMillis() - s);
				if(scroll){
					logArea.setCaretPosition(logArea.getDocument().getLength() );
				}
			}
		};
        //display = new GUIDisplay(logArea);
		
		monitor = new StatusbarConnectionMonitor();
        

        setSize(800,600);
        setLocationByPlatform(true);
        setVisible(true);
		
        String initAddres;
        try{
        	initAddres = InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException uhe){
        	initAddres ="";
        }
   
        String host = JOptionPane.showInputDialog(this, "Please Enter the Tracker's IP Address:", initAddres);
        if(host != null) {
        	try{
        		tracker = new TrackerClient(host, 9989);
        		display.write("Tracker Local Port: "+tracker.getPort(), 0);
        	} catch(IOException e) {
        		JOptionPane.showMessageDialog(ByteTorrent.this, e.getMessage());
                System.exit(0);
            }
        } else {
        	//skips shutdown as there is no traker to clean up for
        	System.exit(0);
        }
	}
	
	public void actionPerformed(ActionEvent e){
		if(e.getSource() == downloadButton){
			downloadButtonAction();
		} else if (e.getSource() == shareButton){
			shareButtonAction();
		}
	}
	
	private void downloadButtonAction(){
		String[] filenames = tracker.getList();
		if (filenames == null || filenames.length == 0) {
			JOptionPane.showMessageDialog(this, "Sorry, No Files Currently Available.");
		} else {
			String file = (String)JOptionPane.showInputDialog(
                    	this,
                    	"Select a File:\n",
                    	"Input",
                    	JOptionPane.QUESTION_MESSAGE,
                    	null,
                    	filenames,
                    	filenames[0]);
			if(file != null){
				FileInfo fInfo = tracker.getFileInfo(file);
				if (fInfo != null) {
					shareButton.setEnabled(false);
					downloadButton.setEnabled(false);
					
					try {
						manager = new ConnectionManager(fInfo);
					}catch (IOException ioe){
						shutDown();
					}
			        manager.addDisplay(display);
			        manager.addMonitor(monitor);
			        port = manager.startServer();
			        if(port != -1){
			        	tracker.registerPartial(fInfo, port);
			        }
			        Thread t = new Thread(new Runnable(){
			        	public void run() {
			        		while( ! manager.isFinished()){
				    			manager.addClient(tracker.getPeers(manager.getFileInfo()));
				    			try {
				    				Thread.sleep(PEER_UPDATE_INTERVAL);
				    			} catch (InterruptedException ie){
				    				System.err.println("PeerUpdater Interupted");
				    				return;
				    			}
			        		}
			    		}
			    	});
			        t.start();
			        
				}
			}
		}
	}
	
	private void shareButtonAction(){
	    JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		int option = chooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			shareButton.setEnabled(false);
			downloadButton.setEnabled(false);
			FileInfo fInfo = new FileInfo(chooser.getSelectedFile().getPath());
			BitSet pieces = new BitSet();
			pieces.flip(0, fInfo.getNumberOfPieces());
			try {
				manager = new ConnectionManager(fInfo, pieces);
			}catch (IOException ioe){
				shutDown();
			}
	        manager.addDisplay(display);
	        manager.addMonitor(monitor);
	        port = manager.startServer();
	        if (port != -1){
	        	tracker.registerComplete(fInfo, port);
	        }

		}
	}
	
	private void shutDown(){
		tracker.unregister(port);
		System.exit(0);
	}
	
	public static void main (String[] args){
		if (!(args.length == 1 && args[0].equalsIgnoreCase("jlaf"))){
			try	{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch(Exception e){
				
			}
		}
		new ByteTorrent();
	}
	
	private class StatusbarConnectionMonitor implements ConnectionMonitor{
		private boolean downloaded = false;
		private int d, h, t, s, c, sm, cm;

		public void duplicates(int d){
			this.d = d;
			updateStatusLabelText();
		}
		
		public void pieces(int h, int t){
			this.h = h;
			this.t = t;
			//display.write(h+"/"+t, 0);
			if(h == t && !downloaded){
				String[] message = new String[]{
						"Finished Downloading "+manager.getFileInfo().getFileName()+".",
						"Please keep the program open so that others can dowload.",
						"You will need to close the program before opening "+manager.getFileInfo().getFileName()+"."
				};
				JOptionPane.showMessageDialog(ByteTorrent.this, message);
				downloaded = true;
			}
			updateStatusLabelText();
		}
		
		public void servers(int s){
			this.s = s;
			if(s > sm) sm = s;
			updateStatusLabelText();
		}
		
		public void clients(int c){
			this.c = c;
			if (c > cm) cm = c;
			updateStatusLabelText();
		}
		
		private void updateStatusLabelText(){
			String pieces = t > 0 ? "Pieces ["+h+"/"+t+"] | " : "";
			String servers = "Servers ["+s+"("+sm+")] | ";
			String clients = "Clients ["+c+"("+cm+")] | ";
			String duplicates = "Duplicates ["+d+"]";
			statusLabel.setText(pieces+servers+clients+duplicates);
		}
	}
	
	private class GUIDisplay implements Display, Runnable {
		
		private ConcurrentLinkedQueue<String> queue;
		private JTextArea area;
		private Thread t;
		
		public GUIDisplay(){
			queue = new ConcurrentLinkedQueue<String>();
			Thread t = new Thread(this);
			t.setDaemon(true);
			t.start();
		}
		
		public void run(){
			while(true){
				try {
					Thread.sleep(2);
				} catch(InterruptedException ie){
					//ignore
				}
				String s = queue.poll();
				if(s != null) { 
					logArea.append(s+"\n");
				}
			}
		}
		
		public void write(String message, int level){
			long s = System.currentTimeMillis();
			queue.add(message);
			System.out.println(System.currentTimeMillis() - s);
		}
	}
}
