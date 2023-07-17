package org.testaco

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class TestacoExtension(configurer: TestacoConfiguration) : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        TODO("Not yet implemented")
    }
}
