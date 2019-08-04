package com.gimlet.edittextformgimlet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.gimlet.edittextform.EditTextForm
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etfName.setType(EditTextForm.Type.TEXT, R.string.obligatory_field)
        etfName.onFocusChangeError = true
        etfEmail.setType(EditTextForm.Type.EMAIL, R.string.valid_email)
        etfEmail.onFocusChangeError = true
        etfCard.setType(EditTextForm.Type.NUMBER, R.string.only_numbers)
        etfCard.addValidation({ it.length == 16 }, R.string.card_length)
        etfCard.onFocusChangeError = true

        EditTextForm.checkAll(
                listOf<EditTextForm>(etfName, etfEmail, etfCard),
                { btnNext.visibility = View.VISIBLE },
                { btnNext.visibility = View.INVISIBLE }
        )

        btnNext.setOnClickListener {
            startActivity(Intent(this, ScrollActivity::class.java))
        }

    }
}
