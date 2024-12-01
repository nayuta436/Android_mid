package com.example.android.notepad;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NoteEditor extends Activity {
    private static final String TAG = "NoteEditor";

    private static final String[] PROJECTION =
            new String[]{
                    NotePad.Notes._ID,
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE,
                    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                    NotePad.Notes.COLUMN_NAME_CREATE_DATE
            };

    private static final String ORIGINAL_CONTENT = "origContent";

    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private ImageView camera;
    private String mOriginalContent;
    private LinearLayout all;
    private Button btn;

    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            int count = getLineCount();

            Rect r = mRect;
            Paint paint = mPaint;

            for (int i = 0; i < count; i++) {

                int baseline = getLineBounds(i, r);

                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            super.onDraw(canvas);
        }
    }

    private String selectedItem = null;
    private Spinner spinner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("image", Context.MODE_PRIVATE);

        final Intent intent = getIntent();


        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {

            mState = STATE_EDIT;
            mUri = intent.getData();

        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {

            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            if (mUri == null) {

                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                finish();
                return;
            }

            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {

            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        mCursor = managedQuery(
                mUri,
                PROJECTION,
                null,
                null,
                null
        );


        if (Intent.ACTION_PASTE.equals(action)) {
            performPaste();
            mState = STATE_EDIT;
        }

        setContentView(R.layout.note_editor);

        mText = (EditText) findViewById(R.id.note);
        camera = (ImageView) findViewById(R.id.camera);
        all = (LinearLayout) findViewById(R.id.all);
        sharedPreferences = getSharedPreferences("image", Context.MODE_PRIVATE);
        int bgColor = sharedPreferences.getInt("bg_color", R.color.wihte);
        all.setBackgroundResource(bgColor);
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater flater = LayoutInflater.from(view.getContext());
                View v= flater.inflate(R.layout.my_color_select, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setView(v);
                final AlertDialog alert = builder.create();
                Button red = (Button)v.findViewById(R.id.red);
                red.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.red);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.red);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button yellow = (Button)v.findViewById(R.id.yellow);
                yellow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.yellow);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.yellow);
                        editor.apply(); // 应用更改
                        alert.cancel();
                    }
                });
                Button green = (Button)v.findViewById(R.id.green);
                green.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.green);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.green);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button blue = (Button)v.findViewById(R.id.blue);
                blue.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.blue);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.blue);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button orange = (Button)v.findViewById(R.id.orange);
                orange.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.orange);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.orange);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button wihte = (Button)v.findViewById(R.id.wihte);
                wihte.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.wihte);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.wihte);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button green_two = (Button)v.findViewById(R.id.green_two);
                green_two.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.green_two);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.green_two);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button red_two = (Button)v.findViewById(R.id.red_two);
                red_two.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.red_two);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.red_two);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button orange_two = (Button)v.findViewById(R.id.orange_two);
                orange_two.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.orange_two);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.orange_two);
                        editor.apply();
                        alert.cancel();
                    }
                });
                Button purple = (Button)v.findViewById(R.id.purple);
                purple.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        all.setBackgroundResource(R.color.purple);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("bg_color", R.color.purple);
                        editor.apply();
                        alert.cancel();
                    }
                });
                alert.show();
            }
        });
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    private String selectedItemDh = null;
    private String imageName = null;

    private void saveImageToApp(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            Long timestamp = System.currentTimeMillis();
            imageName = timestamp + ".png";
            File file = new File(getFilesDir(), imageName);
            String absolutePath = file.getAbsolutePath();
            file.getPath();
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            camera.setImageBitmap(bitmap);
            Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 11 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            saveImageToApp(selectedImage);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCursor != null) {
            mCursor.requery();

            mCursor.moveToFirst();

            if (mState == STATE_EDIT) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }


            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            int colSpinnerIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);
            int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            String note = mCursor.getString(colNoteIndex);
            String time = mCursor.getString(4);
            String title = mCursor.getString(colTitleIndex);
            setTitle(title);
            selectedItemDh = mCursor.getString(3);
            if (!TextUtils.isEmpty(sharedPreferences.getString(time, null)))
                imageName = sharedPreferences.getString(time, null);
            if (!TextUtils.isEmpty(imageName) && !"null".equals(imageName)) {
                File file = new File(getFilesDir(), imageName);
                if (file.exists()) {
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        camera.setImageBitmap(bitmap);
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!TextUtils.isEmpty(note)) {
                mText.setTextKeepState(note);
            }
            camera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(intent, 11);
                    }
                }
            });
            if (!TextUtils.isEmpty(selectedItemDh) && !"null".equals(selectedItemDh)) {
                if ("life".equals(selectedItemDh))
                    spinner.setSelection(0);
                else if ("friend".equals(selectedItemDh))
                    spinner.setSelection(1);
                else if ("car".equals(selectedItemDh))
                    spinner.setSelection(2);
                else if ("school".equals(selectedItemDh))
                    spinner.setSelection(3);
            }
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCursor != null) {

            String text = mText.getText().toString();
            int length = text.length();

            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

            } else if (mState == STATE_EDIT) {
                updateNote(text, selectedItem);
            } else if (mState == STATE_INSERT) {
                updateNote(text, selectedItem);
                mState = STATE_EDIT;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        if (mState == STATE_EDIT) {
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                String text = mText.getText().toString();
                updateNote(text, selectedItem);
                finish();
                break;
            case R.id.menu_delete:
                deleteNote();
                finish();
                break;
            case R.id.menu_revert:
                cancelNote();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final void performPaste() {

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        ContentResolver cr = getContentResolver();

        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text = null;
            String title = null;

            ClipData.Item item = clip.getItemAt(0);

            Uri uri = item.getUri();

            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {

                Cursor orig = cr.query(
                        uri,
                        PROJECTION,
                        null,
                        null,
                        null
                );

                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    orig.close();
                }
            }

            if (text == null) {
                text = item.coerceToText(this).toString();
            }
            setTitle(title);
            updateNote(text, selectedItem);
        }
    }

    private SharedPreferences sharedPreferences;

    private final void updateNote(String text, String category) {
        String title=String.valueOf(getTitle());
        if (TextUtils.isEmpty(title)||"null".equals(title)){
            title=text;
        }
        ContentValues values = new ContentValues();
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = formatter.format(now);
        if (imageName != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(formattedDateTime, imageName);
            editor.apply();
        }
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, formattedDateTime);
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, category);
        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        if (mState == STATE_INSERT) {

            if (title == null) {

                int length = text.length();

                title = text.substring(0, Math.min(30, length));

                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }

        } else if (title != null) {

        }

        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        getContentResolver().update(
                mUri,
                values,
                null,
                null
        );

    }

    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}