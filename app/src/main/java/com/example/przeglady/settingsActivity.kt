package com.example.przeglady

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_ask.*
import java.io.*
import java.lang.Exception
import java.nio.charset.Charset
import com.google.firebase.FirebaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import org.w3c.dom.Text


class settingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)



        var nr_kompleksu: String? = "99999"
        var nr_obiektu: String? = "99999"
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            nr_kompleksu = bundle.getString("nrKompleksu")
            nr_obiektu = bundle.getString("obiekt")
        }

        val naglowek = findViewById<TextView>(R.id.kompleks_obiekt)
        naglowek.setText("Kompleks: "+nr_kompleksu+", Obiekt: "+nr_obiektu)
        val database = FirebaseDatabase.getInstance()
        val myRef = database.reference
        myRef.keepSynced(true)

        // get list of construction elements

        // save local copy of the file

        val wallpaperDirectory = File(this@settingsActivity.filesDir, "gotowe/"+nr_kompleksu.toString()+"/")
        // have the object build the directory structure, if needed.
        wallpaperDirectory.mkdirs()

        val fileName = "gotowe/"+nr_kompleksu.toString()+"/"+nr_obiektu.toString()+".txt"
        val myfile_save = File(this@settingsActivity.filesDir, fileName)
        Log.d("Path",this@settingsActivity.filesDir.toString())


        var reader = BufferedReader(FileReader(this.filesDir.toString() + "/konstrukcja.csv"))
        val mapaKonstrukcji = mutableMapOf<String, List<String>>()
        var line = reader.readLine()
        while (line != null) {
            val tokens = line.split(";")
            if (tokens.size > 0) {
                val element = tokens[0].replace("\"", "")
                val konstrukcja = tokens.drop(1).toMutableList()
                konstrukcja.set(konstrukcja.size - 1, konstrukcja.get(konstrukcja.size - 1).replace("\"", ""))
                mapaKonstrukcji.put(element, konstrukcja)
            }
            line = reader.readLine()
        }

        reader = BufferedReader(FileReader(this.filesDir.toString() + "/usterki.csv"))
        val mapaUsterek = mutableMapOf<String, Map<String, String>>()
        var mapaZalecen = mutableMapOf<String, String>()
        mapaZalecen["test"] = "test"
        line = reader.readLine()
        var element_obiektu = ""
        var stary_element = ""

        while (line != null) {
            val tokens = line.split(";")
            if (tokens.size > 0) {

                stary_element = element_obiektu
                element_obiektu = tokens[0]
                val usterka = tokens[1]
                val zalecenie = tokens[2]
                // check if element is different than last element in map
                if (mapaUsterek.keys.contains(element_obiektu)) {
                    //jezeli juz mamy element na liscie trzeba uzupelnic usterki
                    mapaZalecen[usterka] = zalecenie
                } else if (mapaUsterek.size > 0) {
                    //jezeli mamy nowy element ale lista nie jest pusta (zmiana elementu)
                    mapaUsterek[stary_element] = mapaZalecen.toMap()
                    mapaUsterek[element_obiektu] = emptyMap()
                    mapaZalecen.clear()
                    mapaZalecen[usterka] = zalecenie

                } else {
                    //pierwsza iteracja
                    mapaUsterek[element_obiektu] = mapaZalecen.toMap()
                }
            }
            line = reader.readLine()
        }
        mapaUsterek[element_obiektu] = mapaZalecen.toMap()
//            END FILE READING
//            get listaElementow to create set of buttons on the left side of the layout
        val listaElementow = mapaKonstrukcji.keys.toMutableList()

        val lista_elementow_layout = findViewById<LinearLayout>(R.id.lista_elementow)

        var lista_wypelnionych = emptyList<String>().toMutableList()
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    myRef.addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            var data_ref = dataSnapshot.child(nr_kompleksu.toString()).child(nr_obiektu.toString())
                            var data = data_ref.getValue()
                            if (data != null) {
                                var lista_elementow = (data as Map<String, Any>).keys.toList()
                                for (element_ in lista_elementow) {
                                    if (data_ref.child(element_).child("zużycie").getValue() != null && data_ref.child(element_).child("konstrukcja").getValue() != null && data_ref.child(
                                            element_
                                        ).child("usterki").getValue() != null
                                    ) {
                                        lista_wypelnionych.add(element_)
                                    }
                                }
                            }

                            for (element in listaElementow) {
                                val button = Button(this@settingsActivity)
                                button.layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                button.setText(element)

                                if(mapaKonstrukcji.get(element)?.get(0) == "HEADER"){
                                    button.setBackgroundColor(Color.parseColor("#00ACC1"))
                                    lista_elementow_layout.addView(button)
                                    continue
                                } else if(element in lista_wypelnionych){
                                    button.setBackgroundColor(Color.GREEN)
                                }

//                Add event listener to the button so the list on the right will be filled with buttons
                                button.setOnClickListener {
                                    fill_form(nr_kompleksu, nr_obiektu, button.text.toString(), mapaKonstrukcji, mapaUsterek,myRef, lista_elementow_layout, button, myfile_save)
                                }
                                lista_elementow_layout.addView(button)
                            }
                            // start activity with "fundamenty" clicked

                            lista_elementow_layout.children.toList()[1].performClick()

                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
                } else {
                    for (element in listaElementow) {
                        val button = Button(this@settingsActivity)
                        button.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        button.setText(element)

                        if(mapaKonstrukcji.get(element)?.get(0) == "HEADER"){
                            button.setBackgroundColor(Color.parseColor("#00ACC1"))
                            lista_elementow_layout.addView(button)
                            continue
                        } else if(element in lista_wypelnionych){
                            button.setBackgroundColor(Color.GREEN)
                        }

//                Add event listener to the button so the list on the right will be filled with buttons
                        button.setOnClickListener {
                            fill_form(nr_kompleksu, nr_obiektu, button.text.toString(), mapaKonstrukcji, mapaUsterek,myRef, lista_elementow_layout, button, myfile_save)
                        }
                        lista_elementow_layout.addView(button)
                    }
                    lista_elementow_layout.children.toList()[1].performClick()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun fill_form(
        kompleks: Any?,
        obiekt: Any?,
        element_obiektu: String,
        mapaKonstrukcji: MutableMap<String, List<String>>,
        mapaUsterek: MutableMap<String, Map<String, String>>,
        myRef: DatabaseReference,
        layout_form: LinearLayout,
        button: Button,
        myfile_save: File
    ) {

        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    // Read from the database
                    myRef.addValueEventListener(object : ValueEventListener {
                        @RequiresApi(Build.VERSION_CODES.N)
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                var zuzycie_base = dataSnapshot.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("zużycie").getValue()

                                // TITLE
                                val title_form = findViewById<Button>(R.id.form_title) as Button
                                title_form.setText(getString(R.string.wybrano,element_obiektu))


                                // CHECKBOXES
                                val checkboxes_layout = findViewById<LinearLayout>(R.id.checkboxes)
                                checkboxes_layout.removeAllViews()
                                val lista_elementow_konstrukcji = mapaKonstrukcji.getValue(element_obiektu)
                                var lista_based = dataSnapshot.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("konstrukcja").getValue()
                                var check_list = emptyList<String>().toMutableList()
                                for (element_konstrukcji in lista_elementow_konstrukcji) {
                                    if(element_konstrukcji != ""){
                                        val checkbox = CheckBox(this@settingsActivity)
                                        if (lista_based != null) {
                                            if (element_konstrukcji in (lista_based as List<String>)) {
                                                checkbox.isChecked = true
                                                check_list.add(element_konstrukcji)
                                            }
                                        }
                                        checkbox.setText(element_konstrukcji)
                                        checkboxes_layout.addView(checkbox)
                                        checkbox.setOnClickListener {
                                            check_list = updateCheckList(check_list, checkbox)
                                        }
                                    }
                                }


                                // SEEKBAR
                                val seekbar = findViewById<SeekBar>(R.id.seekBar)

                                val progressText = findViewById<TextView>(R.id.progress_text)

                                if(zuzycie_base != null){
                                    zuzycie_base = zuzycie_base.toString()
                                    seekbar.setProgress(zuzycie_base.toInt(),true)
                                    progressText.setText(zuzycie_base)
                                } else{
                                    seekbar.setProgress(0,true)
                                    progressText.setText("0")
                                }

                                seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                                    override fun onProgressChanged(
                                        seekBar: SeekBar, progress: Int,
                                        fromUser: Boolean
                                    ) {
                                        progressText.setText(progress.toString())

                                    }

                                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                                    }

                                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                                    }
                                })

                                // CHECKBOXES USTERKI
                                val usterki_layout = findViewById<LinearLayout>(R.id.checkboxes_usterki)
                                usterki_layout.removeAllViews()
                                val lista_usterki = mapaUsterek.getValue(element_obiektu).keys
                                var check_list_usterki = emptyMap<String, List<String>>().toMutableMap()
                                var a = dataSnapshot.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("usterki").getValue()
                                for (usterka in lista_usterki) {
                                    val checkbox = CheckBox(this@settingsActivity)
                                    if(a != null){
                                        if (usterka in (a as Map<String,String>).keys){
                                            checkbox.isChecked = true
                                            var b = a as Map<String,List<String>>
                                            var c = b.get(usterka)

                                            check_list_usterki.set(usterka, c!!)
                                        }
                                    }
                                    checkbox.setText(usterka)
                                    usterki_layout.addView(checkbox)
                                    checkbox.setOnClickListener {
                                        if (checkbox.isChecked) {
                                            check_list_usterki = usterki_ilosc_input(checkbox, check_list_usterki)
                                        }
                                    }
                                }



                                // SAVE BUTTON
                                val save_button = findViewById<Button>(R.id.save_button)

                                save_button.setOnClickListener {
                                    val myRef = myRef
                                    if(check_list_usterki.isEmpty() && "nie dotyczy" !in check_list){
                                        //do sth
                                        alert_empty("Uzupełnij usterki!")
                                        return@setOnClickListener
                                    }
                                    if(progressText.text.toString() == "" && "nie dotyczy" !in check_list){
                                        //do sth
                                        alert_empty("Uzupełnij zużycie!")
                                        return@setOnClickListener
                                    }
                                    if(check_list.isEmpty()){
                                        //do sth
                                        alert_empty("Uzupełnij konstrukcje!")
                                        return@setOnClickListener
                                    }


                                    myfile_save.appendText(element_obiektu +" usterki:"+ check_list_usterki.toString())
                                    myfile_save.appendText(element_obiektu +" zużycie:"+ progressText.text.toString())
                                    myfile_save.appendText(element_obiektu +" konstrukcja:"+ check_list.toString())

                                    myRef.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("usterki").setValue(check_list_usterki)
                                    myRef.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("zużycie").setValue(progressText.text.toString())
                                    myRef.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("konstrukcja").setValue(check_list)

                                    //move to next element
                                    if(element_obiektu=="Estetyka obiektu i jego otoczenia"){
                                        val intent = Intent(this@settingsActivity, MainActivity::class.java)
                                        startActivity(intent)
                                    } else{
                                        var lista_guzikow = layout_form.children.toList()
                                        var starting_point = mapaKonstrukcji.keys.toList().indexOf(element_obiektu)

                                        for (guzik in lista_guzikow.slice(starting_point+1 .. lista_guzikow.size-1)){
                                            val b = guzik as Button
                                            if(b == button){
                                                continue
                                            }
                                            if(mapaKonstrukcji.get(b.text.toString())?.get(0) == "HEADER"){
                                                continue
                                            }else{
                                                button.setBackgroundColor(Color.GREEN)
                                                b.performClick()
                                                break
                                            }
                                        }
                                    }
                                }
            //                }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                        }
                    })
                } else {
                    // Failed to read value

                    // TITLE
                    val title_form = findViewById<Button>(R.id.form_title) as Button
                    title_form.setText(getString(R.string.wybrano,element_obiektu))


                    // CHECKBOXES

                    val checkboxes_layout = findViewById<LinearLayout>(R.id.checkboxes)
                    checkboxes_layout.removeAllViews()
                    val lista_elementow_konstrukcji = mapaKonstrukcji.getValue(element_obiektu)
                    var check_list = emptyList<String>().toMutableList()
                    for (element_konstrukcji in lista_elementow_konstrukcji) {
                        if(element_konstrukcji != "") {
                            val checkbox = CheckBox(this@settingsActivity)
                            checkbox.setText(element_konstrukcji)
                            checkboxes_layout.addView(checkbox)
                            checkbox.setOnClickListener {
                                check_list = updateCheckList(check_list, checkbox)
                            }
                        }
                    }


                    // SEEKBAR
                    val seekbar = findViewById<SeekBar>(R.id.seekBar)
                    val progressText = findViewById<TextView>(R.id.progress_text)

                    seekbar.setProgress(0,true)
                    progressText.setText("0")




                    seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar, progress: Int,
                            fromUser: Boolean
                        ) {
                            progressText.setText(progress.toString())
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                        }
                    })

                    // CHECKBOXES USTERKI
                    val usterki_layout = findViewById<LinearLayout>(R.id.checkboxes_usterki)
                    usterki_layout.removeAllViews()
                    val lista_usterki = mapaUsterek.getValue(element_obiektu).keys
                    var check_list_usterki = emptyMap<String, List<String>>().toMutableMap()
                    for (usterka in lista_usterki) {
                        val checkbox = CheckBox(this@settingsActivity)
                        checkbox.setText(usterka)
                        usterki_layout.addView(checkbox)
                        checkbox.setOnClickListener {
                            if (checkbox.isChecked) {
                                check_list_usterki = usterki_ilosc_input(checkbox, check_list_usterki)
                            }
                        }
                    }

                    // SAVE BUTTON
                    val save_button = findViewById<Button>(R.id.save_button)
                    save_button.setOnClickListener {
                        val myRef = myRef
                        if(check_list_usterki.isEmpty() && "nie dotyczy" !in check_list){
                            //do sth
                            alert_empty("Uzupełnij usterki!")
                            return@setOnClickListener
                        }
                        if(progressText.text.toString() == "" && "nie dotyczy" !in check_list){
                            //do sth
                            alert_empty("Uzupełnij zużycie!")
                            return@setOnClickListener
                        }
                        if(check_list.isEmpty()){
                            //do sth
                            alert_empty("Uzupełnij konstrukcje!")
                            return@setOnClickListener
                        }

                        // save local copy of the file

                        myfile_save.appendText(element_obiektu +" usterki:"+ check_list_usterki.toString())
                        myfile_save.appendText(element_obiektu +" zużycie:"+ progressText.text.toString())
                        myfile_save.appendText(element_obiektu +" konstrukcja:"+ check_list.toString())

                        myRef.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("usterki").setValue(check_list_usterki)
                        myRef.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("zużycie").setValue(progressText.text.toString())
                        myRef.child(kompleks.toString()).child(obiekt.toString()).child(element_obiektu).child("konstrukcja").setValue(check_list)

                        //move to next element
                        if(element_obiektu=="Estetyka obiektu i jego otoczenia"){
                            val intent = Intent(this@settingsActivity, MainActivity::class.java)
                            startActivity(intent)
                        } else{
                            var lista_guzikow = layout_form.children.toList()
                            var starting_point = mapaKonstrukcji.keys.toList().indexOf(element_obiektu)

                            for (guzik in lista_guzikow.slice(starting_point+1 .. lista_guzikow.size-1)){
                                val b = guzik as Button
                                if(b == button){
                                    continue
                                }
                                if(mapaKonstrukcji.get(b.text.toString())?.get(0) == "HEADER"){
                                    continue
                                }else{
                                    button.setBackgroundColor(Color.GREEN)
                                    b.performClick()
                                    break
                                }
                            }
                        }
                    }
                }}
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    fun alert_empty(msg: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("UWAGA")
        builder.setMessage(msg)
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton("Ok") { dialog, which ->
            Toast.makeText(applicationContext,
                "Ok", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Anuluj") { dialog, which ->
            Toast.makeText(applicationContext,
                "Anuluj", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    fun updateCheckList(check_list: MutableList<String>, checkBox: CheckBox): MutableList<String> {
        var new_list = check_list.toMutableList()
        if (checkBox.isChecked) {
            if (checkBox.text !in new_list) {
                new_list.add(checkBox.text.toString())
            }
        } else {
            if (!checkBox.isChecked) {
                new_list.remove(checkBox.text)
            }
        }
        return (new_list)
    }


    fun usterki_ilosc_input(checkBox: CheckBox, mapa: MutableMap<String, List<String>>): MutableMap<String, List<String>> {
        val usterka = checkBox.text.toString()
        val mapa_usterki = mapa
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.activity_ask, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)
        val alert_title = dialogLayout.findViewById<TextView>(R.id.alert)
        alert_title.setText(getString(R.string.alert, usterka))

        var pilnosc = ""
        val radio_group = dialogLayout.findViewById<RadioGroup>(R.id.radio_group)
        radio_group.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { group, checkedId ->
                val radio: RadioButton = dialogLayout.findViewById(checkedId)
                pilnosc = radio.text.toString()
            })

        builder.setView(dialogLayout)
        var ilosc = ""
        builder.setPositiveButton("OK") { _, i ->
            // add value to dictionary
            ilosc = editText.text.toString()
            mapa_usterki[usterka] = listOf<String>(ilosc,pilnosc)

        }
        builder.setNeutralButton("Anuluj") { _, _ ->
            checkBox.isChecked = false
        }
        builder.show()
        return mapa_usterki
    }
}

//TODO:
//
//        5. kolorowac guziki z obiektami jak sa juz zrobione


