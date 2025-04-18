package com.example.java_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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
    private static final int SPEECH_REQUEST_CODE = 200;

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
        Button buttonVoiceInput = findViewById(R.id.buttonVoiceInput);
        buttonVoiceInput.setOnClickListener(v -> startVoiceInput());
        buttonEditText.setOnClickListener(v -> openEditActivity());
        buttonSelectImage.setOnClickListener(v -> selectImage());
        buttonRecognizeText.setOnClickListener(v -> recognizeText());
        buttonTakePhoto.setOnClickListener(v -> takePhoto());
        buttonCopyText.setOnClickListener(v -> copyToClipboard());
        buttonScanQR.setOnClickListener(v -> scanQRCode());
        Button buttonSearchGoogle = findViewById(R.id.buttonSearchGoogle);
        buttonSearchGoogle.setOnClickListener(v -> searchInGoogle());

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
        Button buttonHistory = findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivityForResult(intent, 123); // произвольный код
        });

    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }
    private void searchInGoogle() {
        String query = editTextResults.getText().toString().trim();
        if (!query.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Введите текст для поиска", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, TAKE_PHOTO_REQUEST);
        } else {
            Toast.makeText(this, "Камера недоступна на этом устройстве", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToHistory(String text) {
        if (text.isEmpty()) return;

        HistoryDatabaseHelper dbHelper = new HistoryDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("text", text);
        values.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));

        db.insert("history", null, values);
        db.close();
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
                saveToHistory(editedText);
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
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String recognizedText = result.get(0);
                editTextResults.setText(recognizedText);
            }
        }
        if (requestCode == 123 && resultCode == RESULT_OK && data != null) {
            String action = data.getStringExtra("action");
            String historyText = data.getStringExtra("text");
            if (historyText != null) {
                if ("replace".equals(action)) {
                    editTextResults.setText(historyText);
                } else if ("insert".equals(action)) {
                    editTextResults.append("\n" + historyText);
                }
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

                    String text = recognizedText.toString();
                    editTextResults.setText(!text.isEmpty() ? text : "Текст не распознан.");

                    if (!text.isEmpty()) {
                        saveToHistory(text); // <<< Вставка сохранения в БД
                    }
                })
                .addOnFailureListener(e -> editTextResults.setText("Ошибка: " + e.getMessage()))
                .addOnCompleteListener(task -> progressBar.setVisibility(View.GONE));
    }
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU"); // Русский язык
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Распознавание речи не поддерживается", Toast.LENGTH_SHORT).show();
        }
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
