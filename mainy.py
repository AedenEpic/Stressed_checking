# users/main.py
import threading
import time
import psutil
import os
import platform

# Optional: try to use GPUtil for NVIDIA GPUs
try:
    import GPUtil
    GPUtil_available = True
except ImportError:
    GPUtil_available = False

def cpu_stress(duration=10, threads=4):
    def burn():
        x = 0
        while True:
            x += 1
            x *= 2
            x = x % 10000000

    print(f"\n🔥 CPU Stress Test: {threads} threads for {duration} seconds...")
    jobs = []
    for _ in range(threads):
        t = threading.Thread(target=burn)
        t.daemon = True
        t.start()
        jobs.append(t)

    time.sleep(duration)
    print("✅ CPU test complete.\n")

def ram_stress(duration=10, size_mb=500):
    print(f"\n🧠 RAM Stress Test: Allocating {size_mb}MB for {duration} seconds...")
    try:
        big_list = [0] * (size_mb * 1024 * 1024 // 8)  # 8 bytes per int
        time.sleep(duration)
        del big_list
        print("✅ RAM test complete.\n")
    except MemoryError:
        print("❌ Not enough memory!")

def print_system_stats():
    total_ram = psutil.virtual_memory().total / (1024**3)
    cpu_cores = psutil.cpu_count(logical=True)
    print("\n🧠 System Info:")
    print(f"- CPU cores: {cpu_cores}")
    print(f"- Memory: {round(total_ram, 2)} GB")
    print(f"- OS: {platform.system()} {platform.release()}")
    return total_ram, cpu_cores

def get_gpu_stats():
    try:
        system = platform.system()
        if GPUtil_available:
            gpus = GPUtil.getGPUs()
            if gpus:
                return max(gpu.memoryTotal for gpu in gpus)
        elif system == "Darwin":  # macOS
            import subprocess
            result = subprocess.run(["system_profiler", "SPDisplaysDataType"], capture_output=True, text=True)
            lines = result.stdout.split('\n')
            for line in lines:
                if "VRAM" in line:
                    parts = line.strip().split()
                    for part in parts:
                        if "GB" in part:
                            return float(part.replace("GB", ""))
        elif system == "Windows":
            import wmi
            w = wmi.WMI()
            gpus = w.Win32_VideoController()
            vram_list = [float(gpu.AdapterRAM)/(1024**3) for gpu in gpus if gpu.AdapterRAM]
            return max(vram_list) if vram_list else 0
    except Exception:
        return 0
    return 0

def recommend_environment(ram, cpu_cores, vram):
    print("\n📊 Environment & Tool Recommendations:")

    # Music
    print("\n🎵 Music:")
    if ram < 8:
        print("- VLC or local MP3s (best for low RAM)")
    elif ram < 12:
        print("- YouTube Music on browser with low refresh rate (close background tabs)")
    else:
        print("- Spotify desktop or Apple Music (Mac)")

    # IDE
    print("\n💻 IDE:")
    if ram >= 8:
        print("- VS Code (recommended)")
        if ram >= 12:
            print("- PyCharm Community Edition")
    else:
        print("- Sublime Text (ultra-lightweight)")

    # Browsers
    print("\n🌐 Browser:")
    if ram < 12 or cpu_cores < 12:
        print("- Avoid Chrome unless no background apps are running")
        print("- Prefer Firefox or Brave")
    else:
        print("- Chrome is okay for casual use")
        print("- Brave and Edge are good alternatives")

    # Gaming Use Case
    print("\n🎮 Capability:")
    if ram >= 16 and vram >= 12:
        print("- System is capable for moderate to heavy gaming")
    else:
        print("- Light gaming recommended only; upgrade for better experience")

if __name__ == "__main__":
    print("📊 Welcome to stress-check")
    ram_total, cpu_cores = print_system_stats()
    vram_total = get_gpu_stats()
    cpu_stress(duration=10, threads=cpu_cores)
    ram_stress(duration=10, size_mb=500)
    recommend_environment(ram_total, cpu_cores, vram_total)
    print("✅ All tests finished.")
