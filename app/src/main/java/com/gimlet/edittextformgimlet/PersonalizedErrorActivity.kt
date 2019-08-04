package com.gimlet.edittextformgimlet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.gimlet.edittextform.EditTextForm
import kotlinx.android.synthetic.main.activity_personalized_error.*
import kotlin.properties.Delegates

class PersonalizedErrorActivity : AppCompatActivity() {

    private var enabledButton: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personalized_error)
        initializeViews()
    }

    private fun initializeViews() {
        val template = EditTextForm.Template.Builder()
                .setType(EditTextForm.Type.TEXT, R.string.obligatory_field)
                .setOnFocusChangeError(false)
                .setDisplayErrorFunction({
                    layout.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
                },{
                    layout.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
                })
                .build()
        etfPersonalizedError.initializeFromTemplate(template)

        EditTextForm.check(
                etfPersonalizedError,
                { enabledButton = true },
                { enabledButton = false }
        )
        btnNext.setOnClickListener {
            if(enabledButton)
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
            else
                etfPersonalizedError.showError()

        }
    }
}
