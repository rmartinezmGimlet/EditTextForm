package com.gimlet.edittextform

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import kotlin.properties.Delegates

class EditTextForm : EditText {

    /* Constructors to EditText */
    constructor(context: Context): super(context, null)

    constructor(context: Context, attrs: AttributeSet?):
            super(context, attrs, android.R.attr.editTextStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes)


    companion object {

        /* Common types of Verification */
        val NONE = -1
        val TEXT = 0
        val NUMBER = 1
        val EMAIL = 2

        /**
         * Check every EditTextForm in [list]; if the verification was correct then [valid] are
         * invoked otherwise [invalid] are invoked
         * @param list
         * List of EditTextForm to check
         * @param valid
         * Function to invoke if the check was correct
         * @param invalid
         * Function to invoke if the check was incorrect
         */
        fun checkAll(list: List<EditTextForm>, valid: () -> Unit, invalid: () -> Unit ) {
            val verify = {
                var allCorrect = true
                list.forEach {
                    allCorrect = allCorrect and it.isValid
                }
                if (allCorrect)
                    valid.invoke()
                else
                    invalid.invoke()
            }
            verify.invoke()
            list.forEach { it.stateChangeLister = verify }
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
                    .setType(EditTextForm.NONE)
                    .setTypeErrorResource(null)
                    .setOnFocusChangeError(false)
                    .build()
            applyTemplate(template, list)
        }

    }
    /* Type verification to this EditText */
    var type: Int? = EditTextForm.NONE

    /* List with extra validations and its respective errors */
    private val extraValidations: ArrayList<Pair<(String) -> Boolean, Int>> = ArrayList()

    var stateChangeLister: (() -> Unit)? = null
    /* Define if this input is valid or not depending on all the validations added to this */
    var isValid: Boolean by Delegates.observable(false, {
        _, oldValue, newValue ->
            if (oldValue != newValue)
                stateChangeLister?.invoke()
    })

    /* REGEX to verify a valid email */
    private val EMAIL_VERIFICATION = Regex("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")
    /* REGEX to verify a number input */
    private val NUMBER_VERIFICATION = Regex("[0-9]+")

    /* Error to show */
    var errorResource: Int? = null
    /* Type error to show */
    var typeErrorResource: Int? = null

    /* Watcher that verify the input every time the text changes */
    private val watcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { verify() }
        override fun afterTextChanged(s: Editable?) { }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
    }
    /* Focus Listener to show an error when this EditText haven't the focus */
    private val onFocusChangeListenerShowError = OnFocusChangeListener {
        _, hasFocus ->
        if (hasFocus) verify()
        if((errorResource != null) and !hasFocus) showError()

    }
    // Action listener used when this EditText has the ActionDone to verify data
    private val onEditorActionListenerShowError = OnEditorActionListener {
        _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            verify()
            if(errorResource != null) showError()
        }
        false
    }
    /* Indicates if the EditText would show the error when haven't the focus and the error exists. */
    var onFocusChangeError: Boolean by Delegates.observable(false, {
        _, _, newValue ->
        this.onFocusChangeListener = if(newValue) onFocusChangeListenerShowError else null
        this.setOnEditorActionListener(if(newValue) onEditorActionListenerShowError else null)
    })

    init { this.addTextChangedListener(watcher) }

    /**
     * Set a type of verification, the error to show if the verification wasn't correct and
     * a flag which indicates if the EditText would show the error when haven't the focus.
     * @param type
     * Basic type to verify the input data.
     * @param typeErrorResource
     * Error to show if the verification wasn't correct.
     */
    fun setType(type: Int, typeErrorResource: Int){
        this.type = type
        this.typeErrorResource = typeErrorResource
    }

    /**
     * Initialize the EditTextForm with a template
     * @param template
     * [Template] from which we get the information
     */
    fun initializeFromTemplate(template: Template){
        type = template.type ?: EditTextForm.NONE
        typeErrorResource = template.typeErrorResource
        onFocusChangeError = template.onFocusChangeError ?: false
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
        val aux = verifyExtras()
        val typeVerification = when (type){
            EditTextForm.TEXT -> verifyText()
            EditTextForm.NUMBER -> verifyNumber()
            EditTextForm.EMAIL -> verifyEmail()
            else -> true
        }
        if (!typeVerification) errorResource = typeErrorResource
        isValid = aux and typeVerification
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
        if (errorResource != null)
            error = resources.getString(errorResource!!)
    }

    /**
     * Template to initialize values of a EditTextForm
     */
    class Template {
        var type: Int? = null
        var typeErrorResource: Int? = null
        var onFocusChangeError: Boolean? = null
        val extraValidations: ArrayList<Pair<((String) -> Boolean), Int>> = ArrayList()


        class Builder {
            private val instance = Template()

            fun setType(type: Int): Builder {
                instance.type = type
                return this
            }
            fun setTypeErrorResource(typeErrorResource: Int?): Builder {
                instance.typeErrorResource = typeErrorResource
                return this
            }
            fun addValidation(validation: (String) -> Boolean, error: Int): Builder {
                instance.extraValidations.add(Pair(validation, error))
                return this
            }
            fun setOnFocusChangeError(onFocusChangeError: Boolean): Builder {
                instance.onFocusChangeError = onFocusChangeError
                return this
            }

            fun build() : Template = instance
        }

    }

}