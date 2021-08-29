package com.example.przeglady

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.google.firebase.storage.FirebaseStorage
import java.lang.NumberFormatException
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.TextView
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.io.*
import com.google.android.gms.tasks.OnSuccessListener

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: java.lang.Exception){println(e)}


        val database = FirebaseDatabase.getInstance()
        val myRef = database.reference
        myRef.keepSynced(true)

        val cm = this@MainActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnected == true
        if (isConnected){
            val storage = FirebaseStorage.getInstance()
            val gsReference = storage.getReferenceFromUrl("gs://new-rzi.appspot.com")

            val kompleksy_csv = gsReference.child("kompleksy.csv")
            kompleksy_csv.getMetadata().addOnSuccessListener(
                OnSuccessListener<StorageMetadata>() {
                    @Override
                    fun onSuccess(storageMetadata: StorageMetadata ) {
                        var time_old_file = 0
                        try{
                            time_old_file = File(this@MainActivity.filesDir,"kompleksy_time.txt").readLines()[0].toInt()

                        }catch (e: java.lang.Exception){println(e)}
                        val time_server_file = storageMetadata.getUpdatedTimeMillis().toInt()
                        if(time_server_file>time_old_file) {
                            val new_file_kompleksy = File(this@MainActivity.filesDir, "kompleksy.csv")
                            kompleksy_csv.getFile(new_file_kompleksy).addOnSuccessListener {
                                val fileReader = BufferedReader(FileReader(new_file_kompleksy))
                                val file = "kompleksy.csv"
                                val data: String = fileReader.use(BufferedReader::readText)
                                val fileOutputStream: FileOutputStream
                                try {
                                    fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
                                    fileOutputStream.write(data.toByteArray(charset("UTF_8")))
                                } catch (e: Exception) {
                                    println(e)
                                }

                                // save timestamp of the file
                                val fileName = "kompleksy_time.txt"
                                val myfile = File(this@MainActivity.filesDir, fileName)
                                myfile.printWriter().use { out ->
                                    out.print(time_server_file)
                                }

                            }.addOnFailureListener {
                            }
                        }else{
                        }
                        dostuff(BufferedReader(FileReader(File(this@MainActivity.filesDir, "kompleksy.csv"))))
                    }
                    onSuccess(it)
                })

            val konstrukcja_csv = gsReference.child("konstrukcja.csv")
            konstrukcja_csv.getMetadata().addOnSuccessListener(
                OnSuccessListener<StorageMetadata>() {
                    @Override
                    fun onSuccess(storageMetadata: StorageMetadata ) {
                        var time_old_file = 0
                        try{
                            time_old_file = File(this@MainActivity.filesDir,"konstrukcja_time.txt").readLines()[0].toInt()

                        }catch (e: java.lang.Exception){println(e)}
                        val time_server_file = storageMetadata.getUpdatedTimeMillis().toInt()
                        if(time_server_file>time_old_file) {
                            val new_file_konstrukcja = File(this@MainActivity.filesDir, "konstrukcja.csv")
                            konstrukcja_csv.getFile(new_file_konstrukcja).addOnSuccessListener {
                                val fileReader = BufferedReader(FileReader(new_file_konstrukcja))
                                val file = "konstrukcja.csv"
                                val data: String = fileReader.use(BufferedReader::readText)
                                val fileOutputStream: FileOutputStream
                                try {
                                    fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
                                    fileOutputStream.write(data.toByteArray(charset("UTF_8")))
                                } catch (e: Exception) {
                                    println(e)
                                }

                                // save timestamp of the file
                                val fileName = "konstrukcja_time.txt"
                                val myfile = File(this@MainActivity.filesDir, fileName)
                                myfile.printWriter().use { out ->
                                    out.print(time_server_file)
                                }
                            }.addOnFailureListener {
                            }
                        }else{
                        }
                    }
                    onSuccess(it)
                })


            val usterki_csv = gsReference.child("usterki.csv")
            usterki_csv.getMetadata().addOnSuccessListener(
                OnSuccessListener<StorageMetadata>() {
                    @Override
                    fun onSuccess(storageMetadata: StorageMetadata ) {
                        var time_old_file = 0
                        try{
                            time_old_file = File(this@MainActivity.filesDir,"usterki_time.txt").readLines()[0].toInt()

                        }catch (e: java.lang.Exception){println(e)}
                        val time_server_file = storageMetadata.getUpdatedTimeMillis().toInt()
                        if(time_server_file>time_old_file) {
                            val new_file_usterki = File(this@MainActivity.filesDir, "usterki.csv")
                            usterki_csv.getFile(new_file_usterki).addOnSuccessListener {
                                val fileReader = BufferedReader(FileReader(new_file_usterki))
                                val file = "usterki.csv"
                                val data: String = fileReader.use(BufferedReader::readText)
                                val fileOutputStream: FileOutputStream
                                try {
                                    fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
                                    fileOutputStream.write(data.toByteArray(charset("UTF_8")))
                                } catch (e: Exception) {
                                    println(e)
                                }

                                // save timestamp of the file
                                val fileName = "usterki_time.txt"
                                val myfile = File(this@MainActivity.filesDir, fileName)
                                myfile.printWriter().use { out ->
                                    out.print(time_server_file)
                                }
                            }.addOnFailureListener {
                            }
                            }else{
                            }
                    }
                    onSuccess(it)
                })


        } else{
            var fileInputStream: FileInputStream? = null
            fileInputStream = openFileInput("kompleksy.csv")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val fileReader = BufferedReader(inputStreamReader)
            dostuff(fileReader)
        }
    }


    private fun dostuff(fileReader: BufferedReader){
//            Create dictionary in the form kompleks: obiekt, obiekt, obiekt, etc.
        val mapaObiektow = mutableMapOf<String,ArrayList<String>>()
//            Read input csv file
        var line = fileReader.readLine()
        while (line != null) {
            val tokens = line.split(";")
            if (tokens.size > 0) {
                val nr_kompleksu = tokens[2]
                val nr_obiektu = tokens[3]
////                    Checks if key is already in the map, if not adds the key and value (initialize element of map)
                val listaObiektow = mapaObiektow.getOrElse(nr_kompleksu){ arrayListOf(nr_obiektu)}
                if (nr_obiektu !in listaObiektow){
                    listaObiektow.add(nr_obiektu)
                }
                mapaObiektow.put(nr_kompleksu, listaObiektow)
            }
            line = fileReader.readLine()
        }
//            END FILE READING

//            get listaKompleksow to create set of buttons on the left side of the layout
        val listaKompleksow = mapaObiektow.keys.toMutableList()
        listaKompleksow.sort()

//            Dynamically generate buttons with kompleks numbers
//            1. Select layout in which buttons will be generated
        val left_layout = findViewById<LinearLayout>(R.id.left_layout)
//            2. Repeat for every kompleks in a list
        for (kompleks in listaKompleksow){
            try {
                kompleks.toInt()
            } catch (e: NumberFormatException){
                continue
            }
//                3. Create new button
            val button = Button(this)
            button.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            button.setText(kompleks)
            button.setTextSize(TypedValue.COMPLEX_UNIT_PT,getResources().getDimension(R.dimen.result_font))
//                Add event listener to the button so the list on the right will be filled with buttons
            button.setOnClickListener {
                //                    1. Select list of obiekty
                val nazwa = findViewById<TextView>(R.id.nazwa_apki)
                nazwa.setText(kompleks)
                val nr_kompleksu = button.text.toString()
                val listaObiektow = mapaObiektow.getOrElse(nr_kompleksu){arrayListOf<String>("Brak obiektów")}
                fillRightLayout(listaObiektow, nr_kompleksu)
            }
            left_layout.addView(button)
        }
    }

    private fun fillRightLayout(listaObiektow: ArrayList<String>, nr_kompleksu: String) {
//        Select layout
        val right_layout = findViewById<LinearLayout>(R.id.right_layout)
        // CLEAR ALL BUTTONS IN THIS LAYOUT
        right_layout.removeAllViews()

//      Repeat for every obiekt in a list
        for (obiekt in listaObiektow){
//            Make sure it's not header
            try {
                obiekt.toInt()
            } catch (e: NumberFormatException){
                continue
            }
//                3. Create new button
            val button = Button(this)
            try{
                var plik = File(this@MainActivity.filesDir, "gotowe/"+nr_kompleksu+"/"+obiekt+".txt")
                var text = plik.readText()
                if(!text.isEmpty()){
                    button.setBackgroundColor(Color.GREEN)
                }
            }catch (e:Exception){

            }
            button.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            button.setText(obiekt)
            button.setTextSize(TypedValue.COMPLEX_UNIT_PT,getResources().getDimension(R.dimen.result_font))
//                Add event listener to the button so the list on the right will be filled with buttons
            button.setOnClickListener {
                val intent = Intent(this, settingsActivity::class.java)
                intent.putExtra("obiekt",obiekt)
                intent.putExtra("nrKompleksu",nr_kompleksu)
                startActivity(intent)
            }
            right_layout.addView(button)
        }
    }
}
