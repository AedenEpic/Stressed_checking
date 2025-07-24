import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.nio.file.attribute.*;
import java.lang.management.*;
import java.awt.*;
import javax.swing.*;

public class SystemStressTest {

    // CPU Stress Test: Uses threads to perform computational load
    public static void cpuStress(int duration, int threads) {
        System.out.println("\n🔥 CPU Stress Test: " + threads + " threads for " + duration + " seconds...");
        Runnable burnTask = () -> {
            long x = 0;
            while (true) {
                x += 1;
                x *= 2;
                x = x % 10000000;
            }
        };
        
        List<Thread> threadList = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(burnTask);
            thread.start();
            threadList.add(thread);
        }

        try {
            Thread.sleep(duration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("✅ CPU test complete.\n");
    }

    // RAM Stress Test: Allocates a large amount of memory for testing
    public static void ramStress(int duration, int sizeMB) {
        System.out.println("\n🧠 RAM Stress Test: Allocating " + sizeMB + "MB for " + duration + " seconds...");
        try {
            byte[] memory = new byte[sizeMB * 1024 * 1024];
            Thread.sleep(duration * 1000);
            System.out.println("✅ RAM test complete.\n");
        } catch (OutOfMemoryError | InterruptedException e) {
            System.out.println("❌ Not enough memory!");
        }
    }

    // Get system stats like CPU cores and RAM
    public static void printSystemStats() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        long totalMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024);
        System.out.println("\n🧠 System Info:");
        System.out.println("- CPU cores: " + cpuCores);
        System.out.println("- Memory: " + totalMemory + " GB");
        System.out.println("- OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
    }

    // Find the largest files in a directory
    public static List<String> findLargestFiles(String directoryPath, int topN) throws IOException {
        List<String> largeFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                 .map(path -> {
                     try {
                         return new AbstractMap.SimpleEntry<>(path.toString(), Files.size(path));
                     } catch (IOException e) {
                         return null;
                     }
                 })
                 .filter(Objects::nonNull)
                 .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                 .limit(topN)
                 .forEach(entry -> largeFiles.add(entry.getKey() + " : " + entry.getValue() / (1024 * 1024) + " MB"));
        }
        return largeFiles;
    }

    // Find files that may hinder performance (logs, temp, backup, etc.)
    public static List<String> findHinderingFiles(String directoryPath) throws IOException {
        List<String> hinderingFiles = new ArrayList<>();
        String[] extensions = {".bak", ".log", ".tmp", ".swp", ".old", ".part"};
        
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                 .map(Path::toString)
                 .filter(path -> Arrays.stream(extensions).anyMatch(path::endsWith))
                 .forEach(hinderingFiles::add);
        }
        return hinderingFiles;
    }

    // Display the file management statistics (largest files and hindering files)
    public static void displayFileStats(String directoryPath) throws IOException {
        System.out.println("\n🖥️ Analyzing files in: " + directoryPath);
        
        // Largest files
        List<String> largestFiles = findLargestFiles(directoryPath, 5);
        if (!largestFiles.isEmpty()) {
            System.out.println("\n🔍 Largest Files:");
            largestFiles.forEach(System.out::println);
        }

        // Hindering files
        List<String> hinderingFiles = findHinderingFiles(directoryPath);
        if (!hinderingFiles.isEmpty()) {
            System.out.println("\n❌ Potential Performance Hindrances:");
            hinderingFiles.forEach(System.out::println);
        }
    }

    // Get GPU VRAM (This is a platform-specific GPU retrieval function)
    public static String getGpuStats() {
        String gpuInfo = "No GPU info available.";
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                // For Windows, use WMIC to get GPU info
                Process process = Runtime.getRuntime().exec("wmic path win32_videocontroller get caption, adapterram");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Caption")) continue;  // Skip header line
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        gpuInfo = "GPU: " + parts[0] + ", VRAM: " + (Long.parseLong(parts[1]) / (1024 * 1024)) + " MB";
                    }
                }
            } else if (os.contains("mac")) {
                // For macOS, use system_profiler
                Process process = Runtime.getRuntime().exec("system_profiler SPDisplaysDataType");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("VRAM")) {
                        gpuInfo = "GPU: macOS GPU, " + line.trim();
                    }
                }
            } else if (os.contains("linux")) {
                // For Linux, use lshw or nvidia-smi for NVIDIA GPUs
                Process process = Runtime.getRuntime().exec("lshw -C display");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("size") && line.contains("display")) {
                        gpuInfo = "GPU: " + line.trim();
                    }
                }
                // Or use nvidia-smi for NVIDIA
                process = Runtime.getRuntime().exec("nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits");
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String memory = reader.readLine();
                if (memory != null) {
                    gpuInfo = "GPU: NVIDIA, VRAM: " + memory + " MB";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gpuInfo;
    }

    // Main driver function
    public static void main(String[] args) throws IOException {
        System.out.println("📊 Welcome to stress-check");

        // System Stress Test
        printSystemStats();
        cpuStress(10, Runtime.getRuntime().availableProcessors());
        ramStress(10, 500);

        // File Stats
        String userDirectory = System.getProperty("user.home");
        displayFileStats(userDirectory);

        // GPU Stats
        System.out.println("\n🖥️ " + getGpuStats());

        System.out.println("✅ All tests finished.");
    }
}
