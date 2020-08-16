package com.example.filechooser

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import java.io.File

class MainActivity : AppCompatActivity() {

    private  val READ_REQUEST_CODE: Int = 42    //Activity識別用の任意の値

    //デフォルトの処理
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    //ボタンイベント
    fun Click(view: View) {
        //ストレージ アクセス フレームワークを使用してファイルを開く
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    //インテントが終わった時のイベント
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        //ファイルが選択されているときに処理をする
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //選んだファイルのUriを取得する
            val intent = Intent(Intent.ACTION_VIEW)
            val s = "file://"+getPathFromUri(applicationContext, Uri.parse(resultData?.getDataString()))
            val u = resultData?.getData()
            val r = Uri.parse(s)

            //MIMEタイプの取得
            val extension = s.substring(s.lastIndexOf(".") + 1)
            val mimeTypeMap = MimeTypeMap.getSingleton()
            val mimeType = mimeTypeMap.getMimeTypeFromExtension(extension)
            try {
                intent.setDataAndType(r, mimeType)
                startActivity(intent)
            }catch ( e:Exception){
                //暗黙インテントで対応できなかった場合
                println(e)
            }
        }
    }

    //引用した処理
    fun  getPathFromUri(context: Context, uri: Uri): String? {
        //Androidのバージョンによって、データへのアクセス方法が変わる
        val isAfterKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        if (isAfterKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if ("com.android.externalstorage.documents" ==
                    uri.authority
            ) { // ExternalStorageProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                return if ("primary".equals(type, ignoreCase = true)) {
                    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                } else {
                    "/stroage/" + type + "/" + split[1]
                }
            } else if ("com.android.providers.downloads.documents" ==
                    uri.authority
            ) { // DownloadsProvider
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if ("com.android.providers.media.documents" ==
                    uri.authority
            ) { // MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                contentUri = MediaStore.Files.getContentUri("external")
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                        split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { //MediaStore
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
            return uri.path
        }
        return null
    }

    //引用した処理
    fun getDataColumn(
            context: Context, uri: Uri?, selection: String?,
            selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(
                //API level 29 からは、このアクセス方法は廃止されるかも
                MediaStore.Files.FileColumns.DATA
        )
        try {
            //コンテンツ プロバイダにデータをリクエストする
            cursor = context.getContentResolver().query(
                    uri!!, projection, selection, selectionArgs, null
            )
            //リクエスト結果のデータにアクセスする
            if (cursor != null && cursor.moveToFirst()) {
                val cindex: Int = cursor.getColumnIndexOrThrow(projection[0])
                return cursor.getString(cindex)
            }
        } finally {
            if (cursor != null) cursor.close()
        }
        return null
    }
}