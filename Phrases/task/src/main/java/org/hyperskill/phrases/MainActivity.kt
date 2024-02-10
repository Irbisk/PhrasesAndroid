package org.hyperskill.phrases

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import org.hyperskill.phrases.databinding.ActivityMainBinding
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.random.Random

const val CHANNEL_ID = "org.hyperskill.phrases"


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var appDatabase: AppDatabase

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appDatabase = (application as SomeApplication).database

        val phrasesAdapter = PhrasesAdapter(
            appDatabase
                .getPhraseDao()
                .getAllPhrases()
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = phrasesAdapter
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Phrase"
            val descriptionText = "Your phrase"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        binding.reminderTextView.setOnClickListener {
            if (appDatabase.getPhraseDao().getAllPhrases().isEmpty()) {
                binding.reminderTextView.text = "No reminder set"
                Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show()
            } else {
                showTimePicker()
            }
        }


        binding.addButton.setOnClickListener {
            val contentView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_edit_text, null, false)
            AlertDialog.Builder(this)
                .setTitle("Add phrase")
                .setView(contentView)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val editText = contentView.findViewById<EditText>(R.id.editText)
                    if (editText.text.isNotEmpty()) {
                        appDatabase.getPhraseDao().insert(Phrase(editText.text.toString()))
                        phrasesAdapter.data = appDatabase.getPhraseDao().getAllPhrases()
                    }
                }
                .show()
        }

    }

    inner class PhrasesAdapter(data: MutableList<Phrase>) : RecyclerView.Adapter<PhrasesAdapter.PhraseViewHolder>() {

        var data: MutableList<Phrase> = data
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhraseViewHolder {
            return PhraseViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.items, parent, false))

        }

        override fun onBindViewHolder(holder: PhraseViewHolder, position: Int) {

            var phrase = data[position]
            holder.phrase.text = phrase.name


            holder.delete.setOnClickListener {
                appDatabase.getPhraseDao().delete(phrase)
                data = appDatabase.getPhraseDao().getAllPhrases()
                if (data.isEmpty()) {
                    val intent = Intent(applicationContext, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    val am: AlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                    am.cancel(pendingIntent)
                    binding.reminderTextView.text = "No reminder set"
                }

            }

        }

        override fun getItemCount(): Int {
            return data.size
        }

        inner class PhraseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val phrase = view.findViewById<TextView>(R.id.phraseTextView)
            val delete = view.findViewById<TextView>(R.id.deleteTextView)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val alarmCalendar = Calendar.getInstance()

        val myTimeListener = TimePickerDialog.OnTimeSetListener { timePicker, i, i2 ->

            if (timePicker.isShown) {
                val time = LocalTime.of(timePicker.hour, timePicker.minute)
                val dtf = DateTimeFormatter.ofPattern("HH:mm")
                binding.reminderTextView.text = "Reminder set for ${time.format(dtf)}"

                alarmCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                alarmCalendar.set(Calendar.MINUTE, timePicker.minute)
                if (alarmCalendar.before(calendar)) {
                    var dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
                    dayOfYear++
                    if (dayOfYear == 366) dayOfYear = 1
                    alarmCalendar.set(Calendar.DAY_OF_YEAR, dayOfYear)
                }
                val chosenTime = alarmCalendar.timeInMillis

                val intent = Intent(applicationContext, AlarmReceiver::class.java)
                val phrasesList = appDatabase.getPhraseDao().getAllPhrases()
                val phraseRandom = phrasesList[Random.nextInt(phrasesList.size)].name
                intent.putExtra("note", phraseRandom)
                intent.getStringExtra("note")?.let { Log.i("INTENT", it) }
                val pendingIntent = PendingIntent.getBroadcast(applicationContext, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)

                val am: AlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                am.setRepeating(AlarmManager.RTC_WAKEUP, chosenTime, AlarmManager.INTERVAL_DAY, pendingIntent)
            }
        }

        val timePickerDialog = TimePickerDialog(this, myTimeListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE), true)
        timePickerDialog.show()
    }
}



@Entity (tableName = "phrases")
data class Phrase(
    @ColumnInfo(name = "phrase") var name: String,
    @PrimaryKey(autoGenerate = true) var id: Int = 0
)

@Database(entities = [Phrase::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getPhraseDao(): PhraseDao
}

@Dao
interface PhraseDao {
    @Insert
    fun insert(vararg phrase: Phrase)

    @Delete
    fun delete(phrase: Phrase)

    @Query("DELETE FROM phrases")
    fun deleteAll()

    @Query("SELECT * FROM phrases")
    fun getAllPhrases(): MutableList<Phrase>

    @Query("SELECT * FROM phrases WHERE id = :id")
    fun getPhraseById(id: Int): Phrase

}

class SomeApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "phrases.db"
        ).allowMainThreadQueries().build()
    }
}
