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

/** 关于窗格。 */
@Composable
fun AboutScreen() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Virgo 1.0",
            color = VirgoColors.OnBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "三阶魔方计时器（333）",
            color = VirgoColors.OnSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        AboutLine("打乱算法", "完全复刻 csTimer（随机态 + min2phase 逆解）")
        AboutLine("计时精度", "纳秒计时，显示到 0.01 秒")
        AboutLine("数据存储", "成绩保存在本机数据库，卸载即清除")
        AboutLine("网络", "完全离线，未申请联网权限")
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "成绩数据不会上传到任何服务器。",
            color = VirgoColors.Disabled,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AboutLine(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().height(46.dp)) {
        Text(text = title, color = VirgoColors.OnBackground, fontSize = 14.sp)
        Text(text = value, color = VirgoColors.OnSurfaceVariant, fontSize = 12.sp)
    }
}