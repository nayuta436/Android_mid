package com.example.android.notepad;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {

    private static final String TAG = "NotePadProvider";

    private static final String DATABASE_NAME = "note_pad.db";

    private static final int DATABASE_VERSION = 2;

    private static HashMap<String, String> sNotesProjectionMap;

    private static HashMap<String, String> sLiveFolderProjectionMap;

    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_TITLE,
    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;

    private static final int NOTES = 1;

    private static final int NOTE_ID = 2;

    private static final int LIVE_FOLDER_NOTES = 3;

    private static final UriMatcher sUriMatcher;

    private DatabaseHelper mOpenHelper;

    static {

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);

        sNotesProjectionMap = new HashMap<String, String>();

        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);

        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        sLiveFolderProjectionMap = new HashMap<String, String>();

        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);

        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
                LiveFolders.NAME);
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {

            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS notes");

            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {

        mOpenHelper = new DatabaseHelper(getContext());

        return true;
    }

    private SharedPreferences sharedPref;
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(NotePad.Notes.TABLE_NAME);

        sharedPref = getContext().getSharedPreferences("sp", Context.MODE_PRIVATE);
        String res = sharedPref.getString("res", null);
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setProjectionMap(sNotesProjectionMap);
                if (!TextUtils.isEmpty(res)&&!"null".equals(res)){
                    qb.appendWhere(
                            NotePad.Notes.COLUMN_NAME_TITLE +
                                    " like '%"+res+"%'");
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("res", null);
                    editor.apply();
                }
                break;

            case NOTE_ID:
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(
                        NotePad.Notes._ID +
                                "=" +
                                uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_NOTES:
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }


        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = qb.query(
                db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {

        switch (sUriMatcher.match(uri)) {

            case NOTES:
            case LIVE_FOLDER_NOTES:
                return NotePad.Notes.CONTENT_TYPE;

            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        switch (sUriMatcher.match(uri)) {

            case NOTES:
            case LIVE_FOLDER_NOTES:
                return null;

            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }


    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {

        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

        if (mimeTypes != null) {

            Cursor c = query(
                    uri,
                    READ_NOTE_PROJECTION,

                    null,
                    null,
                    null

            );


            if (c == null || !c.moveToFirst()) {

                if (c != null) {
                    c.close();
                }

                throw new FileNotFoundException("Unable to query " + uri);
            }

            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
                                Bundle opts, Cursor c) {
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Ooops", e);
        } finally {
            c.close();
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        if (sUriMatcher.match(uri) != NOTES) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;

        if (initialValues != null) {
            values = new ContentValues(initialValues);

        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        if (values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TimeZone timeZoneBeijing = TimeZone.getTimeZone("Asia/Shanghai");
            sdf.setTimeZone(timeZoneBeijing);
            String formattedDate = sdf.format(new Date(now));
            values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, formattedDate);
        }

        if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }

        if (values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE) == false) {
            Resources r = Resources.getSystem();
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
        }

        if (values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE) == false) {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(
                NotePad.Notes.TABLE_NAME,
                NotePad.Notes.COLUMN_NAME_NOTE,

                values

        );

        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);

            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        switch (sUriMatcher.match(uri)) {

            case NOTES:
                count = db.delete(
                        NotePad.Notes.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;

            case NOTE_ID:

                finalWhere =
                        NotePad.Notes._ID +
                                " = " +
                                uri.getPathSegments().
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.delete(
                        NotePad.Notes.TABLE_NAME,
                        finalWhere,
                        whereArgs
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone timeZoneBeijing = TimeZone.getTimeZone("Asia/Shanghai");
        sdf.setTimeZone(timeZoneBeijing);
        Long now = Long.valueOf(System.currentTimeMillis());
        String formattedDate = sdf.format(new Date(now));
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, formattedDate);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        switch (sUriMatcher.match(uri)) {

            case NOTES:

                count = db.update(
                        NotePad.Notes.TABLE_NAME,
                        values,
                        where,
                        whereArgs
                );
                break;

            case NOTE_ID:
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);

                finalWhere =
                        NotePad.Notes._ID +
                                " = " +
                                uri.getPathSegments().
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.update(
                        NotePad.Notes.TABLE_NAME,
                        values,
                        finalWhere,
                        whereArgs
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }


    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}