package com.popcorn.live.ui.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorn.live.ui.theme.CyanDeep
import com.popcorn.live.ui.theme.ElectricCyan
import com.popcorn.live.ui.theme.Obsidian
import com.popcorn.live.ui.theme.OutlineGhost
import com.popcorn.live.ui.theme.SurfaceHigh
import com.popcorn.live.ui.theme.SurfaceHighest
import com.popcorn.live.ui.theme.SurfaceLow
import com.popcorn.live.ui.theme.TextPrimary
import com.popcorn.live.ui.theme.TextSecondary

@Composable
fun ConfigScreen(
    state: ConfigUiState,
    onBaseUrlChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Obsidian, Color(0xFF101827), Obsidian),
                ),
            )
            .padding(32.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceLow.copy(alpha = 0.88f))
                .border(BorderStroke(1.dp, OutlineGhost), RoundedCornerShape(22.dp))
                .padding(24.dp),
        ) {
            Text(
                text = "POPCORN",
                color = ElectricCyan,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
            )
            Text(
                text = "CONNEXION XTREAM",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            ConfigTextField(
                value = state.baseUrl,
                label = "URL du serveur",
                placeholder = "https://example.com:8080",
                keyboardType = KeyboardType.Uri,
                onValueChanged = onBaseUrlChanged,
            )
            ConfigTextField(
                value = state.username,
                label = "Identifiant",
                placeholder = "username",
                keyboardType = KeyboardType.Text,
                onValueChanged = onUsernameChanged,
            )
            ConfigTextField(
                value = state.password,
                label = "Mot de passe",
                placeholder = "password",
                keyboardType = KeyboardType.Password,
                password = true,
                onValueChanged = onPasswordChanged,
            )
            state.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = ElectricCyan,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SaveButton(
                enabled = state.canSave,
                isSaving = state.isSaving,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun ConfigTextField(
    value: String,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onValueChanged: (String) -> Unit,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = SurfaceHigh,
            unfocusedContainerColor = SurfaceHigh.copy(alpha = 0.84f),
            cursorColor = ElectricCyan,
            focusedBorderColor = ElectricCyan,
            unfocusedBorderColor = OutlineGhost,
            focusedLabelColor = ElectricCyan,
            unfocusedLabelColor = TextSecondary,
            focusedPlaceholderColor = TextSecondary,
            unfocusedPlaceholderColor = TextSecondary,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val active = enabled && !isSaving
    val background = when {
        focused && active -> Brush.horizontalGradient(listOf(ElectricCyan, CyanDeep))
        active -> Brush.horizontalGradient(listOf(SurfaceHighest, SurfaceHigh))
        else -> Brush.horizontalGradient(listOf(SurfaceHigh.copy(alpha = 0.48f), SurfaceHigh.copy(alpha = 0.48f)))
    }
    val contentColor = if (focused && active) Color.Black else TextPrimary.copy(alpha = if (active) 1f else 0.52f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(background)
            .border(
                BorderStroke(if (focused && active) 2.dp else 1.dp, if (focused && active) ElectricCyan else OutlineGhost),
                shape,
            )
            .clickable(
                enabled = active,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onSave,
            )
            .focusable(enabled = active, interactionSource = interactionSource),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .height(20.dp)
                    .width(20.dp),
            )
        } else {
            Text(
                text = "Enregistrer",
                color = contentColor,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
        }
    }
}
