// package com.example.sshsearch;

// import com.jcraft.jsch.*;
// import java.io.InputStream;

// public class App {
//     public static void main(String[] args) {
//         System.out.println("SSH Search Tool starting...");

//         String host = "localhost";
//         int port = 2222;
//         String user = "pratik";   // Ubuntu WSL username
//         String password = "pratik"; // change this

//         try {
//             // Setup SSH
//             JSch jsch = new JSch();
//             Session session = jsch.getSession(user, host, port);
//             session.setPassword(password);

//             session.setConfig("StrictHostKeyChecking", "no");
//             session.connect(5000);
//             System.out.println("✅ Connected to Ubuntu!");

//             // Command: search for ERROR in logs
//             String command = "grep -RIn \"ERROR\" ~/testlogs";

//             ChannelExec channel = (ChannelExec) session.openChannel("exec");
//             channel.setCommand(command);
//             channel.setInputStream(null);
//             channel.setErrStream(System.err);

//             InputStream in = channel.getInputStream();
//             channel.connect();

//             byte[] tmp = new byte[1024];
//             while (true) {
//                 while (in.available() > 0) {
//                     int i = in.read(tmp, 0, 1024);
//                     if (i < 0) break;
//                     System.out.print(new String(tmp, 0, i));
//                 }
//                 if (channel.isClosed()) {
//                     if (in.available() > 0) continue;
//                     System.out.println("\n\nCommand exit-status: " + channel.getExitStatus());
//                     break;
//                 }
//                 Thread.sleep(100);
//             }

//             channel.disconnect();
//             session.disconnect();
//             System.out.println("🔌 Disconnected.");
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
