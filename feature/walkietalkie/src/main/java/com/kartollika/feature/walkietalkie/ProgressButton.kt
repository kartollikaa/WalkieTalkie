package com.kartollika.feature.walkietalkie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressButton(
  modifier: Modifier = Modifier,
  loading: Boolean,
  idleContent: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  Button(
    modifier = modifier
      .animateContentSize()
      .width(128.dp),
    onClick = onClick
  ) {
    AnimatedVisibility(visible = loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = MaterialTheme.colors.onPrimary,
        strokeWidth = 2.dp
      )
    }
    AnimatedVisibility(visible = !loading) {
      idleContent()
    }
  }
}