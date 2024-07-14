/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.provider;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Helper for accessing features in {@link DocumentsContract}.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public final class DocumentsContractCompat {

    /**
     * Helper for accessing features in {@link DocumentsContract.Document}.
     */
    public static final class DocumentCompat {
        /**
         * Flag indicating that a document is virtual, and doesn't have byte
         * representation in the MIME type specified as {@link Document#COLUMN_MIME_TYPE}.
         *
         * <p><em>Virtual documents must have at least one alternative streamable
         * format via {@link DocumentsProvider#openTypedDocument}</em>
         *
         * @see Document#FLAG_VIRTUAL_DOCUMENT
         */
        public static final int FLAG_VIRTUAL_DOCUMENT = 1 << 9;

        private DocumentCompat() {
        }
    }

    private static final String PATH_TREE = "tree";

    /**
     * Checks if the given URI represents a {@link Document} backed by a
     * {@link DocumentsProvider}.
     *
     * @see DocumentsContract#isDocumentUri(Context, Uri)
     */
    public static boolean isDocumentUri(Context context, Uri uri) {
        return DocumentsContract.isDocumentUri(context, uri);
    }

    /**
     * Checks if the given URI represents a {@link Document} tree.
     *
     * @see DocumentsContract#isTreeUri(Uri)
     */


    /**
     * Extract the {@link Document#COLUMN_DOCUMENT_ID} from the given URI.
     *
     * @see DocumentsContract#getDocumentId(Uri)
     */
    
    public static String getDocumentId(Uri documentUri) {
        return DocumentsContract.getDocumentId(documentUri);
    }

    /**
     * Extract the via {@link Document#COLUMN_DOCUMENT_ID} from the given URI.
     *
     * @see DocumentsContract#getTreeDocumentId(Uri)
     */
    
    public static String getTreeDocumentId( Uri documentUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.getTreeDocumentId(documentUri);
        }
        return null;
    }

    /**
     * Build URI representing the target {@link Document#COLUMN_DOCUMENT_ID} in
     * a document provider. When queried, a provider will return a single row
     * with columns defined by {@link Document}.
     *
     * @see DocumentsContract#buildDocumentUri(String, String)
     */
    
    public static Uri buildDocumentUri(String authority, String documentId) {
        return DocumentsContract.buildDocumentUri(authority, documentId);
    }

    /**
     * Build URI representing the target {@link Document#COLUMN_DOCUMENT_ID} in
     * a document provider. When queried, a provider will return a single row
     * with columns defined by {@link Document}.
     */
    
    public static Uri buildDocumentUriUsingTree( Uri treeUri,  String documentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildDocumentUriUsingTree(treeUri, documentId);
        }
        return null;
    }

    /**
     * Build URI representing access to descendant documents of the given
     * {@link Document#COLUMN_DOCUMENT_ID}.
     *
     * @see DocumentsContract#buildTreeDocumentUri(String, String)
     */
    
    public static Uri buildTreeDocumentUri( String authority,  String documentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildTreeDocumentUri(authority, documentId);
        }
        return null;
    }

    /**
     * Build URI representing the children of the target directory in a document
     * provider. When queried, a provider will return zero or more rows with
     * columns defined by {@link Document}.
     *
     * @see DocumentsContract#buildChildDocumentsUri(String, String)
     */
    
    public static Uri buildChildDocumentsUri( String authority,
                                              String parentDocumentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildChildDocumentsUri(authority, parentDocumentId);
        }
        return null;
    }

    /**
     * Build URI representing the children of the target directory in a document
     * provider. When queried, a provider will return zero or more rows with
     * columns defined by {@link Document}.
     *
     * @see DocumentsContract#buildChildDocumentsUriUsingTree(Uri, String)
     */
    
    public static Uri buildChildDocumentsUriUsingTree( Uri treeUri,
                                                       String parentDocumentId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.buildChildDocumentsUriUsingTree(treeUri,
                    parentDocumentId);
        }
        return null;
    }

    /**
     * Create a new document with given MIME type and display name.
     *
     * @param content           the resolver to use to create the document.
     * @param parentDocumentUri directory with {@link Document#FLAG_DIR_SUPPORTS_CREATE}
     * @param mimeType          MIME type of new document
     * @param displayName       name of new document
     * @return newly created document, or {@code null} if failed
     */
    
    public static Uri createDocument( ContentResolver content,
                                      Uri parentDocumentUri,  String mimeType,  String displayName)
            throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.createDocument(content, parentDocumentUri, mimeType,
                    displayName);
        }
        return null;
    }

    /**
     * Change the display name of an existing document.
     *
     * @see DocumentsContract#renameDocument(ContentResolver, Uri, String)
     */
    
    public static Uri renameDocument( ContentResolver content,
                                      Uri documentUri,  String displayName) throws FileNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return DocumentsContractApi21Impl.renameDocument(content, documentUri, displayName);
        }
        return null;
    }


    private static class DocumentsContractApi21Impl {
        static String getTreeDocumentId(Uri documentUri) {
            return DocumentsContract.getTreeDocumentId(documentUri);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public static Uri buildTreeDocumentUri(String authority, String documentId) {
            return DocumentsContract.buildTreeDocumentUri(authority, documentId);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId) {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
        }

        static Uri buildChildDocumentsUri(String authority, String parentDocumentId) {
            return DocumentsContract.buildChildDocumentsUri(authority, parentDocumentId);
        }

        static Uri buildChildDocumentsUriUsingTree(Uri treeUri, String parentDocumentId) {
            return DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId);
        }

        static Uri createDocument(ContentResolver content, Uri parentDocumentUri,
                                  String mimeType, String displayName) throws FileNotFoundException {
            return DocumentsContract.createDocument(content, parentDocumentUri, mimeType,
                    displayName);
        }

        static Uri renameDocument( ContentResolver content,
                                   Uri documentUri,  String displayName)
                throws FileNotFoundException {
            return DocumentsContract.renameDocument(content, documentUri, displayName);
        }

        private DocumentsContractApi21Impl() {
        }
    }


    private DocumentsContractCompat() {
    }
}