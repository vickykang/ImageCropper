package cn.dream.imagecropper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import cn.dream.cropperimageview.cropper.ImageCropperView;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private ImageCropperView mCropperView;
    private ImageView mImageView;
    private Button mOkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOkButton = (Button) findViewById(R.id.btn_ok);
        mCropperView = (ImageCropperView) findViewById(R.id.cropper);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mImageView.setVisibility(View.GONE);
        mOkButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_ok) {
            mOkButton.setVisibility(View.GONE);
            Bitmap croppedBmp = mCropperView.getCroppedBitmap();

            mCropperView.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);

            if (croppedBmp != null) {
                mImageView.setImageBitmap(croppedBmp);
            }
        }
    }
}
