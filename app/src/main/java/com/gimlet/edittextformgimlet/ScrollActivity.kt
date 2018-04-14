package com.gimlet.edittextformgimlet

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.gimlet.edittextform.EditTextForm
import kotlinx.android.synthetic.main.activity_scroll.*

class ScrollActivity : AppCompatActivity() {

    private val template = EditTextForm.Template.Builder()
            .setType(EditTextForm.TEXT)
            .setTypeErrorResource(R.string.obligatory_field)
            .setOnFocusChangeError(true)
            .addValidation({ !it.contains(" ") }, R.string.blank_spaces_error)
            .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll)

        val editTextList: List<EditTextForm> = listOf(etf1, etf2, etf3, etf4, etf5, etf6, etf7,
                etf8, etf9, etf10, etf11, etf12, etf13, etf14, etf15)

        EditTextForm.applyTemplate(template, editTextList)

        EditTextForm.checkAll(editTextList, { btnScrollNext.isEnabled = true },
                { btnScrollNext.isEnabled = false })
    }
}
