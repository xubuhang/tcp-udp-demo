import socket
import time
import signal
import sys
import json
import random

# 定义目标地址和端口号
target_ip = '127.0.0.1'
target_port = 30100

# 创建UDP套接字
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

# 定义要发送的数据
data = {
    "daogaos": [
        {
            "name": "1",
            "value": 120.0
        }
    ],
    "kmName": "K23",
    "lachus": [
        {
            "name": "1",
            "value": 310.0
        }
    ],
    "lat": 100.0,
    "lng": 100.0,
    "poleName": "2007",
    "speed": 10.0
}

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
    # 更新 poleName 字段
    data['poleName'] = str(int(time.time()))
    
    # 更新 value 字段，使其在200以内随机浮动
    data['daogaos'][0]['value'] = random.uniform(0, 200)
    data['lachus'][0]['value'] = random.uniform(0, 200)
    
    # 将字典转换为JSON字符串
    json_data = json.dumps(data)
    
    # 发送消息
    sock.sendto(json_data.encode('utf-8'), (target_ip, target_port))
    print(f"Sent: {json_data}")
    
    # 等待1秒
    time.sleep(1)

# 关闭套接字
sock.close()