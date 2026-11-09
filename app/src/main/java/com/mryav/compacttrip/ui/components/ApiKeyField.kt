package com.mryav.compacttrip.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.mryav.compacttrip.R

/** API key input (Figma 22:1016): "API key" placeholder, trailing "code" icon, no autocorrect. */
@Composable
fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignTextField(
        value = value,
        // Pasted keys often carry spaces or line breaks.
        onValueChange = { onValueChange(it.filterNot(Char::isWhitespace)) },
        placeholder = "API key",
        trailingIcon = R.drawable.ic_code,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        onDone = onDone,
        modifier = modifier,
    )
}
