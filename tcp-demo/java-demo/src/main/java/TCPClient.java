import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPClient {
    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private static String serverAddress;
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
        if (!file.exists()) {
            return;
        }
        String filePath = file.getAbsolutePath();
        File root = new File(rootPath);
        String fileName = filePath.replace(root.getAbsolutePath(), "").replace("//", "/");
        if (dataOutputStream != null) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                dataOutputStream.writeUTF("FILE:" + fileName);
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

    static String rootPath ;
    static final String targetName = "info.xml";
    static final String doneName = "done.txt";
    public static void main(String[] args) {
        serverAddress = "localhost";
        rootPath = "/home/FileSpace"; // 默认根路径
        int port = 12345; // 默认端口

        // 解析命令行参数
        if (args.length >= 2) {
            String  ipport = args[0];
            String[] ipportArr =  ipport.split(":");
            serverAddress = ipportArr[0];
            try {
                port = Integer.parseInt(ipportArr[1]); // 第二个参数为端口
            } catch (NumberFormatException e) {
                System.out.println("无效的端口号，使用默认端口 12345");
            }
            rootPath = args[1]; // 第一个参数为根路径
        } else {
            System.out.println("使用默认根路径和端口,serverAddress:"+serverAddress+" "+"port:"+port+" "+"rootPath:"+rootPath);
        }
        TCPClient client = new TCPClient(serverAddress, port);
        while (true) {
            try {
                //查询rootPath下的xml所在的文件夹
                File[] files = new File(rootPath).listFiles(File::isDirectory);
                if(files==null){
                    return;
                }
                List<File> dirs = new ArrayList<>();
                for (File file : files) {
                    File target = new File(file.getAbsolutePath() + "/" + targetName);
                    File done = new File(file.getAbsolutePath() + "/" + doneName);
                    if (target.exists() && !done.exists()) {
                        dirs.add(file);
                    }
                }
                for (File dir : dirs) {
                    File[] notxmlfilesInDir = dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return !pathname.getName().toLowerCase().endsWith(targetName);
                        }
                    });
                    File[] xmlfilesInDir = dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.getName().toLowerCase().endsWith(targetName);
                        }
                    });
                    for (File file : notxmlfilesInDir) {
                        client.sendFile(file);
                    }
                    for (File file : xmlfilesInDir) {
                        client.sendFile(file);
                        try {
                            File v = new File(file.getParentFile().getAbsolutePath() + "/" + doneName);
                            v.createNewFile();
                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
