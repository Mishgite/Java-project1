package com.example.java_project;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textViewResults;
    private Button buttonRecognizeText, buttonDetectObjects, buttonCopyResult;
    private PreviewView previewView;

    private Bitmap selectedImage;
    private TextRecognizer textRecognizer;
    private ObjectDetector objectDetector;

    private ExecutorService cameraExecutor;
    private View cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);

        cameraExecutor = Executors.newSingleThreadExecutor();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider((Preview.SurfaceProvider) cameraPreview.getOutlineProvider());
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Ошибка запуска камеры", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    private void recognizeText() throws ExecutionException, InterruptedException {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            @SuppressWarnings("UnsafeOptInUsageError")
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees());

            textRecognizer.process(image)
                    .addOnSuccessListener(result -> {
                        runOnUiThread(() -> {
                            String recognizedText = result.getText();
                            if (!recognizedText.isEmpty()) {
                                textViewResults.setText(recognizedText);
                            } else {
                                textViewResults.setText("Текст не найден.");
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> textViewResults.setText("Ошибка распознавания текста."));
                        Log.e("TextRecognition", "Ошибка распознавания текста", e);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        });

        ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
        cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis);
    }


    private void detectObjects() throws ExecutionException, InterruptedException {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            @SuppressWarnings("UnsafeOptInUsageError")
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees());

            objectDetector.process(image)
                    .addOnSuccessListener(results -> {
                        runOnUiThread(() -> {
                            StringBuilder detectedObjects = new StringBuilder("Обнаруженные объекты:\n");
                            for (DetectedObject object : results) {
                                for (DetectedObject.Label label : object.getLabels()) {
                                    detectedObjects.append(label.getText())
                                            .append(" (")
                                            .append(label.getConfidence())
                                            .append(")\n");
                                }
                            }
                            textViewResults.setText(detectedObjects.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> textViewResults.setText("Ошибка распознавания объектов."));
                        Log.e("ObjectDetection", "Ошибка распознавания объектов", e);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        });

        ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
        cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis);
    }


    private void copyResultToClipboard() {
        String resultText = textViewResults.getText().toString();
        if (!resultText.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Результат", resultText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Результат скопирован.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Нет текста для копирования.", Toast.LENGTH_SHORT).show();
        }
    }

}
