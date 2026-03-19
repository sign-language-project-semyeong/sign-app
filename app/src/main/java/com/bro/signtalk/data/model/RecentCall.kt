// 위치: java/com/bro/signtalk/data/model/RecentCall.kt
package com.bro.signtalk.data.model

// [핵심] 데이터만 담는 깔끔한 그릇이다 이말이야!
data class RecentCall(
    val id: Long,
    val name: String,        // 이름
    val phoneNumber: String, // 번호
    val callTime: String,    // 시간 (오후 1:11)
    val date: String,        // 날짜 (오늘, 어제)
    val type: CallType       // 수신/발신/부재중 구분!
)

enum class CallType {
    INCOMING, OUTGOING, MISSED
}