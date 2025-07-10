package com.itosfish.colorfeatureenhance.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 高亮显示匹配的文本
 * @param text 完整文本
 * @param query 搜索查询
 * @param style 文本样式
 * @param modifier 修饰符
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    if (query.isBlank()) {
        Text(text = text, style = style, modifier = modifier)
        return
    }
    
    val annotatedText = buildAnnotatedString {
        val normalizedText = text.lowercase()
        val normalizedQuery = query.trim().lowercase()
        
        var currentIndex = 0
        while (currentIndex < text.length) {
            val matchIndex = normalizedText.indexOf(normalizedQuery, currentIndex)
            if (matchIndex == -1) {
                // 如果没有找到匹配，添加剩余文本
                append(text.substring(currentIndex))
                break
            }
            
            // 添加匹配前的文本
            if (matchIndex > currentIndex) {
                append(text.substring(currentIndex, matchIndex))
            }
            
            // 添加高亮的匹配文本
            val matchEndIndex = matchIndex + normalizedQuery.length
            withStyle(SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )) {
                append(text.substring(matchIndex, matchEndIndex))
            }
            
            currentIndex = matchEndIndex
        }
    }
    
    Text(
        text = annotatedText,
        style = style,
        modifier = modifier
    )
} 