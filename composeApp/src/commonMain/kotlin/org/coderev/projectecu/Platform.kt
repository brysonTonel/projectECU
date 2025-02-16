package org.coderev.projectecu

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform