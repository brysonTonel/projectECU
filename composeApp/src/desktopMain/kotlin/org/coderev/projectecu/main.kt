package org.coderev.projectecu

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

import androidx.compose.ui.window.singleWindowApplication

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.sp

fun main() = singleWindowApplication {
    DashboardScreen()
}

@Composable
fun DashboardScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PROJECT ECU (Diagnostic Tool) For Yamaha and Honda by codeRev",
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
    }
}