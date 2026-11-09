package com.mryav.compacttrip.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.mryav.compacttrip.ui.theme.Palette
import com.mryav.compacttrip.ui.theme.Type
import com.mryav.compacttrip.ui.theme.du

/**
 * The app's single-line input row (API key, stop search). White row, grey placeholder, trailing
 * icon; focused: orange focus state with white text. Unfocused long values are ellipsised.
 * D-pad UP/DOWN always leave the field (keypad phones); LEFT/RIGHT move the cursor.
 * [below] renders inside the row under the text (e.g. the search screen's mode icons).
 */
@Composable
fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes trailingIcon: Int,
    keyboardOptions: KeyboardOptions,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    below: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val textColor = if (focused) Palette.FocusedContent else Palette.BodyText
    val iconColor = when {
        focused -> Palette.FocusedContent
        value.isEmpty() -> Palette.PlaceholderText
        else -> Palette.BodyText
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                val direction = when (event.key) {
                    Key.DirectionUp -> FocusDirection.Up
                    Key.DirectionDown -> FocusDirection.Down
                    else -> return@onPreviewKeyEvent false
                }
                if (event.type == KeyEventType.KeyDown) focusManager.moveFocus(direction)
                true
            }
            .focusHighlight(focused = focused, color = Palette.Card)
            .semantics { contentDescription = placeholder },
        singleLine = true,
        textStyle = Type.row(textColor),
        cursorBrush = SolidColor(textColor),
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { onDone() }, onSearch = { onDone() }),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier.padding(horizontal = 16.du, vertical = 20.du),
                verticalArrangement = Arrangement.spacedBy(4.du),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.du),
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        when {
                            value.isEmpty() -> {
                                innerTextField()
                                BasicText(placeholder, style = Type.row(if (focused) Palette.FocusedContent else Palette.PlaceholderText))
                            }
                            focused -> innerTextField()
                            else -> {
                                // Keep the real field laid out (it owns focus) but show an ellipsised copy.
                                Box(Modifier.alpha(0f)) { innerTextField() }
                                BasicText(value, style = Type.row(textColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Image(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconColor),
                        modifier = Modifier.size(40.du),
                    )
                }
                below?.invoke()
            }
        },
    )
}
