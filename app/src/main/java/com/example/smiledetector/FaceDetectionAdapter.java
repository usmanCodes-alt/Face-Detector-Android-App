package com.example.smiledetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FaceDetectionAdapter extends RecyclerView.Adapter<FaceDetectionAdapter.ViewHolder> {

    private List<FaceDetectionModel> faceDetectionModelList;

    public FaceDetectionAdapter(List<FaceDetectionModel> faceDetectionModelList) {
        this.faceDetectionModelList = faceDetectionModelList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_face_detection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FaceDetectionModel faceDetectionModel = faceDetectionModelList.get(position);
        holder.firstFaceDetectionTextView.setText(String.valueOf(faceDetectionModel.getId()));
        holder.secondFaceDetectionTextView.setText(faceDetectionModel.getText());
    }

    @Override
    public int getItemCount() {
        return faceDetectionModelList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView firstFaceDetectionTextView;
        TextView secondFaceDetectionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            firstFaceDetectionTextView = itemView.findViewById(R.id.item_face_detection_text_view_1);
            secondFaceDetectionTextView = itemView.findViewById(R.id.item_face_detection_text_view_2);
        }
    }
}
