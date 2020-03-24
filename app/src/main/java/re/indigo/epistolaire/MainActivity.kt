/*
 This is free and unencumbered software released into the public domain.
 See LICENSE file for details.
 */

package re.indigo.epistolaire

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.BufferedOutputStream
import java.io.File

/*
Róbert Kiszeli https://www.youtube.com/watch?v=ZALMdNgx9bw
https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android
 */

class MainActivity : AppCompatActivity() {
    private val TAG = "EpistolaireMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        /*
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
         */

        val wtf = 2
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), wtf)
        }

        addLine("--------------------")
        addLine("Please wait, backup is in progress. It can take a while if you have a lot of SMSes or MMSes.")

        DumpTask().execute()
    }

    fun addLine(line: String) {
        myText.text = myText.text.toString() + "\n" + line
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class DumpTask : AsyncTask<Void, Int, Unit>() {
        val file = File(getExternalFilesDir("."), "backup.json")
        val dumper = MmsDumper(contentResolver)

        override fun doInBackground(vararg params: Void?) {
            var written = 0

            val stream = BufferedOutputStream(file.outputStream())
            val writer = JsonObjectWriter(stream)
            writer.beginObject()
            writer.dumpKey("conversations")

            writer.beginArray()
            for (threadId in dumper.conversations()) {
                writer.beginArray()
                dumper.foreachThreadMessage(threadId) { jmsg ->
                    writer.dumpValue(jmsg)
                    written += 1
                    publishProgress(written)
                }
                writer.endArray()
            }
            writer.endArray()

            writer.dumpKey("errors")
            writer.dumpValue(dumper.errors)

            writer.endObject()
            writer.close()
        }

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
            progressBar.max = dumper.countAllMessages()
            Log.i(TAG, "max = ${progressBar.max}")
        }

        override fun onPostExecute(result: Unit?) {
            progressBar.visibility = View.GONE
            val hasErrors = (dumper.errors.length() > 0)
            if (hasErrors) {
                Log.i(TAG, "backup done with some errors")
            } else {
                Log.i(TAG, "backup successful")
            }

            addLine("Done! Backup was saved to $file")
            if (hasErrors) {
                addLine("Some errors were encountered though")
            }

            addLine("See https://gitlab.com/hydrargyrum/epistolaire for viewing backup as HTML")
        }

        override fun onProgressUpdate(vararg values: Int?) {
            progressBar.progress = values[0]!!
        }
    }
}
