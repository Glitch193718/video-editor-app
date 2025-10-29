package com.fimozik.stretcher;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import com.arthenica.mobileffmpeg.FFmpeg;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RealVideoProcessor {
    
    public interface ProcessingCallback {
        void onProgress(int progress, String message);
        void onSuccess(String outputPath);
        void onError(String error);
    }
    
    private final Context context;
    
    public RealVideoProcessor(Context context) {
        this.context = context;
    }
    
    public void processVideo(Uri inputUri, String outputFormat, int quality, ProcessingCallback callback) {
        new Thread(() -> {
            try {
                // Копируем видео в временную папку
                String inputPath = copyVideoToTemp(inputUri);
                if (inputPath == null) {
                    callback.onError("Не удалось скопировать видео");
                    return;
                }
                
                // Создаем выходной файл
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String outputFileName = "enhanced_" + timeStamp + ".mp4";
                File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), outputFileName);
                
                // Строим команду FFmpeg
                String command = buildFFmpegCommand(inputPath, outputFile.getAbsolutePath(), outputFormat, quality);
                
                callback.onProgress(10, "Запуск обработки...");
                
                // Запускаем FFmpeg
                int result = FFmpeg.execute(command);
                
                if (result == 0) {
                    callback.onProgress(100, "Обработка завершена!");
                    callback.onSuccess(outputFile.getAbsolutePath());
                } else {
                    callback.onError("Ошибка FFmpeg: " + result);
                }
                
                // Удаляем временный файл
                new File(inputPath).delete();
                
            } catch (Exception e) {
                callback.onError("Ошибка: " + e.getMessage());
            }
        }).start();
        
        // Симуляция прогресса (так как FFmpeg не отдает прогресс в AIDE)
        simulateProgress(callback);
    }
    
    private String copyVideoToTemp(Uri uri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String tempFileName = "temp_video_" + timeStamp + ".mp4";
            File tempFile = new File(context.getExternalFilesDir(null), tempFileName);
            
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
            
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String buildFFmpegCommand(String inputPath, String outputPath, String format, int quality) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("-y -i ").append(inputPath).append(" ");
        
        // Настройки качества
        switch (quality) {
            case 0: // Без сжатия
                cmd.append("-c:v libx264 -crf 18 -preset medium ");
                break;
            case 1: // С сжатием
                cmd.append("-c:v libx264 -crf 23 -preset fast ");
                break;
            case 2: // AI улучшение
                cmd.append("-c:v libx264 -crf 20 -preset medium ");
                cmd.append("-r 30 "); // Увеличиваем FPS
                break;
        }
        
        // Аудио
        cmd.append("-c:a aac -b:a 128k ");
        
        // Формат вывода
        switch (format) {
            case "16:9":
                cmd.append("-vf \"scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2\" ");
                break;
            case "21:9":
                cmd.append("-vf \"scale=1920:800:force_original_aspect_ratio=decrease,pad=1920:800:(ow-iw)/2:(oh-ih)/2\" ");
                break;
            case "1:1":
                cmd.append("-vf \"scale=720:720:force_original_aspect_ratio=decrease,pad=720:720:(ow-iw)/2:(oh-ih)/2\" ");
                break;
            case "9:16":
                cmd.append("-vf \"scale=720:1280:force_original_aspect_ratio=decrease,pad=720:1280:(ow-iw)/2:(oh-ih)/2\" ");
                break;
        }
        
        // AI улучшения (упрощенные)
        if (quality == 2) {
            cmd.append("-vf \"scale=1280:720:flags=lanczos,unsharp=3:3:0.5\" ");
        }
        
        cmd.append("\"").append(outputPath).append("\"");
        return cmd.toString();
    }
    
    private void simulateProgress(ProcessingCallback callback) {
        new Thread(() -> {
            try {
                String[] steps = {
                    "Анализ видео...",
                    "Подготовка формата...",
                    "Обработка качества...", 
                    "Применение фильтров...",
                    "Кодирование видео...",
                    "Сохранение..."
                };
                
                for (int i = 0; i < steps.length; i++) {
                    Thread.sleep(3000);
                    int progress = 15 + (i * 14);
                    callback.onProgress(Math.min(progress, 85), steps[i]);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
