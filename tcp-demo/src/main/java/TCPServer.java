import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.*;

public class TCPServer {
    private static final int THREAD_POOL_SIZE = 10;
    private static ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static ConcurrentHashMap<String, Socket> clientMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Socket, DataOutputStream> outputStreamMap = new ConcurrentHashMap<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    static String rootPath ;
    public static void main(String[] args) {
        rootPath = "/home/FileSpace"; // 默认根路径
        int port = 12345; // 默认端口

        // 解析命令行参数
        if (args.length >= 2) {

            try {
                port = Integer.parseInt(args[0]); // 第二个参数为端口
            } catch (NumberFormatException e) {
                System.out.println("无效的端口号，使用默认端口 12345");
            }
            rootPath = args[1]; // 第一个参数为根路径
        } else {
            System.out.println("使用默认根路径和端口,port:"+port+" "+"rootPath:"+rootPath);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port 12345...");
            startHeartbeat();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())) {
            // 接收客户端标识符
            String clientId = dataInputStream.readUTF();
            clientMap.put(clientId, clientSocket);
            outputStreamMap.put(clientSocket, dataOutputStream);
            System.out.println("Client connected: " + clientId + " from " + clientSocket.getInetAddress());

            while (true) {
                String messageType = dataInputStream.readUTF();
                if (messageType.equals("HEARTBEAT")) {
                    System.out.println("Received heartbeat from " + clientSocket.getInetAddress());
                } else if (messageType.startsWith("MESSAGE:")) {
                    String message = messageType.substring(8);
                    System.out.println("Received message from " + clientSocket.getInetAddress() + ": " + message);
                    sendToClient(clientSocket, "MESSAGE_RECEIVED");
                } else if (messageType.startsWith("FILE:")) {
                    String fileName = messageType.substring(5);
                    saveFile(dataInputStream, clientSocket, fileName);
                }
            }
        } catch (SocketException e) {
            System.err.println("Socket exception handling client: " + e.getMessage());
            System.err.println("Client disconnected: " + clientSocket.getInetAddress());
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
            System.err.println("Client disconnected: " + clientSocket.getInetAddress());
        } finally {
            removeClient(clientSocket);
        }
    }
    private static void saveFile(DataInputStream dataInputStream, Socket clientSocket, String fileName) {

        File file = new File(rootPath + fileName);
        File dir = file.getParentFile();
        if(!dir.exists()){
            dir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                if (bytesRead < buffer.length) {
                    break; // End of file
                }
            }
            fos.flush();
            System.out.println("File received: " + fileName);
            sendToClient(clientSocket, "FILE_RECEIVED");
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }

    private static void sendToClient(String clientId, String message) {
        try {
            Socket clientSocket = clientMap.get(clientId);
            DataOutputStream dataOutputStream = outputStreamMap.get(clientSocket);
            if (dataOutputStream != null) {
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
        }
    }

    private static void sendToClient(Socket clientSocket, String message) {
        try {
            DataOutputStream dataOutputStream = outputStreamMap.get(clientSocket);
            if (dataOutputStream != null) {
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to client: " + e.getMessage());
        }
    }

    private static void removeClient(Socket clientSocket) {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        } finally {
            outputStreamMap.remove(clientSocket);
            clientMap.values().remove(clientSocket);
            System.out.println("Client disconnected and removed: " + clientSocket.getInetAddress());
        }
    }

    public static void sendMessageToAllClients(String message) {
        clientMap.forEach((clientId, clientSocket) -> {
            DataOutputStream dataOutputStream = outputStreamMap.get(clientSocket);
            try {
                dataOutputStream.writeUTF("SERVER_MESSAGE:" + message);
                dataOutputStream.flush();
            } catch (IOException e) {
                System.err.println("Error sending message to client: " + e.getMessage());
            }
        });
    }

    private static void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending heartbeat to all clients: " + clientMap.keySet());
            for (Map.Entry<String, Socket> entry : clientMap.entrySet()) {
                sendToClient(entry.getKey(), "HEARTBEAT");
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
}
