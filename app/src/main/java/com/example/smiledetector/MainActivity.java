package com.example.smiledetector;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceLandmark;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Facing;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements FrameProcessor {

    private Facing cameraFacing = Facing.FRONT;
    private Button toggleButton;
    private ImageView imageView;
    private CameraView faceDetectionCameraView;
    private RecyclerView bottomSheetRecyclerView;
    private ArrayList<FaceDetectionModel> faceDetectionModels;
    private BottomSheetBehavior bottomSheetBehavior;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FrameLayout bottomSheetButton = findViewById(R.id.bottom_sheet_button);
        faceDetectionModels = new ArrayList<>();
        imageView = findViewById(R.id.face_detection_image_view);

        faceDetectionCameraView = findViewById(R.id.face_detection_camera_view);
        bottomSheetRecyclerView = findViewById(R.id.bottom_sheet_recycler_view);
        toggleButton = findViewById(R.id.face_detection_camera_toggle_button);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait.");


        // setting up our camera view.
        faceDetectionCameraView.setFacing(cameraFacing);
        faceDetectionCameraView.setLifecycleOwner(this);
        faceDetectionCameraView.addFrameProcessor(this);

        bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        FaceDetectionAdapter adapter = new FaceDetectionAdapter(faceDetectionModels);
        bottomSheetRecyclerView.setAdapter(adapter);
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        toggleButton.setOnClickListener(v -> {
            cameraFacing = (cameraFacing == Facing.FRONT ? Facing.BACK : Facing.FRONT);
            faceDetectionCameraView.setFacing(cameraFacing);
        });

        bottomSheetButton.setOnClickListener(v -> CropImage.activity().start(MainActivity.this));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert result != null;
                Uri imageUri = result.getUri();
                try {
                    analyseImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void analyseImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Error Occurred!", Toast.LENGTH_SHORT).show();
            return;
        }
        imageView.setImageBitmap(null);
        faceDetectionModels.clear();
        Objects.requireNonNull(bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        showProgress();

        // Create image from bitmap (i.e. Image from device)
        InputImage firebaseVisionImage = InputImage.fromBitmap(bitmap, 0);

        // Configure options, i.e. What do you want to be detected in the image (this is optional)
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        // Pass the options to the detector.
        FaceDetector detector = FaceDetection.getClient(options);
        // Invoke face detector.
        detector.process(firebaseVisionImage)
            .addOnSuccessListener(firebaseVisionFaces -> {

                Bitmap mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                detectFaces(firebaseVisionFaces, mutableImage);

                imageView.setImageBitmap(mutableImage);
                Log.d("Bitmap set!", "analyseImage: image view bitmap set");

                progressDialog.dismiss();
                bottomSheetRecyclerView.getAdapter().notifyDataSetChanged();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                Log.d("Failure!", "onFailure: " + e.getMessage());
                progressDialog.dismiss();
            });
    }

    private void detectFaces(List<Face> firebaseVisionFaces, Bitmap mutableImage) {
        if (firebaseVisionFaces == null || mutableImage == null) {
            Toast.makeText(this, "Error Occurred_1", Toast.LENGTH_SHORT).show();
            return;
        }
        Canvas canvas = new Canvas(mutableImage);

        Paint facePaint = new Paint();
        facePaint.setColor(Color.GREEN);
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(5f);

        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.BLUE);
        faceTextPaint.setTextSize(30f);
        faceTextPaint.setTypeface(Typeface.SANS_SERIF);

        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(8f);

        for(int counter = 0; counter < firebaseVisionFaces.size(); counter++) {
            canvas.drawRect(firebaseVisionFaces.get(counter).getBoundingBox(), facePaint);
            canvas.drawText("Face " + counter,
                    (firebaseVisionFaces.get(counter).getBoundingBox().centerX()
                    - (firebaseVisionFaces.get(counter).getBoundingBox().width() >> 1) + 8f),
                    (firebaseVisionFaces.get(counter).getBoundingBox().centerY()
                    - (firebaseVisionFaces.get(counter).getBoundingBox().height() >> 1)) - 8f, facePaint);

            Face face = firebaseVisionFaces.get(counter);

            if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) {
                FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                assert leftEye != null;
                canvas.drawCircle(
                        leftEye.getPosition().x,
                        leftEye.getPosition().y,
                        8f,
                        landmarkPaint
                );
            }

            if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
                FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                assert rightEye != null;
                canvas.drawCircle(
                        rightEye.getPosition().x,
                        rightEye.getPosition().y,
                        8f,
                        landmarkPaint
                );
            }

            if (face.getLandmark(FaceLandmark.NOSE_BASE) != null) {
                FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
                assert nose != null;
                canvas.drawCircle(
                        nose.getPosition().x,
                        nose.getPosition().y,
                        8f,
                        landmarkPaint
                );
            }

            if (face.getLandmark(FaceLandmark.LEFT_EAR) != null) {
                FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                assert leftEar != null;
                canvas.drawCircle(
                        leftEar.getPosition().x,
                        leftEar.getPosition().y,
                        8f,
                        landmarkPaint
                );
            }

            if (face.getLandmark(FaceLandmark.RIGHT_EAR) != null) {
                FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
                assert rightEar != null;
                canvas.drawCircle(
                        rightEar.getPosition().x,
                        rightEar.getPosition().y,
                        8f,
                        landmarkPaint
                );
            }

            if (face.getLandmark(FaceLandmark.MOUTH_LEFT) != null
                && face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null
                && face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null) {

                FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
                FaceLandmark mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
                FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);

                assert mouthLeft != null;
                assert mouthBottom != null;
                canvas.drawLine(
                        mouthLeft.getPosition().x,
                        mouthLeft.getPosition().y,
                        mouthBottom.getPosition().x,
                        mouthBottom.getPosition().y,
                        landmarkPaint
                );

                assert mouthRight != null;
                canvas.drawLine(
                        mouthBottom.getPosition().x,
                        mouthBottom.getPosition().y,
                        mouthRight.getPosition().x,
                        mouthRight.getPosition().y,
                        landmarkPaint
                );

                faceDetectionModels.add(new FaceDetectionModel(counter, "Smiling Probability: " +
                        face.getSmilingProbability())); // 0.99 * 100 = %
                faceDetectionModels.add(new FaceDetectionModel(counter, "Left Eye Open Probability: " +
                        face.getLeftEyeOpenProbability()));
                faceDetectionModels.add(new FaceDetectionModel(counter, "Right Eye Open Probability: " +
                        face.getRightEyeOpenProbability()));
            }
        }
    }

    private void showProgress() {
        progressDialog.show();
    }

    @Override
    public void process(@NonNull Frame frame) {
        faceDetectionCameraView.setVisibility(View.INVISIBLE);
    }
}