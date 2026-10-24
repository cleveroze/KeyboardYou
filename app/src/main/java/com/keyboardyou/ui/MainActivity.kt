package com.keyboardyou.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keyboardyou.data.ClipboardRepository
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.data.WordRepository
import com.keyboardyou.theme.KeyShape
import kotlinx.coroutines.delay

enum class Screen(val title: String, val subtitle: String, val icon: ImageVector) {
    HOME("Keyboard You", "", Icons.Rounded.Keyboard),
    COLORS("Цвета и фон", "Material You, стекло, AMOLED", Icons.Rounded.Palette),
    SHAPES("Формы клавиш", "Печенье, клевер, сквиркл", Icons.Rounded.Category),
    FONT("Шрифт и размер", "Шрифты, высота, отступы", Icons.Rounded.TextFields),
    TYPING("Ввод текста", "Подсказки, курсор, ряды", Icons.Rounded.Edit),
    LANGUAGES("Языки", "Раскладки и переключение", Icons.Rounded.Language),
    FEEDBACK("Отклик и анимации", "Вибрация, звук, движение", Icons.Rounded.Vibration),
    CLIPBOARD("Буфер обмена", "История и закрепление", Icons.Rounded.ContentPaste),
    TRANSLATOR("Переводчик", "Офлайн-перевод прямо в клавиатуре", Icons.Rounded.Translate),
    ABOUT("О приложении", "Версия и сброс", Icons.Rounded.Info),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingsRepository.init(this)
        ClipboardRepository.init(this)
        WordRepository.init(this)
        enableEdgeToEdge()
        setContent { AppTheme { App() } }
    }
}

@Composable
private fun App() {
    val stack = remember { mutableStateListOf(Screen.HOME) }
    val current = stack.last()
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                val forward = targetState != Screen.HOME
                if (forward) {
                    (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 400f)) { it / 4 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(targetScale = 0.94f))
                } else {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.94f)) togetherWith
                        (slideOutHorizontally(spring(dampingRatio = 0.85f, stiffness = 400f)) { it / 4 } + fadeOut(tween(160)))
                }
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.HOME -> HomeScreen { stack.add(it) }
                else -> SubScreen(screen) { stack.removeAt(stack.lastIndex) }
            }
        }
    }
}

private fun Context.imeEnabled(): Boolean =
    getSystemService(InputMethodManager::class.java).enabledInputMethodList.any { it.packageName == packageName }

private fun Context.imeSelected(): Boolean =
    Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith("$packageName/") == true

@Composable
private fun HomeScreen(open: (Screen) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(context.imeEnabled()) }
    var selected by remember { mutableStateOf(context.imeSelected()) }
    LaunchedEffect(Unit) {
        while (true) {
            enabled = context.imeEnabled()
            selected = context.imeSelected()
            delay(800)
        }
    }
    var test by rememberSaveable { mutableStateOf("") }
    val tiles = Screen.entries.drop(1)
    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { Hero() }
        item {
            AnimatedVisibility(
                visible = !enabled || !selected,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
            ) {
                SetupCard(enabled, selected)
            }
        }
        item { CrashCard() }
        item {
            OutlinedTextField(
                value = test,
                onValueChange = { test = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Попробуйте клавиатуру здесь…") },
                leadingIcon = { Icon(Icons.Rounded.Keyboard, null) },
                shape = RoundedCornerShape(28.dp),
            )
        }
        val rows = tiles.chunked(2)
        rows.forEachIndexed { rowIndex, row ->
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEachIndexed { i, screen ->
                        val index = rowIndex * 2 + i
                        val (container, content) = tileColors(index)
                        PressableCard(
                            modifier = Modifier.weight(1f).height(150.dp),
                            color = container,
                            shape = RoundedCornerShape(if (index % 3 == 0) 36.dp else 26.dp),
                            onClick = { open(screen) },
                        ) {
                            IconBadge(
                                screen.icon,
                                container = content,
                                content = container,
                                shape = KeyShape.entries[index % KeyShape.entries.size],
                                size = 44,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(screen.title, style = MaterialTheme.typography.titleMedium, color = content)
                            Text(screen.subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f), maxLines = 2)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
}

@Composable
private fun tileColors(index: Int): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (index % 4) {
        0 -> c.primaryContainer to c.onPrimaryContainer
        1 -> c.secondaryContainer to c.onSecondaryContainer
        2 -> c.tertiaryContainer to c.onTertiaryContainer
        else -> c.surfaceContainerHighest to c.onSurface
    }
}

@Composable
private fun Hero() {
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MorphingLogo(size = 72) {
                Icon(Icons.Rounded.Keyboard, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(16.dp))
            // Заголовок «дышит» толщиной вариативного шрифта.
            val breath = androidx.compose.animation.core.rememberInfiniteTransition(label = "breath")
            val w by breath.animateFloat(
                400f, 900f,
                androidx.compose.animation.core.infiniteRepeatable(
                    androidx.compose.animation.core.tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "w",
            )
            Column {
                Text(
                    "Keyboard",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(w.toInt())),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "You",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(1300 - w.toInt())),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Выбирай свой шрифт, цвет и формы клавиш — клавиатура становится твоей.\nВерсия ${com.keyboardyou.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetupCard(enabled: Boolean, selected: Boolean) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Пара шагов до старта", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(12.dp))
            SetupStep(1, "Включите Keyboard You в списке клавиатур", enabled) {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Spacer(Modifier.height(8.dp))
            SetupStep(2, "Выберите её клавиатурой по умолчанию", selected, enabledButton = enabled) {
                context.getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            }
        }
    }
}

@Composable
private fun SetupStep(n: Int, text: String, done: Boolean, enabledButton: Boolean = true, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(done, label = "step") { isDone ->
                if (isDone) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                else IconBadge(Icons.Rounded.Keyboard, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, KeyShape.COOKIE_6, 30)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("$n. $text", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        if (!done) {
            if (n == 1) Button(onClick = onClick) { Text("Включить") }
            else FilledTonalButton(onClick = onClick, enabled = enabledButton) { Text("Выбрать") }
        }
    }
}

@Composable
private fun CrashCard() {
    val context = LocalContext.current
    var text by remember { mutableStateOf(com.keyboardyou.data.CrashLog.read(context)) }
    val crash = text ?: return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Клавиатура недавно упала", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(6.dp))
            Text(
                crash.lineSequence().take(8).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 8,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("crash", crash))
                    android.widget.Toast.makeText(context, "Скопировано — отправьте разработчику", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Скопировать отчёт") }
                FilledTonalButton(onClick = { com.keyboardyou.data.CrashLog.clear(context); text = null }) { Text("Скрыть") }
            }
        }
    }
}
