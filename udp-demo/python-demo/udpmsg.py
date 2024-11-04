import socket
import time
import signal
import sys

# 定义目标地址和端口号
target_ip = '127.0.0.1'
target_port = 30200

# 创建UDP套接字
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# 定义要发送的消息数组
messages = [
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\100\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\101\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\106\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\111\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\112\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\113\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\114\\",
    "{C4_PATH}&D:\\C4\\pictureDownPath\\20241029\\202410291638\\\\115\\"
]

# 定义一个标志变量，用于控制循环
running = True

# 定义信号处理函数，用于捕获 Ctrl+C 信号
def signal_handler(sig, frame):
    global running
    print("Terminating the program...")
    running = False
    sock.close()
    sys.exit(0)

# 注册信号处理函数
signal.signal(signal.SIGINT, signal_handler)

# 循环发送消息
while running:
    for msg in messages:
        if not running:
            break
        # 发送消息
        sock.sendto(msg.encode('utf-8'), (target_ip, target_port))
        print(f"Sent: {msg}")
        
        # 等待1秒
        time.sleep(1)

# 关闭套接字
sock.close()