package com.djavid.dentistassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.djavid.dentistassistant.Geometry.Geometry;
import com.djavid.dentistassistant.Geometry.LineSegment;
import com.djavid.dentistassistant.Geometry.Point;
import com.frosquivel.magicalcamera.MagicalCamera;
import com.frosquivel.magicalcamera.MagicalPermissions;

import java.util.HashMap;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    @BindView(R.id.iv_image)
    ImageView photoView;
    @BindView(R.id.alert)
    TextView alert;
    @BindView(R.id.btn_rotate)
    ImageButton btn_rotate;
    @BindView(R.id.rl_photo)
    RelativeLayout rl_photo;
    @BindView(R.id.btn_undo)
    ImageButton btn_undo;
    @BindView(R.id.btn_reset)
    ImageButton btn_reset;
    @BindView(R.id.btn_calc_edges)
    ImageButton btn_calc_edges;


    private int RESIZE_PHOTO_PIXELS_PERCENTAGE = 80;
    private MagicalPermissions magicalPermissions;
    private MagicalCamera magicalCamera;

    Bitmap bitmap;
    Bitmap bitmapRaw;
    Bitmap bitmapBackup;
    Path path;
    HashMap <Integer, Path> pathMap;
    HashMap <Integer, LineSegment> lineCoordsMap;
    int lastPathId;
    Canvas canvas;
    Paint paint;
    Paint paintAlt;
    float downx = 0, downy = 0, upx = 0, upy = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorToolbarTitle));
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> addImageFromGallery());

        btn_rotate.setOnClickListener(this::buttonRotate);
        btn_undo.setOnClickListener(this::buttonUndo);
        btn_reset.setOnClickListener(this::buttonReset);
        btn_calc_edges.setOnClickListener(this::buttonCalcEdges);


        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        magicalPermissions = new MagicalPermissions(this, permissions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_camera) {
            addImageFromCamera();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        magicalCamera.resultPhoto(requestCode, resultCode, data);
        magicalCamera.resultPhoto(requestCode, resultCode, data, MagicalCamera.ORIENTATION_ROTATE_270);
        bitmap = magicalCamera.getPhoto();

        if (bitmap != null) {
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(20);

            paintAlt = new Paint();
            paintAlt.setColor(Color.WHITE);
            paintAlt.setAntiAlias(true);
            paintAlt.setDither(true);
            paintAlt.setStyle(Paint.Style.FILL_AND_STROKE);
            paintAlt.setStrokeJoin(Paint.Join.ROUND);
            paintAlt.setStrokeCap(Paint.Cap.ROUND);
            paintAlt.setTextSize(100);
            paintAlt.setStrokeWidth(5);
            paintAlt.setShadowLayer(10, 10, 10, Color.GRAY);

            pathMap = new HashMap<>();
            lineCoordsMap = new HashMap<>();
            lastPathId = -1;

            bitmapRaw = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            canvas = new Canvas(bitmap);
            photoView.setImageBitmap(bitmap);
            photoView.setOnTouchListener(this);

            showImage();
        }
    }

    private void addImageFromGallery() {
        magicalCamera = new MagicalCamera(this, RESIZE_PHOTO_PIXELS_PERCENTAGE, magicalPermissions);
        magicalCamera.selectedPicture("Выберите фотографию");
    }

    private void addImageFromCamera() {
        magicalCamera = new MagicalCamera(this, RESIZE_PHOTO_PIXELS_PERCENTAGE, magicalPermissions);
        magicalCamera.takePhoto();
    }

    private void showImage() {
        alert.setVisibility(View.GONE);
        rl_photo.setVisibility(View.VISIBLE);
    }

    private void hideImage() {
        alert.setVisibility(View.VISIBLE);
        rl_photo.setVisibility(View.GONE);
        photoView.setRotation(0);
    }

    private void buttonRotate(View v) {
        magicalCamera.setPhoto(magicalCamera.rotatePicture(bitmapRaw,
                MagicalCamera.ORIENTATION_ROTATE_90));
        bitmap = magicalCamera.getPhoto();
        bitmapRaw = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        photoView.setImageBitmap(bitmap);
        canvas = new Canvas(bitmap);
        pathMap.clear();
        lineCoordsMap.clear();
    }

    private void buttonUndo(View v) {
        if (bitmapBackup != null) {
            canvas.drawBitmap(bitmapBackup, 0, 0, null);
            pathMap.remove(lastPathId);
            lineCoordsMap.remove(lastPathId);
            lastPathId--;
            photoView.invalidate();
        }
    }

    private void buttonReset(View v) {
        canvas.drawBitmap(bitmapRaw, 0, 0, null);
        pathMap.clear();
        lineCoordsMap.clear();
        photoView.invalidate();
    }

    private void buttonCalcEdges(View v) {
        LineSegment[] lineSegments = new LineSegment[lineCoordsMap.size()];

        int j = 0;
        for (Integer i : lineCoordsMap.keySet()) {
            System.out.println(lineCoordsMap.get(i).toString());
            lineSegments[j++] = lineCoordsMap.get(i);
        }

        Set<LineSegment[]> pairs = Geometry.getAllIntersectingLinesByBruteForce(lineSegments);
        for (LineSegment[] pair : pairs) {
            Point intersection = Geometry.getLinesIntersection(pair[0], pair[1]);
            if (intersection != null) {
                double angle = Geometry.getAngleBetweenLines(pair[0], pair[1]);

                canvas.drawPoint((float) intersection.getX(), (float) intersection.getY(), paintAlt);
                canvas.drawText(Float.toString((float)angle), (float)intersection.getX(),
                        (float)intersection.getY(), paintAlt);
                photoView.invalidate();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        magicalPermissions.permissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (rl_photo.getVisibility() == View.VISIBLE)
            hideImage();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int action = event.getAction();

        int[] position = getBitmapPositionInsideImageView(photoView); //[left,top] [right,bottom]
        System.out.println("position " + position[0] + " " + position[1] + " " + position[2] + " " + position[3]);
        System.out.println("canvas " + canvas.getWidth() + " " + canvas.getHeight());
        float scaleX = (float) canvas.getWidth() / (position[2]);
        float scaleY = (float) canvas.getHeight() / (position[3]);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                System.out.println("event " + event.getX() + " " + event.getY());
                if (event.getX() >= position[0] && event.getX() <= (position[0] + position[2])
                        && event.getY() >= position[1] && event.getY() <= (position[1] + position[3])) {
                    downx = (event.getX() - position[0]) * scaleX;
                    downy = (event.getY() - position[1]) * scaleY;

                    bitmapBackup = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                    path = new Path();
                    path.moveTo(downx, downy);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (downx != 0 && downy != 0) {

                    upx = (event.getX() - position[0]) * scaleX;
                    upy = (event.getY() - position[1]) * scaleY;

                    path.lineTo(upx, upy);
                    canvas.drawPath(path, paint);
                    photoView.invalidate();
                }

                break;

            case MotionEvent.ACTION_UP:
                if (downx != 0 && downy != 0) {
                    if (event.getX() >= position[0] && event.getX() <= (position[0] + position[2])
                            && event.getY() >= position[1] && event.getY() <= (position[1] + position[3])) {

                        upx = (event.getX() - position[0]) * scaleX;
                        upy = (event.getY() - position[1]) * scaleY;

                        System.out.println("(getx " + event.getX() + "; gety " + event.getY() + ")");
                        System.out.println("(vwidth " + v.getWidth() + "; vheight " + v.getHeight() + ")");
                        System.out.println("(upx " + upx + "; upy " + upy + ")");
                        System.out.println("(downx " + downx + "; downy " + downy + ")");

                        //restore bitmap when action down
                        canvas.drawBitmap(bitmapBackup, 0, 0, null);

                        LineSegment lineCoords;
                        path = new Path();
                        path.moveTo(downx, downy);
                        path.lineTo(upx, upy);

                        lineCoords = new LineSegment(new Point(downx, downy), new Point(upx, upy));
                        pathMap.put(++lastPathId, path);
                        lineCoordsMap.put(++lastPathId, lineCoords);
                        canvas.drawPath(path, paint);

                        photoView.invalidate();
                        downx = 0;  downy = 0;
                        upx = 0;    upy = 0;
                    }
                }

                break;

            case MotionEvent.ACTION_CANCEL:
                break;

            default:
                break;
        }

        return true;
    }

    public static int[] getBitmapPositionInsideImageView(ImageView imageView) {
        int[] ret = new int[4];

        if (imageView == null || imageView.getDrawable() == null)
            return ret;

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH)/2;
        int left = (int) (imgViewW - actW)/2;

        ret[0] = left;
        ret[1] = top;

        return ret;
    }

}
