package com.example.java_project;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;
    private static final int TAKE_PHOTO_REQUEST = 2;

    private ImageView imageView;
    private TextView textViewResults;
    private Button buttonSelectImage, buttonRecognizeText, buttonTakePhoto;
    private Bitmap selectedImage;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewResults = findViewById(R.id.textViewResults);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonRecognizeText = findViewById(R.id.buttonRecognizeText);
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto);

        buttonSelectImage.setOnClickListener(v -> selectImage());

        buttonRecognizeText.setOnClickListener(v -> recognizeText());

        buttonTakePhoto.setOnClickListener(v -> takePhoto());
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на камеру предоставлено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Разрешение на камеру не предоставлено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
                buttonRecognizeText.setEnabled(true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == TAKE_PHOTO_REQUEST && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            selectedImage = (Bitmap) extras.get("data");
            imageView.setImageBitmap(selectedImage);
            buttonRecognizeText.setEnabled(true);
        }
    }

    private void recognizeText() {
        if (selectedImage == null) {
            textViewResults.setText("Пожалуйста, выберите изображение.");
            return;
        }

        InputImage image = InputImage.fromBitmap(selectedImage, 0);

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this::displayRecognizedText)
                .addOnFailureListener(e -> textViewResults.setText("Ошибка распознавания: " + e.getMessage()));
    }

    private void displayRecognizedText(Text result) {
        StringBuilder recognizedText = new StringBuilder();

        for (Text.TextBlock block : result.getTextBlocks()) {
            recognizedText.append(block.getText()).append("\n");
        }

        if (recognizedText.length() > 0) {
            textViewResults.setText(recognizedText.toString());
        } else {
            textViewResults.setText("Текст не распознан.");
        }
    }
}
