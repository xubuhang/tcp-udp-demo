package com.xubh;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPClient {
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private String serverAddress;
    private int port;
    private static final int RETRY_LIMIT = 3; // 最大重试次数
    private static final int TIMEOUT = 10000; // 超时时间（毫秒）
    private static final int HEARTBEAT_INTERVAL = 5000; // 心跳间隔（毫秒）
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>(); // 用于存储接收到的消息

    public TCPClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
        connectToServer();
        startHeartbeat();
        startMessageReceiver();
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverAddress, port);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Connected to server");
            // 发送客户端标识符
            String clientId = "client1"; // 客户端唯一标识
            dataOutputStream.writeUTF(clientId);
            dataOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            retryConnection();
        }
    }

    private void retryConnection() {
        while (socket == null || socket.isClosed()) {
            try {
                System.out.println("Attempting to reconnect in 5 seconds...");
                Thread.sleep(5000);
                connectToServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (dataOutputStream != null) {
            try {
                dataOutputStream.writeUTF("MESSAGE:" + message);
                dataOutputStream.flush();
                System.out.println("Sent: MESSAGE:" + message);

                boolean confirmed = waitForConfirmation("MESSAGE_RECEIVED");
                if (confirmed) {
                    System.out.println("Server confirmed message reception: " + message);
                } else {
                    System.err.println("Failed to receive confirmation after " + RETRY_LIMIT + " attempts");
                }
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }
    }

    public void sendFile(File file) {
        if (dataOutputStream != null) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                dataOutputStream.writeUTF("FILE:" + file.getName());
                dataOutputStream.flush();

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    dataOutputStream.write(buffer, 0, bytesRead);
                }
                dataOutputStream.flush();
                System.out.println("File sent: " + file.getName());

                boolean confirmed = waitForConfirmation("FILE_RECEIVED");
                if (confirmed) {
                    System.out.println("Server confirmed file reception: " + file.getName());
                } else {
                    System.err.println("Failed to receive confirmation after " + RETRY_LIMIT + " attempts");
                }
            } catch (IOException e) {
                System.err.println("Error sending file: " + e.getMessage());
            }
        }
    }

    private boolean waitForConfirmation(String expectedResponse) {
        int retryCount = 0;
        while (retryCount < RETRY_LIMIT) {
            try {
                socket.setSoTimeout(TIMEOUT);
                String response = messageQueue.poll(TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (expectedResponse.equals(response)) {
                    return true;
                } else if (response != null) {
                    System.err.println("Unexpected response: " + response);
                }
            } catch (InterruptedException e) {
                System.err.println("Error receiving confirmation: " + e.getMessage());
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            retryCount++;
            System.out.println("Retrying to send, attempt " + retryCount);
        }
        return false;
    }

    private void startHeartbeat() {
        Thread heartbeatThread = new Thread(() -> {
            try {
                while (true) {
                    dataOutputStream.writeUTF("HEARTBEAT");
                    dataOutputStream.flush();
                    Thread.sleep(HEARTBEAT_INTERVAL);
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Heartbeat error: " + e.getMessage());
            }
        });
        heartbeatThread.start();
    }

    private void startMessageReceiver() {
        Thread receiverThread = new Thread(() -> {
            try {
                while (true) {
                    String receivedMessage = dataInputStream.readUTF();
                    if (!receivedMessage.equals("HEARTBEAT")) {
                        System.out.println("Received from server: " + receivedMessage);
                        messageQueue.offer(receivedMessage); // 将消息存入队列
                    }
                }
            } catch (IOException e) {
                System.err.println("Error receiving message from server: " + e.getMessage());
            }
        });
        receiverThread.start();
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int port = 12345;
        TCPClient client = new TCPClient(serverAddress, port);
        client.sendMessage("24234");

        client.sendFile(new File("C:/Users/xubh/Downloads/龙门架.png"));
        client.sendMessage("=========");
        client.sendFile(new File("C:/Users/xubh/Downloads/map-demo.zip"));
        client.sendMessage("24234");
    }
}
