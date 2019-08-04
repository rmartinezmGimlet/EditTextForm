package com.gimlet.edittextform

import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.RequiresApi
import kotlin.properties.Delegates

class EditTextForm : EditText {

    constructor(context: Context): super(context, null)

    constructor(context: Context, attrs: AttributeSet?):
            super(context, attrs, android.R.attr.editTextStyle)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr, 0)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes)


    /**
     *
     */
    enum class Type {
        NONE,
        TEXT,
        NUMBER,
        EMAIL;
    }

    companion object {

        /**
         * Check the EditTextForm sent by param; if the verification was correct then [valid] are
         * invoked otherwise [invalid] are invoked
         * @param editTextForm EditTextForm to check
         * @param valid Function to invoke if the check was correct
         * @param invalid Function to invoke if the check was incorrect
         */
        fun check(editTextForm: EditTextForm, valid: () -> Unit, invalid: () -> Unit ) {
            val verifyFunction: (Boolean) -> Unit = {
                if(it) valid.invoke() else invalid.invoke()
            }
            editTextForm.onValidChangeListener = verifyFunction
        }

        /**
         * Check every EditTextForm in [list]; if the verification was correct then [valid] are
         * invoked otherwise [invalid] are invoked
         * @param list List of EditTextForm to check
         * @param valid Function to invoke if the check was correct
         * @param invalid Function to invoke if the check was incorrect
         */
        fun checkAll(list: List<EditTextForm>, valid: () -> Unit, invalid: () -> Unit ) {
            val verifyFunction: (Boolean?) -> Unit = {
                val isAValidList = list.all { it.isValid }
                if(isAValidList) valid.invoke()
                else invalid.invoke()
            }
            verifyFunction(null)
            list.forEach { it.onValidChangeListener = verifyFunction }
        }

        /**
         * Initialize every EditTextForm in [list] with [template]
         * @param template
         * Template to assign to each EditTextForm
         * @param list
         * List of EditTextForm to initialize
         */
        fun applyTemplate(template: Template, list: List<EditTextForm>){
            list.forEach { it.initializeFromTemplate(template) }
        }

        /**
         * Clear every EditTextForm in [list]
         * @param list
         * List of EditTextForm to clear
         */
        fun clearAll(list: List<EditTextForm>){
            val template = Template.Builder()
                    .setType(Type.NONE, null)
                    .setOnFocusChangeError(false)
                    .build()
            applyTemplate(template, list)
        }

    }


    /* Type verification to this EditText */
    var type: Type? = Type.NONE

    /* List with extra validations and its respective errors */
    private val extraValidations: ArrayList<Pair<(String) -> Boolean, Int>> = ArrayList()

    var onValidChangeListener: ((Boolean) -> Unit)? = null
    /* Define if this input is valid or not depending on all the validations added to this */
    var isValid: Boolean by Delegates.observable(false){ _, oldValue, newValue ->
        if(oldValue != newValue) onValidChangeListener?.invoke(newValue)
    }

    /* Error to show */
    var errorResource: Int? = null
    /* Type error to show */
    var typeErrorResource: Int? = null
    /* Function to display a personalized error */
    var displayNormalFunction: ((EditTextForm) -> Unit)? = null
    var displayErrorFunction: ((EditTextForm) -> Unit)? = null

    /* Watcher that verify the input every time the text changes */
    private val watcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            displayNormalFunction?.invoke(this@EditTextForm)
            verify()
        }
        override fun afterTextChanged(s: Editable?) { /* Empty implementation */ }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            /* Empty implementation */
        }
    }

    /* Focus Listener to show an error when this EditText haven't the focus */
    private val onFocusChangeListenerShowError = OnFocusChangeListener { _, hasFocus ->
        if (hasFocus) verify()
        if((errorResource != null) && !hasFocus) showError()
    }

    // Action listener used when this EditText has the ActionDone to verify data
    private val onEditorActionListenerShowError = OnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            verify()
            if(errorResource != null) showError()
        }
        false
    }

    /* Indicates if the EditText would show the error when haven't the focus and the error exists. */
    var onFocusChangeError: Boolean by Delegates.observable(false, { _, _, value ->
        this.onFocusChangeListener = if(value) onFocusChangeListenerShowError else null
        this.setOnEditorActionListener(if(value) onEditorActionListenerShowError else null)
    })

    init { this.addTextChangedListener(watcher) }

    /**
     * Set a verification type and the error to show if the verification it's wrong.
     * @param type [Type] to verify the input data.
     * @param typeErrorResource Error to show if the verification it's wrong.
     */
    fun setType(type: Type, typeErrorResource: Int){
        this.type = type
        this.typeErrorResource = typeErrorResource
    }

    /**
     * Initialize the EditTextForm with a template
     * @param template
     * [Template] from which we get the information
     */
    fun initializeFromTemplate(template: Template){
        type = template.type ?: Type.NONE
        typeErrorResource = template.typeErrorResource
        onFocusChangeError = template.onFocusChangeError ?: false
        displayErrorFunction = template.displayErrorFunction
        displayNormalFunction = template.displayNormalFunction
        extraValidations.clear()
        extraValidations.addAll(template.extraValidations)
    }

    /**
     * Add a extra validation to EditText with its respective error to show if this fails.
     * @param validation
     * Validation that indicates if the input is correct or not.
     * @param error
     * Error to show if this validation fails.
     */
    fun addValidation(validation: (String) -> Boolean, error: Int) {
        extraValidations.add(Pair(validation, error))
    }

    /**
     * Verify the input data.
     */
    private fun verify(){
        val extrasValid = verifyExtras()
        val typeValid = when (type){
            Type.TEXT -> verifyText()
            Type.NUMBER -> verifyNumber()
            Type.EMAIL -> verifyEmail()
            else -> true
        }
        if (!typeValid) errorResource = typeErrorResource
        isValid = extrasValid && typeValid
        if (isValid) errorResource = null
    }

    /**
     * Verify that the input data hasn't empty
     */
    private fun verifyText() : Boolean = text.toString().trim().isNotEmpty()

    /**
     * Verify that the input data contains only numbers
     */
    private fun verifyNumber() : Boolean =
            text.matches(NUMBER_VERIFICATION) and text.toString().trim().isNotEmpty()

    /**
     * Verify that the input data be a valid email.
     */
    private fun verifyEmail() : Boolean = text.toString().matches(EMAIL_VERIFICATION)

    /**
     * Check each validation in [extraValidations].
     */
    private fun verifyExtras(): Boolean {
        extraValidations.forEach {
            val result = it.first(this.text.toString())
            if (!result) {
                errorResource = it.second
                return false
            }
        }
        errorResource = null
        return true
    }

    /**
     * Show the input error if this was different of null
     */
    fun showError(){
        if (errorResource != null) {
            val errorString = resources.getString(errorResource!!)
            if(displayErrorFunction != null)
                displayErrorFunction!!(this)
            else error = errorString
        }
    }

    /**
     * Template to initialize values of a EditTextForm
     */
    class Template {

        var type: Type? = null
        var typeErrorResource: Int? = null
        var displayNormalFunction: ((EditTextForm) -> Unit)? = null
        var displayErrorFunction: ((EditTextForm) -> Unit)? = null

        var onFocusChangeError: Boolean? = null
        val extraValidations: ArrayList<Pair<((String) -> Boolean), Int>> = ArrayList()


        class Builder {

            private val instance = Template()

            fun setType(type: Type, errorResource: Int?): Builder = apply {
                instance.type = type
                instance.typeErrorResource = errorResource
            }

            fun setDisplayErrorFunction(
                    normalFunction: ((EditTextForm) -> Unit),
                    errorFunction: ((EditTextForm) -> Unit)
            ) : Builder = apply {
                instance.displayNormalFunction = normalFunction
                instance.displayErrorFunction = errorFunction
            }

            fun addValidation(validation: (String) -> Boolean, error: Int): Builder = apply {
                instance.extraValidations.add(Pair(validation, error))
            }

            fun setOnFocusChangeError(onFocusChangeError: Boolean): Builder = apply {
                instance.onFocusChangeError = onFocusChangeError
            }

            fun build() : Template = instance
        }

    }

}