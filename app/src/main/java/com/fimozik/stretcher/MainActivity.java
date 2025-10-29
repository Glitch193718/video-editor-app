package com.fimozik.stretcher;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements RealVideoProcessor.ProcessingCallback {
    
    private Button enhanceButton;
    private LinearLayout selectVideoCard;
    private Spinner qualitySpinner;
    private RadioGroup formatGroup;
    private LinearLayout progressContainer;
    private TextView progressText;
    private ProgressBar progressBar;
    private TextView selectedVideoPath;
    
    private Uri selectedVideoUri;
    private RealVideoProcessor videoProcessor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupVideoProcessor();
        setupAnimations();
        setupQualitySpinner();
        setupButtonInteractions();
    }
    
    private void setupVideoProcessor() {
        videoProcessor = new RealVideoProcessor(this);
    }
    
    private void initializeViews() {
        enhanceButton = findViewById(R.id.enhanceButton);
        selectVideoCard = findViewById(R.id.selectVideoCard);
        qualitySpinner = findViewById(R.id.qualitySpinner);
        formatGroup = findViewById(R.id.formatGroup);
        progressContainer = findViewById(R.id.progressContainer);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        selectedVideoPath = findViewById(R.id.selectedVideoPath);
        
        progressContainer.setVisibility(View.GONE);
        selectedVideoPath.setVisibility(View.GONE);
    }
    
    private void setupQualitySpinner() {
        String[] qualities = {
            "🎯 Без сжатия (макс. качество)",
            "⚡ С сжатием (баланс)", 
            "🤖 AI улучшение (4K 120FPS)"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, qualities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qualitySpinner.setAdapter(adapter);
        
        qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showQualityTooltip(position);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void showQualityTooltip(int position) {
        String[] messages = {
            "Исходное качество сохраняется, большой размер файла",
            "Оптимальное качество при уменьшенном размере",
            "AI улучшает качество до 4K и добавляет кадры до 120FPS"
        };
        Toast.makeText(this, messages[position], Toast.LENGTH_LONG).show();
    }
    
    private void setupButtonInteractions() {
        selectVideoCard.setOnClickListener(v -> selectVideoFile());
        
        enhanceButton.setOnClickListener(v -> {
            if (selectedVideoUri != null) {
                startRealEnhancementProcess();
            } else {
                showMessage("🎬 Сначала выберите видео!");
                selectVideoFile();
            }
        });
        
        findViewById(R.id.telegramChannel).setOnClickListener(v -> openTelegramChannel());
        findViewById(R.id.contactText).setOnClickListener(v -> openTelegramContact());
    }
    
    private void selectVideoFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите видео"), 100);
    }
    
    private void startRealEnhancementProcess() {
        int quality = qualitySpinner.getSelectedItemPosition();
        
        String format = "16:9";
        int checkedId = formatGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.format21_9) format = "21:9";
        else if (checkedId == R.id.format1_1) format = "1:1";
        else if (checkedId == R.id.format9_16) format = "9:16";
        
        showProgress();
        videoProcessor.processVideo(selectedVideoUri, format, quality, this);
    }
    
    private void showProgress() {
        progressContainer.setVisibility(View.VISIBLE);
        progressContainer.setAlpha(0f);
        progressContainer.animate()
                .alpha(1f)
                .setDuration(500)
                .start();
        
        enhanceButton.setEnabled(false);
        progressBar.setProgress(0);
    }
    
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    private void openTelegramChannel() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://t.me/Script_Releases"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Установите Telegram", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openTelegramContact() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://t.me/glitch_qzq"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Установите Telegram", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupAnimations() {
        View[] viewsToAnimate = {
            findViewById(R.id.appTitle),
            findViewById(R.id.headerCard),
            selectVideoCard,
            findViewById(R.id.qualityCard),
            findViewById(R.id.formatCard),
            enhanceButton
        };
        
        for (int i = 0; i < viewsToAnimate.length; i++) {
            View view = viewsToAnimate[i];
            view.setAlpha(0f);
            view.setTranslationY(50f);
            
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(100L * i)
                    .setDuration(600)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.getData();
            showMessage("✅ Видео выбрано! Теперь настройте параметры.");
            animateVideoSelected();
            
            String fileName = getFileName(selectedVideoUri);
            selectedVideoPath.setText("📹 Выбрано: " + fileName);
            selectedVideoPath.setVisibility(View.VISIBLE);
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"));
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "video.mp4";
    }
    
    private void animateVideoSelected() {
        ObjectAnimator bounceAnim = ObjectAnimator.ofFloat(selectVideoCard, "scaleX", 1f, 1.05f, 1f);
        bounceAnim.setDuration(600);
        bounceAnim.setInterpolator(new BounceInterpolator());
        bounceAnim.start();
    }
    
    @Override
    public void onProgress(int progress, String message) {
        runOnUiThread(() -> {
            progressBar.setProgress(progress);
            progressText.setText(message);
        });
    }
    
    @Override
    public void onSuccess(String outputPath) {
        runOnUiThread(() -> {
            progressText.setText("✅ Видео успешно обработано!");
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                enhanceButton.setEnabled(true);
                progressContainer.setVisibility(View.GONE);
                String fileName = outputPath.substring(outputPath.lastIndexOf("/") + 1);
                showMessage("🎉 Видео готово! Файл: " + fileName);
            }, 2000);
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            progressText.setText("❌ Ошибка: " + error);
            enhanceButton.setEnabled(true);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                progressContainer.setVisibility(View.GONE);
                showMessage("😞 Не удалось обработать видео");
            }, 2000);
        });
    }
                 }
