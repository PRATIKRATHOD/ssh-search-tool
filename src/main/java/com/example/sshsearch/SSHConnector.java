package com.example.sshsearch;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.Properties;

public class SSHConnector {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private Session session;

    public SSHConnector(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    private synchronized void connect() throws Exception {
        if (session != null && session.isConnected()) return;
        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(5000); // 5s timeout
    }

    public synchronized String runCommand(String command) throws Exception {
        // Ensure connected
        connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);
        InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int len = in.read(buffer, 0, buffer.length);
                if (len < 0) break;
                output.append(new String(buffer, 0, len));
            }
            if (channel.isClosed()) break;
            Thread.sleep(50);
        }

        channel.disconnect();
        return output.toString().trim();
    }

    public synchronized void disconnect() {
        try {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        } finally {
            session = null;
        }
    }

    public synchronized boolean isConnected() {
        return session != null && session.isConnected();
    }
}
