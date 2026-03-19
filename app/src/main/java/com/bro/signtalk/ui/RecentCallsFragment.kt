package com.bro.signtalk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.CallType
import com.bro.signtalk.data.model.RecentCall
import com.bro.signtalk.ui.recent.RecentCallsAdapter // [주의] 어@댑터 임포트 확인해라!

class RecentCallsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recent_calls, container, false)
    }

    // [핵심] 뷰@가 생성된 후 리스트를 조@지는 곳이다 이말이야!
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. RecyclerView 멱@살 잡기
        val recyclerView = view.findViewById<RecyclerView>(R.id.recent_calls_recycler)

        // 2. 가@짜 데이터 찰@지게 준비 (나중에 진짜 통화기록으로 바꿀 거다!)
        val dummyData = listOf(
            RecentCall(1, "김@삼@성", "010-1234-5678", "오후 1:11", "오늘", CallType.INCOMING),
            RecentCall(2, "수@어 브@로", "010-9999-8888", "오전 10:05", "오늘", CallType.MISSED),
            RecentCall(3, "개발자 형", "010-5555-4444", "어제", "어제", CallType.OUTGOING)
        )

        // 3. 식@탁(LayoutManager) 차리고 요@리(Adapter) 올리기!
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RecentCallsAdapter(dummyData)

        // [디버깅] 이@게 로그캣에 뜨는지 확인해라 브로!
        android.util.Log.d("RecentCall", "리스트 3@줄 쫀득하게 뿌렸다 유남생?!?")
    }
}