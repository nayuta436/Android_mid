package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";

    private static final String[] PROJECTION = new String[]{
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_CREATE_DATE, // 2
            NotePad.Notes.COLUMN_NAME_NOTE, // 3
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 4
    };

    private static final int COLUMN_INDEX_TITLE = 1;

    private SimpleCursorAdapter adapter;
    private Cursor cursor;
    private SharedPreferences sharedPref;
    private Uri data;
    private String[] dataColumns = {NotePad.Notes.COLUMN_NAME_TITLE,NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_CREATE_DATE,NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE};

    private int[] viewIDs = {android.R.id.text1,R.id.category, R.id.time, R.id.myClass};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getSharedPreferences("sp", Context.MODE_PRIVATE);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();

        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        data = getIntent().getData();
        cursor = managedQuery(
                data,
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs
        );
        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {

            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            Intent[] specifics = new Intent[1];

            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            MenuItem[] items = new MenuItem[1];

            Intent intent = new Intent(null, uri);

            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );
            if (items[0] != null) {

                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_select:
                LayoutInflater flater = LayoutInflater.from(this);
                View view = flater.inflate(R.layout.select_by_title_or_content, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(view);
                final AlertDialog alert = builder.create();
                Button btn_select = (Button)view.findViewById(R.id.btn_select);
                final EditText content=(EditText)view.findViewById(R.id.content);
                btn_select.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        String s = String.valueOf(content.getText()).trim();
                        if (TextUtils.isEmpty(s)||"null".equals(s)){
                            Toast.makeText(NotesList.this, "内容为空，请输入内容", Toast.LENGTH_SHORT).show();
                            editor.putString("res", null);
                        }else {
                            editor.putString("res", s);
                        }
                        editor.apply();
                        cursor = managedQuery(
                                data,
                                PROJECTION,
                                null,
                                null,
                                NotePad.Notes.DEFAULT_SORT_ORDER
                        );


                        adapter = new SimpleCursorAdapter(
                                v.getContext(),
                                R.layout.noteslist_item,
                                cursor,
                                dataColumns,
                                viewIDs
                        );

                        setListAdapter(adapter);
                        alert.cancel();
                    }
                });
                alert.show();
                return true;
            case R.id.menu_add:

                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            case R.id.menu_paste:

                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            Log.e(TAG, "bad menuInfo", e);

            return false;
        }
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        switch (item.getItemId()) {
            case R.id.context_open:
                startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                return true;
            case R.id.context_copy:
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);

                clipboard.setPrimaryClip(ClipData.newUri(
                        getContentResolver(),
                        "Note",
                        noteUri)
                );

                return true;
            case R.id.context_delete:

                getContentResolver().delete(
                        noteUri,
                        null,
                        null
                );

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}