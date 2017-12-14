package net.termer.jchat.server;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {
   BufferedReader in;
   PrintWriter out;
   JFrame frame = null;
   JTextField textField = new JTextField(40);
   JTextArea messageArea = new JTextArea(12, 40);
   JButton sendBtn = new JButton("Send");
   JButton logoutBtn = new JButton("Log out");
   JButton exitBtn = new JButton("Exit");
   JButton sendFile = new JButton("Send a file");
   JFileChooser f2send = new JFileChooser();
   
   public Main() {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException | ClassNotFoundException e1) {
         System.out.println("Could not set look and feel");
         e1.printStackTrace();
      }

      this.frame = new JFrame("JChat Client");
      this.textField.setEditable(false);
      this.messageArea.setEditable(false);
      this.sendBtn.setEnabled(false);
      this.logoutBtn.setEnabled(false);
      this.exitBtn.setEnabled(false);
      this.sendFile.setEnabled(false);
      this.textField.setMargin(new Insets(4, 4, 4, 4));
      this.messageArea.setMargin(new Insets(4, 4, 4, 4));
      this.frame.getContentPane().add(this.textField, "East");
      this.frame.getContentPane().add(this.sendBtn, "South");
      this.frame.getContentPane().add(new JScrollPane(this.messageArea), "Center");
      this.frame.pack();
      
      f2send.setDialogTitle("Select file to send");
      f2send.setDialogType(JFileChooser.OPEN_DIALOG);
      
      this.textField.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Main.this.send(Main.this.textField.getText());
            Main.this.textField.setText("");
         }
      });
      this.sendBtn.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Main.this.send(Main.this.textField.getText());
            Main.this.textField.setText("");
         }
      });
      this.logoutBtn.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Main.this.out.close();
         }
      });
      this.exitBtn.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(JOptionPane.showConfirmDialog(null, "Are you sure you want to exit JChat?") == 0) {
               System.exit(0);
            }
         }
      });
      this.sendFile.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String u = JOptionPane.showInputDialog("Send to which user?");
			f2send.showOpenDialog(null);
			if(f2send.getSelectedFile().exists()) {
				out.println("SENDFILEOK "+u);
			} else {
				JOptionPane.showMessageDialog(null, "File does not exist");
			}
		}
      });
      this.frame.addWindowListener(new WindowListener() {
         public void windowOpened(WindowEvent e) {
         }

         public void windowClosing(WindowEvent e) {
            if(JOptionPane.showConfirmDialog(null, "Are you sure you want to exit JChat?") == 0) {
               System.exit(0);
            }

         }

         public void windowClosed(WindowEvent e) {
         }

         public void windowIconified(WindowEvent e) {
         }

         public void windowDeiconified(WindowEvent e) {
         }

         public void windowActivated(WindowEvent e) {
         }

         public void windowDeactivated(WindowEvent e) {
         }
      });
      this.frame.requestFocus();
      this.textField.requestFocus();
      this.frame.setJMenuBar(new JMenuBar());
      this.frame.getJMenuBar().add(this.logoutBtn, 0);
      this.frame.getJMenuBar().add(this.exitBtn, 1);
      this.frame.getJMenuBar().add(this.sendFile, 2);
      this.frame.getJMenuBar().setVisible(true);
   }

   private String getServerAddress() {
      return JOptionPane.showInputDialog(this.frame, "Enter IP Address of the Server:", "Welcome to JChat", 3).trim();
   }

   private String getName() {
      return JOptionPane.showInputDialog(this.frame, "Type the name you\'d like to be known by.\nNOTE: If this pops up again, that means that the name is already in use.", "Type name", -1);
   }

   private void run() throws IOException {
      Socket socket = null;

      try {
         String serverAddress = this.getServerAddress();
         if(serverAddress.isEmpty() || serverAddress == null) {
            System.exit(0);
         }

         System.out.println("Connecting...");
         socket = new Socket(InetAddress.getByName(serverAddress).getHostAddress(), 9001);
         this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         this.out = new PrintWriter(socket.getOutputStream(), true);
      } catch (IOException err) {
         socket.close();
         System.out.println("Could not connect to server");
         JOptionPane.showMessageDialog(null, "Could not connect to server");
      }

      System.out.print("Connected to " + socket.getInetAddress().getHostAddress());
      JFileChooser floc = new JFileChooser();
      floc.setDialogTitle("Select Save Location");
      floc.setDialogType(JFileChooser.SAVE_DIALOG);
      boolean fileOk = false;
      String fsender = "";
      long rf = 0;
      while(true) {
    	 if(rf>0) {
    		 System.out.println("Receiving file from "+fsender+"...");
    		 FileOutputStream fout = new FileOutputStream(floc.getSelectedFile());
    		 while(rf>0) {
    			 int c = in.read();
    			 System.out.print("Receiving and writing "+(char)c+"...");
    			 fout.write(c);
    			 rf--;
    			 System.out.println(Long.toString(rf)+" bytes left");
    		 }
    		 fout.close();
    	 }
    	 String line = this.in.readLine();
         System.out.println("IN: " + line);
         if(line.startsWith("SUBMITNAME")) {
            this.send(this.getName());
         } else if(line.startsWith("NAMEACCEPTED")) {
            this.textField.setEditable(true);
            this.sendBtn.setEnabled(true);
            this.logoutBtn.setEnabled(true);
            this.exitBtn.setEnabled(true);
            this.sendFile.setEnabled(true);
         } else if(line.startsWith("MESSAGE")) {
            this.messageArea.append(line.substring(8) + "\n");
            this.messageArea.setCursor(new Cursor(2));
         } else if(line.startsWith("KICK")) {
            JOptionPane.showMessageDialog((Component)null, "You have been kicked!");
            this.frame.dispose();
            System.out.println("Disconnected");
            return;
         } else if(line.startsWith("OKFILE")) {
        	 if(JOptionPane.showConfirmDialog(null, "User "+line.substring(7)+" wants to send you a file. Receive it?")==0) {
        		 fileOk=true;
        		 fsender=line.substring(7);
        		 floc.showOpenDialog(null);
        		 out.println("ACCEPTFILE "+line.substring(7));
        	 }
         } else if(line.startsWith("FILEACCEPTED")) {
        	 String u = line.substring(13);
        	 out.println("SENDFILE "+u+" "+Long.toString(f2send.getSelectedFile().length()));
        	 FileInputStream fin = new FileInputStream(f2send.getSelectedFile());
        	 while(fin.available()>0) {
        		 out.write(fin.read());
        	 }
        	 fin.close();
         } else if(line.startsWith("FILE")) {
        	 String[] f = line.substring(5).split(" ");
        	 if(fileOk) {
        		rf = Long.parseLong(f[0]);
        		System.out.println("File size: "+f[0]+" bytes");
        		System.out.println("Receiving file from "+fsender+"...");
       		 FileOutputStream fout = new FileOutputStream(floc.getSelectedFile());
       		 while(rf>0) {
       			 int c = in.read();
       			 fout.write(c);
       			 rf--;
       		 }
       		 fout.close();
        	 } else {
        		 System.out.println("");
        	 }
         }
      }
   }

   public static void main(String[] args) throws Exception {
      boolean rl = true;
      
      while(true) {
         Main client = new Main();
         client.frame.setDefaultCloseOperation(0);
         client.frame.setVisible(true);

         try {
            if(rl) {
               rl = false;
               //Force reload to fix GUI issues
               throw new Exception("Reload");
            }

            client.run();
         } catch (Exception e) {
            if(e instanceof IOException) {
               JOptionPane.showMessageDialog(null, "Connection lost", "", 0);
            }
            client.frame.dispose();
         }
      }
   }

   private void send(String str) {
      if(!str.isEmpty()) {
         System.out.println("OUT: " + str);
      }

      this.out.println(str);
   }
}
