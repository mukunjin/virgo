package com.virgo.cubetimer.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virgo.cubetimer.ui.theme.VirgoColors

/** 关于界面：项目简介 + 致谢 + 作者信息。 */
@Composable
fun AboutScreen() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "一个简易轻巧的Android原生三阶魔方计时器。Kotlin+Jetpack Compose实现。",
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("致谢")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "csTimer",
            color = VirgoColors.OnBackground,
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "打乱与计时逻辑参考并复刻自 csTimer。",
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("作者")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Virgo 作者 mukunjin",
            color = VirgoColors.OnBackground,
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "仓库：https://github.com/mukunjin/virgo",
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = VirgoColors.OnBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    )
}