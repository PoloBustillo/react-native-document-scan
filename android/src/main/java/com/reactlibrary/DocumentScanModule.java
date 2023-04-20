// DocumentScanModule.java
package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.module.annotations.ReactModule;
import io.polobustillo.documentscanner.DocumentScanner;
import io.polobustillo.documentscanner.constants.DocumentScannerExtra;
import java.util.ArrayList;

@ReactModule(name = DocumentScanModule.NAME)
public class DocumentScanModule extends ReactContextBaseJavaModule {
    public static final String NAME = "DocumentScan";
    private static final int DOCUMENT_SCAN_REQUEST = 938;
    DocumentScanner documentScanner;

    private ActivityEventListener activityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(
                final Activity activity,
                final int requestCode,
                final int resultCode,
                final Intent intent) {
            // trigger callbacks (success, cancel, error)
            if (requestCode == DOCUMENT_SCAN_REQUEST) {
                documentScanner.handleDocumentScanIntentResult(
                        new ActivityResult(resultCode, intent)
                );
            }
        }
    };

    public DocumentScanModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(activityEventListener);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void scanDocument(ReadableMap options, Promise promise) {
        Activity currentActivity = getCurrentActivity();
        WritableMap response = new WritableNativeMap();

        // create a document scanner
        documentScanner = new DocumentScanner(
                (ComponentActivity) currentActivity,
                (ArrayList<String> documentScanResults) -> {
                    // document scan success
                    WritableArray docScanResults = new WritableNativeArray();
                    documentScanResults.forEach(
                            documentScanResult -> docScanResults.pushString(documentScanResult)
                    );
                    response.putArray(
                            "scannedImages",
                            docScanResults
                    );
                    response.putString("status", "success");
                    promise.resolve(response);
                    return null;
                },
                (String errorMessage) -> {
                    // document scan error
                    promise.reject("document scan error", errorMessage);
                    return null;
                },
                () -> {
                    // when user cancels document scan
                    response.putString("status", "cancel");
                    promise.resolve(response);
                    return null;
                },
                options.hasKey("responseType")
                        ? options.getString("responseType") : null,
                options.hasKey(DocumentScannerExtra.EXTRA_LET_USER_ADJUST_CROP)
                        ? options.getBoolean(DocumentScannerExtra.EXTRA_LET_USER_ADJUST_CROP)
                        : null,
                options.hasKey(DocumentScannerExtra.EXTRA_MAX_NUM_DOCUMENTS)
                        ? options.getInt(DocumentScannerExtra.EXTRA_MAX_NUM_DOCUMENTS)
                        : null,
                options.hasKey(DocumentScannerExtra.EXTRA_MIN_NUM_DOCUMENTS)
                        ? options.getInt(DocumentScannerExtra.EXTRA_MIN_NUM_DOCUMENTS)
                        : null,
                options.hasKey(DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY)
                        ? options.getInt(DocumentScannerExtra.EXTRA_CROPPED_IMAGE_QUALITY)
                        : null
        );

        // launch the document scanner
        currentActivity.startActivityForResult(
                documentScanner.createDocumentScanIntent(),
                DOCUMENT_SCAN_REQUEST
        );
    }
}

