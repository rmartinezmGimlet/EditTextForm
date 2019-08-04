package com.gimlet.edittextform


/* REGEX to verify a valid email */
internal val EMAIL_VERIFICATION = Regex(
        "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
)

/* REGEX to verify a number input */
internal val NUMBER_VERIFICATION = Regex("[0-9]+")