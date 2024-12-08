package server;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerWindow {
    private JFrame frame;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private List<ClientHandler> clients = new ArrayList<>();

    public ServerWindow() {
        frame = new JFrame("Chat Server");
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startServer();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });

        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);
        frame.add(panel);
        frame.add(new JScrollPane(logArea), "South");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void startServer() {
        if (!isRunning) {
            try {
                serverSocket = new ServerSocket(12345);
                isRunning = true;
                logArea.append("Server started...\n");
                new Thread(() -> {
                    while (isRunning) {
                        try {
                            Socket socket = serverSocket.accept();
                            logArea.append("Client connected...\n");
                            ClientHandler clientHandler = new ClientHandler(socket);
                            clients.add(clientHandler);
                            new Thread(clientHandler).start();
                        } catch (IOException e) {
                            logArea.append("Error: " + e.getMessage() + "\n");
                        }
                    }
                }).start();
            } catch (IOException e) {
                logArea.append("Error starting server: " + e.getMessage() + "\n");
            }
        }
    }

    private void stopServer() {
        if (isRunning) {
            try {
                for (ClientHandler client : clients) {
                    client.stop();
                }
                serverSocket.close();
                isRunning = false;
                logArea.append("Server stopped.\n");
            } catch (IOException e) {
                logArea.append("Error stopping server: " + e.getMessage() + "\n");
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                logArea.append("Error setting up client handler: " + e.getMessage() + "\n");
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    logArea.append("Received: " + message + "\n");
                    broadcast(message, this); // Отправляем сообщение всем, кроме отправителя
                }
            } catch (IOException e) {
                logArea.append("Client disconnected.\n");
            } finally {
                stop();
            }
        }

        public void stop() {
            try {
                socket.close();
            } catch (IOException e) {
                logArea.append("Error closing client socket: " + e.getMessage() + "\n");
            }
        }

        private void broadcast(String message, ClientHandler sender) {
            for (ClientHandler client : clients) {
                if (client != sender) { // Не отправляем сообщение самому себе
                    client.out.println(message);
                }
            }
        }
    }
}