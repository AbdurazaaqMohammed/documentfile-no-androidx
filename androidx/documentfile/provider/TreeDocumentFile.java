/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.documentfile.provider;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import java.util.ArrayList;

class TreeDocumentFile extends DocumentFile {
    private Context mContext;
    private Uri mUri;

    TreeDocumentFile( DocumentFile parent, Context context, Uri uri) {
        super(parent);
        mContext = context;
        mUri = uri;
    }

    @Override
    
    public DocumentFile createFile( String mimeType,  String displayName) {
        final Uri result = TreeDocumentFile.createFile(mContext, mUri, mimeType, displayName);
        return (result != null) ? new TreeDocumentFile(this, mContext, result) : null;
    }

    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Uri createFile(Context context, Uri self, String mimeType,
                                  String displayName) {
        try {
            return DocumentsContract.createDocument(context.getContentResolver(), self, mimeType,
                    displayName);
        } catch (Exception e) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    
    public DocumentFile createDirectory( String displayName) {
        final Uri result = TreeDocumentFile.createFile(
                mContext, mUri, DocumentsContract.Document.MIME_TYPE_DIR, displayName);
        return (result != null) ? new TreeDocumentFile(this, mContext, result) : null;
    }

    
    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    
    public String getName() {
        return DocumentsContractApi19.getName(mContext, mUri);
    }

    @Override
    
    public String getType() {
        return DocumentsContractApi19.getType(mContext, mUri);
    }

    @Override
    public boolean isDirectory() {
        return DocumentsContractApi19.isDirectory(mContext, mUri);
    }

    @Override
    public boolean isFile() {
        return DocumentsContractApi19.isFile(mContext, mUri);
    }

    @Override
    public boolean isVirtual() {
        return DocumentsContractApi19.isVirtual(mContext, mUri);
    }

    @Override
    public long lastModified() {
        return DocumentsContractApi19.lastModified(mContext, mUri);
    }

    @Override
    public long length() {
        return DocumentsContractApi19.length(mContext, mUri);
    }

    @Override
    public boolean canRead() {
        return DocumentsContractApi19.canRead(mContext, mUri);
    }

    @Override
    public boolean canWrite() {
        return DocumentsContractApi19.canWrite(mContext, mUri);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public boolean delete() {
        try {
            return DocumentsContract.deleteDocument(mContext.getContentResolver(), mUri);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean exists() {
        return DocumentsContractApi19.exists(mContext, mUri);
    }

    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public DocumentFile[] listFiles() {
        final ContentResolver resolver = mContext.getContentResolver();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri,
                DocumentsContract.getDocumentId(mUri));
        final ArrayList<Uri> results = new ArrayList<>();

        Cursor c = null;
        try {
            c = resolver.query(childrenUri, new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID }, null, null, null);
            while (c.moveToNext()) {
                final String documentId = c.getString(0);
                final Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(mUri,
                        documentId);
                results.add(documentUri);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed query: " + e);
        } finally {
            closeQuietly(c);
        }

        final Uri[] result = results.toArray(new Uri[0]);
        final DocumentFile[] resultFiles = new DocumentFile[result.length];
        for (int i = 0; i < result.length; i++) {
            resultFiles[i] = new TreeDocumentFile(this, mContext, result[i]);
        }
        return resultFiles;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean renameTo( String displayName) {
        try {
            final Uri result = DocumentsContract.renameDocument(
                    mContext.getContentResolver(), mUri, displayName);
            if (result != null) {
                mUri = result;
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
