import os
import os.path
import time
from datetime import datetime
from PIL import Image, ImageDraw, ImageFont

# 定义 channels 列表
channels = [
    {"name": "附加悬挂S5", "code": "S5", "side": "L"},
    {"name": "支持装置S1", "code": "S1", "side": "L"},
    {"name": "支持装置S3", "code": "S3", "side": "L"},
    {"name": "附加悬挂S7", "code": "S7", "side": "L"},
    {"name": "吊弦S23", "code": "S23", "side": "L"},
    {"name": "杆号相机S21", "code": "S21", "side": "L", "pole": True},
    {"name": "附加悬挂S6", "code": "S6", "side": "R"},
    {"name": "支持装置S2", "code": "S2", "side": "R"},
    {"name": "支持装置S4", "code": "S4", "side": "R"},
    {"name": "附加悬挂S8", "code": "S8", "side": "R"},
    {"name": "吊弦S24", "code": "S24", "side": "R"},
    {"name": "杆号相机S22", "code": "S22", "side": "R", "pole": True}
]

# 定义输出目录
output_dir = "C:/Users/xubh/Downloads/output_images"
os.makedirs(output_dir, exist_ok=True)

# 定义字体
font_path = "C:/Windows/Fonts/simhei.ttf"  # 你可以使用其他字体文件
font_size = 20
font = ImageFont.truetype(font_path, font_size)

# 生成图片
for channel in channels:
    # 创建空白图像
    img = Image.new('RGB', (1920, 1080), color=(255, 255, 255))  # 增加分辨率
    draw = ImageDraw.Draw(img)

    # 获取当前时间戳
    timestamp = datetime.now().strftime("%H:%M:%S.%f")

    # 添加背景渐变
    for y in range(1080):
        color = (y % 256, (y // 2) % 256, (y // 4) % 256)
        draw.line([(0, y), (1920, y)], fill=color)

    # 绘制文本
    text = f"{channel['name']} - {timestamp}"
    draw.text((img.width/2, img.height/2), text, fill=(255, 255, 255), font=font, anchor="mm")

    # 保存图像
    filename = f"{channel['code']}_{int(time.time())}.jpg"  # 使用JPEG格式
    channel_dir = os.path.join(output_dir, channel['code'])
    os.makedirs(channel_dir, exist_ok=True)  # 确保目录存在
    filepath = os.path.join(channel_dir, filename)

    # 调整图片质量和大小
    img.save(filepath, optimize=False, compress_level=0,quality=1920)  # 降低质量
