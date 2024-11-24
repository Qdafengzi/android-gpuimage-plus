package org.wysaid.cgeDemo;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.wysaid.cgeDemo.camera.CameraFragment;
import org.wysaid.cgeDemo.databinding.CameraDemoBinding;

public class CameraDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraDemoBinding binding = CameraDemoBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        getSupportFragmentManager().beginTransaction().add(binding.fragmentContainer.getId(), new CameraFragment()).commitNow();
    }
}
