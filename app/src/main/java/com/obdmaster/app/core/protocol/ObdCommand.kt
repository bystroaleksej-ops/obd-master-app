package com.obdmaster.app.core.protocol

open class ObdCommand(
    val command: String,
    val name: String,
    val description: String = ""
) {
    var rawResponse: String = ""
        protected set

    var formattedResult: String = ""
        protected set

    open fun parse(response: String) {
        this.rawResponse = response
        this.formattedResult = response
    }

    override fun toString(): String = "$name [$command]: $formattedResult"
}
