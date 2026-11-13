package io.github.fallasleeptimetable.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class WelcomePage(
    val number: String,
    val title: String,
    val body: String,
)

private val welcomePages = listOf(
    WelcomePage(
        number = "01",
        title = "无广告，完全免费",
        body = "一个纯粹的课表工具。没有广告、没有订阅，也没有账号和后端。",
    ),
    WelcomePage(
        number = "02",
        title = "Kotlin 编写，Material 3",
        body = "原生 Kotlin 与 Jetpack Compose 构建，Material 3 设计，清爽的课堂帮手。",
    ),
    WelcomePage(
        number = "03",
        title = "三种教务，一次导入",
        body = "支持正方（新 / 旧）与强智教务系统；导入后，和同学轻松分享课表。",
    ),
    WelcomePage(
        number = "04",
        title = "接入系统待办，不占用后台",
        body = "课表写入系统日历与待办，提醒交给系统，无需常驻后台。",
    ),
)

@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { welcomePages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == welcomePages.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
        ) {
            AnimatedVisibility(
                visible = !isLastPage,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                TextButton(onClick = onFinish) {
                    Text("跳过")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "FallAsleep 课程表",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp,
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val pageOffset = (
                        (pagerState.currentPage - page) +
                            pagerState.currentPageOffsetFraction
                        )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val progress = abs(pageOffset).coerceIn(0f, 1f)
                                alpha = 1f - progress * 0.75f
                                translationX = -pageOffset * size.width * 0.18f
                                translationY = progress * 28.dp.toPx()
                                scaleX = 1f - progress * 0.05f
                                scaleY = 1f - progress * 0.05f
                            }
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = welcomePages[page].number,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = welcomePages[page].title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = welcomePages[page].body,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 28.sp,
                            modifier = Modifier.widthIn(max = 520.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(welcomePages.size) { index ->
                            val selected = pagerState.currentPage == index
                            val width by animateDpAsState(
                                targetValue = if (selected) 28.dp else 6.dp,
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                label = "welcomeDotWidth",
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onFinish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1,
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .widthIn(min = 140.dp),
                        shape = CircleShape,
                    ) {
                        AnimatedContent(
                            targetState = isLastPage,
                            transitionSpec = {
                                fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                            },
                            label = "welcomeButtonLabel",
                        ) { last ->
                            Text(if (last) "开始使用" else "下一步")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
