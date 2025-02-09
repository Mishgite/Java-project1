package com.example.java_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int EDIT_TEXT_REQUEST = 1;

    private Button buttonEditText;
    private ImageView imageView;
    private EditText editTextResults;
    private ProgressBar progressBar;
    private Button buttonSelectImage, buttonRecognizeText, buttonTakePhoto, buttonCopyText, buttonScanQR;
    private Bitmap selectedImage;

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        editTextResults = findViewById(R.id.editTextResults);
        progressBar = findViewById(R.id.progressBar);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonRecognizeText = findViewById(R.id.buttonRecognizeText);
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto);
        buttonCopyText = findViewById(R.id.buttonCopyText);
        buttonScanQR = findViewById(R.id.buttonScanQR);
        buttonEditText = findViewById(R.id.buttonEditText);

        buttonEditText.setOnClickListener(v -> openEditActivity());
        buttonSelectImage.setOnClickListener(v -> selectImage());
        buttonRecognizeText.setOnClickListener(v -> recognizeText());
        buttonTakePhoto.setOnClickListener(v -> takePhoto());
        buttonCopyText.setOnClickListener(v -> copyToClipboard());
        buttonScanQR.setOnClickListener(v -> scanQRCode());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
        Button buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        } else {
            Toast.makeText(this, "Камера недоступна на этом устройстве", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MainActivity", "onActivityResult called. RequestCode: " + requestCode + ", ResultCode: " + resultCode);

        if (requestCode == EDIT_TEXT_REQUEST && resultCode == RESULT_OK && data != null) {
            String editedText = data.getStringExtra("editedText");
            Log.d("MainActivity", "Отредактированный текст: " + editedText);

            if (editedText != null) {
                editTextResults.setText(editedText);
            }
        }

        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data == null || data.getData() == null) {
                Toast.makeText(this, "Ошибка: данные изображения отсутствуют!", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri imageUri = data.getData();
            Log.d("DEBUG", "Получен imageUri: " + imageUri);

            if (imageUri != null) {
                loadImageAsync(imageUri);
            } else {
                Toast.makeText(this, "Ошибка: imageUri пустой!", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == TAKE_PHOTO_REQUEST && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                selectedImage = (Bitmap) extras.get("data");
                imageView.setImageBitmap(selectedImage);
                enableButtons();
            }
        }
    }

    private void loadImageAsync(Uri imageUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                handler.post(() -> {
                    selectedImage = bitmap;
                    imageView.setImageBitmap(selectedImage);
                    enableButtons();
                });
            } catch (IOException e) {
                handler.post(() -> Toast.makeText(MainActivity.this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show());
            }
        });

        executor.shutdown();
    }


    private void enableButtons() {
        buttonRecognizeText.setEnabled(true);
        buttonScanQR.setEnabled(true);
    }

    private void recognizeText() {
        if (selectedImage == null) {
            editTextResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);

        TextRecognizer recognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        progressBar.setVisibility(View.VISIBLE);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    StringBuilder recognizedText = new StringBuilder();
                    for (Text.TextBlock block : result.getTextBlocks()) {
                        recognizedText.append(block.getText()).append("\n");
                    }
                    editTextResults.setText(recognizedText.length() > 0 ? recognizedText.toString() : "Текст не распознан.");
                })
                .addOnFailureListener(e -> editTextResults.setText("Ошибка: " + e.getMessage()))
                .addOnCompleteListener(task -> progressBar.setVisibility(View.GONE));
    }

    private void scanQRCode() {
        if (selectedImage == null) {
            editTextResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_QR_CODE
                )
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        progressBar.setVisibility(View.VISIBLE);
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {
                        editTextResults.setText("Штрих-код или QR-код не найден.");
                    } else {
                        StringBuilder barcodeResult = new StringBuilder("Результаты:\n");
                        for (Barcode barcode : barcodes) {
                            barcodeResult.append(barcode.getRawValue()).append("\n");
                        }
                        editTextResults.setText(barcodeResult.toString());
                    }
                })
                .addOnFailureListener(e -> editTextResults.setText("Ошибка сканирования: " + e.getMessage()))
                .addOnCompleteListener(task -> progressBar.setVisibility(View.GONE));
    }

    private void copyToClipboard() {
        String textToCopy = editTextResults.getText().toString();
        if (!textToCopy.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Recognized Text", textToCopy);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Нет текста для копирования", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEditActivity() {
        String recognizedText = editTextResults.getText().toString();
        Intent intent = new Intent(MainActivity.this, EditActivity.class);
        intent.putExtra("recognizedText", recognizedText);
        startActivityForResult(intent, EDIT_TEXT_REQUEST);
    }
}
