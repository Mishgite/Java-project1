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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewResults = findViewById(R.id.textViewResults);
        buttonRecognizeText = findViewById(R.id.buttonRecognizeText);
        buttonDetectObjects = findViewById(R.id.buttonDetectObjects);
        buttonCopyResult = findViewById(R.id.buttonCopyResult);
        previewView = findViewById(R.id.cameraPreview);

        // Initialize TextRecognizer
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        // Initialize ObjectDetector with default options
        ObjectDetectorOptions options =
                new ObjectDetectorOptions.Builder()
                        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                        .enableMultipleObjects()
                        .enableClassification() // Optional: classify objects
                        .build();
        objectDetector = ObjectDetection.getClient(options);

        // Camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Set up button listeners
        buttonRecognizeText.setOnClickListener(v -> recognizeText());
        buttonDetectObjects.setOnClickListener(v -> detectObjects());
        buttonCopyResult.setOnClickListener(v -> copyResultToClipboard());

        // Request camera permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);

        // Start the camera
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                ImageCapture imageCapture = new ImageCapture.Builder().build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                );

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void recognizeText() {
        if (selectedImage == null) {
            textViewResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(result -> {
                    String recognizedText = result.getText();
                    if (recognizedText.isEmpty()) {
                        textViewResults.setText("Текст не найден.");
                    } else {
                        textViewResults.setText(recognizedText);
                    }
                })
                .addOnFailureListener(e -> {
                    textViewResults.setText("Ошибка распознавания текста.");
                    Log.e("TextRecognition", "Ошибка: ", e);
                });
    }

    private void detectObjects() {
        if (selectedImage == null) {
            textViewResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);
        objectDetector.process(image)
                .addOnSuccessListener(results -> {
                    StringBuilder detectedObjects = new StringBuilder("Объекты:");
                    for (DetectedObject object : results) {
                        detectedObjects.append("\nОбъект с ID: ").append(object.getTrackingId());
                    }
                    textViewResults.setText(detectedObjects.toString());
                })
                .addOnFailureListener(e -> {
                    textViewResults.setText("Ошибка распознавания объектов.");
                    Log.e("ObjectDetection", "Ошибка: ", e);
                });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
