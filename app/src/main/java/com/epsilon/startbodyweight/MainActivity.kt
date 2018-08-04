package com.epsilon.startbodyweight

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.epsilon.startbodyweight.data.DBHelperUtil
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.notif.NotificationUtil
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class MainActivity : AppCompatActivity() {
    val LTAG = MainActivity::class.qualifiedName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    fun startWorkoutActivity(v: View) {
        doAsync {
            val isInitialized = DBHelperUtil.isDbInitialized(RoomDB.get(weakRef.get()))
            uiThread {
                if (isInitialized) {
                    Log.i(LTAG, "Our DB is initialized, start workout")
                    startActivity(Intent(it, WorkoutActivity::class.java))
                } else {
                    Log.i(LTAG, "Our DB is not initialized, select our workouts")
                    startActivity(Intent(it, SelectorActivity::class.java))
                }
            }
        }
    }

    fun nukeWorkoutData(v: View) {
        doAsync {
            DBHelperUtil.nukeDatabase(RoomDB.get(weakRef.get()))

            uiThread {
                Log.i(LTAG, "Data in DB deleted")
                Toast.makeText(it, "Data deleted", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun reselectExercises(v: View) {
        val intent = Intent(this, SelectorActivity::class.java)
        intent.putExtra("SELECTED_FROM_MAIN_MENU", true)

        doAsync {
            val isDbInitialized = DBHelperUtil.isDbInitialized(RoomDB.get(weakRef.get()))
            uiThread {
                if (isDbInitialized) {
                    intent.putExtra("LOAD_FROM_DB", true)
                }
                startActivity(intent)
            }
        }
    }

    @SuppressLint("NewApi")
    fun testMisc(v: View) {
        NotificationUtil.scheduleNotification(this, "MY NOTIF LOL", "YOU ARE GREAT!!!")
        NotificationUtil.scheduleNotification(this, "MY NOTIF LOL", "YOU ARE GREAT!!!", 30)
    }
}
