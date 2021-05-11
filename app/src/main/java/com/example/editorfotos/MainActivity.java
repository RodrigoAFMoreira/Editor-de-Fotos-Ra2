package com.example.editorfotos;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inic();
    }

    private static final int REQUEST_PERMISSIONS = 333;
    private static final String[] PERMISSIONS ={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PERMISSIONS_COUNT = 2;

    @SuppressLint("NewApi")
    private boolean semPermissao(){
        for(int i = 0; i < PERMISSIONS_COUNT ; i++){
            if(checkSelfPermission(PERMISSIONS[i])!= PackageManager.PERMISSION_GRANTED) {
            return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && semPermissao()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if(semPermissao()){
                ((ActivityManager)this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                recreate();
            }
        }
    }
    private static final int REQUEST_PICK_IMAGE = 4444;

    private ImageView imageView;

    private void inic(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        imageView = findViewById(R.id.imageView);

        if(!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
                findViewById(R.id.fotografarButton).setVisibility(View.GONE);
        }
        final Button selecioneImgButton = findViewById(R.id.selecioneImgButton);
        selecioneImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                final Intent pickIntent = new Intent(Intent.ACTION_PICK);
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
                final Intent chooserIntent = Intent.createChooser(intent,"Selecione Imagem");
                startActivityForResult(chooserIntent, REQUEST_PICK_IMAGE);
            }
        });
        final Button fotografarButton = findViewById(R.id.fotografarButton);
        fotografarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent tirarFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(tirarFotoIntent.resolveActivity(getPackageManager()) != null){
                    //criar um arquivo da foto que foi tirada
                    final File aqvFoto = criarImgArquivo();
                    imageUri = Uri.fromFile(aqvFoto);
                    final SharedPreferences minhasPref = getSharedPreferences(appID,0);
                    minhasPref.edit().putString("path",aqvFoto.getAbsolutePath()).apply();
                    tirarFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(tirarFotoIntent, REQUEST_IMAGE_CAPTURE);
                }else{
                    Toast.makeText(MainActivity.this,"A sua camera não é compativel",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        final Button filtroFotoButton = findViewById(R.id.filtroFoto);
        filtroFotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    @Override
                    public void run() {
                        for(int i =0; i<getMaxPixelCount; i++){
                            pixels[i] /=2;
                        }
                        bitmap.setPixels(pixels,0,width,0,0,width,height);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                }.start();
            }
        });
    }
    private static final int REQUEST_IMAGE_CAPTURE = 55555;
    private static final String appID = "editorDeFotos";
    private Uri imageUri;

    private File criarImgArquivo(){
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        final String nomeArqImg = "/JPEG_" + timeStamp +".jpg";
        final File dirArmazenamento = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(dirArmazenamento+ nomeArqImg);
    }

    private boolean editMode = false;
    private Bitmap bitmap;
    private int width = 0;
    private int height = 0;
    private static final int MAX_PIXEL_COUNT = 2048;

    private int[] pixels;
    private int getMaxPixelCount = 0;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != RESULT_OK){
            return;
        }
        if(requestCode == REQUEST_IMAGE_CAPTURE){
            if(imageUri == null){
                final SharedPreferences p = getSharedPreferences(appID, 0);
                final String path = p.getString("path","");
                if(path.length() < 1){
                    recreate();
                    return;
                }
                imageUri = Uri.parse("file://"+ path);
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imageUri));
        }else if(data ==null){
            recreate();
            return;
        }else if(requestCode == REQUEST_PICK_IMAGE){
            imageUri = data.getData();
        }
        final ProgressDialog dialog = ProgressDialog.show(MainActivity.this,"Carregando", "Espere...", true);
        //new interface
        editMode = true;

        findViewById(R.id.telaPrincipal).setVisibility(View.GONE);
        findViewById(R.id.telaEdicao).setVisibility(View.VISIBLE);

        new Thread(){
            public void run() {
                bitmap = null;
                final BitmapFactory.Options bmpOptions = new BitmapFactory.Options();
                bmpOptions.inBitmap = bitmap;
                bmpOptions.inJustDecodeBounds = true;
                try (InputStream intput = getContentResolver().openInputStream(imageUri)) {
                    bitmap = BitmapFactory.decodeStream(intput, null, bmpOptions);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds = false;
                width = bmpOptions.outWidth;
                height = bmpOptions.outHeight;
                int resizeScale = 2;
                if (width > MAX_PIXEL_COUNT) {
                    resizeScale = width / MAX_PIXEL_COUNT;
                } else if (height > MAX_PIXEL_COUNT) {
                    resizeScale = height / MAX_PIXEL_COUNT;
                }
                if (width / resizeScale > MAX_PIXEL_COUNT || height / resizeScale > MAX_PIXEL_COUNT) {
                    resizeScale++;
                }
                bmpOptions.inSampleSize = resizeScale;
                InputStream input = null;
                try {
                    input = getContentResolver().openInputStream(imageUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    recreate();
                    return;
                }
                bitmap = BitmapFactory.decodeStream(input, null, bmpOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        dialog.cancel();
                    }
                });
            }
        }.start();
    }
}