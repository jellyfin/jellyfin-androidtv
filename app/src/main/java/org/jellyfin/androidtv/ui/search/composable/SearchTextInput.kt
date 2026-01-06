package org.jellyfin.androidtv.ui.search.composable

import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent as AndroidKeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

@Composable
fun SearchTextInput(
	query: String,
	onQueryChange: (query: String) -> Unit,
	onQuerySubmit: () -> Unit,
	onKeyboardDismissed: () -> Unit = {},
	focusRequester: FocusRequester,
	autoShowIme: Boolean = false,
	canNavigateDown: Boolean = false,
	onNavigateDown: () -> Unit = {},
	suppressImeOnFocusSignal: Int = 0,
	onEditTextRef: (AppCompatEditText?) -> Unit = {},
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	val inputMethodManager = remember { context.getSystemService<InputMethodManager>() }

	val onQueryChangeState = rememberUpdatedState(onQueryChange)
	val onQuerySubmitState = rememberUpdatedState(onQuerySubmit)
	val onKeyboardDismissedState = rememberUpdatedState(onKeyboardDismissed)
	val onNavigateDownState = rememberUpdatedState(onNavigateDown)
	val canNavigateDownState = rememberUpdatedState(canNavigateDown)
	val onEditTextRefState = rememberUpdatedState(onEditTextRef)
	val suppressImeOnFocusSignalState = rememberUpdatedState(suppressImeOnFocusSignal)

	val suppressTextChange = remember { mutableStateOf(false) }
	val editTextRef = remember { mutableStateOf<AppCompatEditText?>(null) }
	val lastImeVisible = remember { mutableStateOf<Boolean?>(null) }
	var editTextFocused by remember { mutableStateOf(false) }
	var rowFocused by remember { mutableStateOf(false) }
	var viewNodeFocused by remember { mutableStateOf(false) }
	var suppressImeOnce by remember { mutableStateOf(false) }

	val hasFocus = editTextFocused || rowFocused || viewNodeFocused
	val hasFocusState = rememberUpdatedState(hasFocus)
	val color = when {
		hasFocus -> JellyfinTheme.colorScheme.inputFocused to JellyfinTheme.colorScheme.onInputFocused
		else -> JellyfinTheme.colorScheme.input to JellyfinTheme.colorScheme.onInput
	}
	val shape = RoundedCornerShape(percent = 30)
	val borderWidth = if (hasFocus) 3.dp else 2.dp
	val backgroundColor = if (hasFocus) color.first.copy(alpha = 0.12f) else Color.Transparent
	val textColor = color.second.toArgb()

	fun resolveImeVisible(view: View): Boolean {
		val insets = ViewCompat.getRootWindowInsets(view)
		if (insets != null) return insets.isVisible(WindowInsetsCompat.Type.ime())
		val isActive = inputMethodManager?.isActive(view) == true
		val isAccepting = inputMethodManager?.isAcceptingText == true
		return isActive || isAccepting
	}

	fun updateImeVisibility(view: View) {
		val insets = ViewCompat.getRootWindowInsets(view)
		val imeVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
		val isActive = inputMethodManager?.isActive(view) == true
		val isAccepting = inputMethodManager?.isAcceptingText == true
		val visible = imeVisible || isActive || isAccepting
		if (lastImeVisible.value != visible) {
			if (lastImeVisible.value == true && !visible && (view.hasFocus() || hasFocusState.value)) {
				onKeyboardDismissedState.value()
			}
			lastImeVisible.value = visible
		}
	}

	fun ensureFocused(editText: AppCompatEditText) {
		if (!editText.isFocused) {
			editText.requestFocus()
			editText.requestFocusFromTouch()
		}
	}

	var showImeRequestId by remember { mutableStateOf(0) }

	fun showIme(editText: AppCompatEditText) {
		val requestId = ++showImeRequestId
		val startTimeMs = SystemClock.uptimeMillis()

		fun attemptShowIme(attempt: Int) {
			if (requestId != showImeRequestId) return

			ensureFocused(editText)

			if (!editText.isAttachedToWindow ||
				!editText.isShown ||
				!editText.hasWindowFocus() ||
				!editText.isFocused
			) {
				if (SystemClock.uptimeMillis() - startTimeMs >= 5_000) {
					return
				}
				editText.postDelayed({ attemptShowIme(attempt) }, 50)
				return
			}

			val implicitResult = inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT) ?: false
			if (!implicitResult) {
				inputMethodManager?.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
			}

			val controller = ViewCompat.getWindowInsetsController(editText)
			if (controller != null) {
				controller.show(WindowInsetsCompat.Type.ime())
			}

			editText.postDelayed({
				if (requestId != showImeRequestId) return@postDelayed
				updateImeVisibility(editText)
				if (!resolveImeVisible(editText) && attempt < 10) {
					attemptShowIme(attempt + 1)
				}
			}, 200)
		}

		editText.post { attemptShowIme(0) }
	}

	fun handleImeTrigger(event: ComposeKeyEvent): Boolean {
		if (event.type != KeyEventType.KeyDown) return false
		val keyCode = event.nativeKeyEvent.keyCode
		val isImeKey = when (event.key) {
			Key.DirectionCenter,
			Key.Enter,
			Key.NumPadEnter -> true
			else -> keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
				keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
				keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
		}
		if (!isImeKey) return false
		val editText = editTextRef.value ?: return false
		if (!resolveImeVisible(editText)) {
			showIme(editText)
			return true
		}
		return false
	}

	fun handleNavigateDown(event: ComposeKeyEvent): Boolean {
		if (event.type != KeyEventType.KeyDown) return false
		val keyCode = event.nativeKeyEvent.keyCode
		val isDown = event.key == Key.DirectionDown || keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
		if (!isDown || !canNavigateDownState.value) return false
		val editText = editTextRef.value ?: return false
		inputMethodManager?.hideSoftInputFromWindow(editText.windowToken, 0)
		onNavigateDownState.value()
		return true
	}

	var autoShowImeDone by remember { mutableStateOf(false) }
	LaunchedEffect(autoShowIme, editTextRef.value) {
		if (!autoShowIme || autoShowImeDone) return@LaunchedEffect
		val editText = editTextRef.value ?: return@LaunchedEffect
		autoShowImeDone = true
		editText.requestFocus()
		showIme(editText)
	}

	LaunchedEffect(suppressImeOnFocusSignalState.value) {
		if (suppressImeOnFocusSignalState.value > 0) {
			suppressImeOnce = true
		}
	}

	LaunchedEffect(editTextFocused, rowFocused, viewNodeFocused) {
		if (editTextFocused && !(rowFocused || viewNodeFocused)) {
			focusRequester.requestFocus()
		}
	}

	ProvideTextStyle(
		LocalTextStyle.current.copy(
			color = color.second,
			fontSize = 16.sp,
		)
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier
				.focusRequester(focusRequester)
				.focusable()
				.onFocusChanged { state ->
					rowFocused = state.hasFocus
					if (state.hasFocus) {
						editTextRef.value?.let { editText ->
							editText.requestFocus()
							editText.requestFocusFromTouch()
						}
					}
				}
				.onPreviewKeyEvent { event ->
					handleNavigateDown(event) || handleImeTrigger(event)
				}
				.background(backgroundColor, shape)
				.border(borderWidth, color.first, shape)
				.padding(12.dp)
		) {
			Icon(ImageVector.vectorResource(R.drawable.ic_search), contentDescription = null)
			Spacer(Modifier.width(12.dp))
			AndroidView(
				modifier = Modifier
					.weight(1f)
					.onFocusChanged { state ->
						viewNodeFocused = state.hasFocus
						if (state.hasFocus) {
							editTextRef.value?.let { editText ->
								if (!editText.isFocused) {
									editText.requestFocus()
								}
							}
						}
					}
					.onPreviewKeyEvent { event ->
						handleNavigateDown(event) || handleImeTrigger(event)
					},
				factory = { ctx ->
					val editText = object : AppCompatEditText(ctx) {
						private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
							updateImeVisibility(this)
						}

						override fun onAttachedToWindow() {
							super.onAttachedToWindow()
							viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
							ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
								updateImeVisibility(view)
								insets
							}
							ViewCompat.requestApplyInsets(this)
							onEditTextRefState.value(this)
							updateImeVisibility(this)
						}

						override fun onDetachedFromWindow() {
							val observer = viewTreeObserver
							if (observer.isAlive) observer.removeOnGlobalLayoutListener(layoutListener)
							ViewCompat.setOnApplyWindowInsetsListener(this, null)
							onEditTextRefState.value(null)
							super.onDetachedFromWindow()
						}

						override fun onKeyPreIme(keyCode: Int, event: AndroidKeyEvent): Boolean {
							if (keyCode == AndroidKeyEvent.KEYCODE_BACK &&
								event.action == AndroidKeyEvent.ACTION_UP
							) {
								inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
								onKeyboardDismissedState.value()
								updateImeVisibility(this)
								return true
							}
							return super.onKeyPreIme(keyCode, event)
						}
					}

					editTextRef.value = editText
					onEditTextRefState.value(editText)
					if (rowFocused || viewNodeFocused) {
						editText.requestFocus()
					}

					editText.apply {
						isFocusable = true
						isFocusableInTouchMode = true
						isSingleLine = true
						showSoftInputOnFocus = true
						imeOptions = EditorInfo.IME_ACTION_SEARCH
						inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
						setBackgroundColor(android.graphics.Color.TRANSPARENT)
						setPadding(0, 0, 0, 0)
						setTextColor(textColor)
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

						setOnFocusChangeListener { _, hasFocus ->
							editTextFocused = hasFocus
							updateImeVisibility(this)
							if (hasFocus) {
								if (suppressImeOnce) {
									suppressImeOnce = false
								} else {
									showIme(this)
								}
							}
						}

						setOnClickListener {
							showIme(this)
						}

						setOnKeyListener { _, keyCode, event ->
							if (event.action == AndroidKeyEvent.ACTION_DOWN &&
								keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN &&
								canNavigateDownState.value
							) {
								inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
								onNavigateDownState.value()
								true
							} else {
								val isDown = event.action == AndroidKeyEvent.ACTION_DOWN
								val isImeKey = keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
									keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
									keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
								if (isDown && isImeKey) {
									if (!resolveImeVisible(this)) {
										showIme(this)
										true
									} else {
										false
									}
								} else {
									false
								}
							}
						}

						setOnEditorActionListener { _, actionId, _ ->
							if (actionId == EditorInfo.IME_ACTION_SEARCH) {
								onQuerySubmitState.value()
								true
							} else {
								false
							}
						}

						addTextChangedListener(object : TextWatcher {
							override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
							override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
							override fun afterTextChanged(s: Editable?) {
								if (suppressTextChange.value) return
								val text = s?.toString().orEmpty()
								onQueryChangeState.value(text)
							}
						})
					}

					editText
				},
				update = { editText ->
					val newText = query
					if (editText.text?.toString() != newText) {
						suppressTextChange.value = true
						editText.setText(newText)
						editText.setSelection(newText.length)
						suppressTextChange.value = false
					}
					if (editText.currentTextColor != textColor) {
						editText.setTextColor(textColor)
					}
				},
			)
		}
	}
}
