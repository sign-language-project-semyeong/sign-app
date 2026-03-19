package com.bro.signtalk.ui.recent

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

// [팩폭] Fragment 상속 안 받으면 넌 그냥 일반 클래스일 뿐이다 브로!
class RecentCallsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 도화지(XML)를 먼저 깔아줘야 한다 이말이야!
        return inflater.inflate(R.layout.fragment_recent_calls, container, false)
    }

    // RecentCallsFragment.kt 내부 onViewCreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recent_calls_recycler)

        // [팩폭] 데이터 없으면 리스트 안 뜬다! 가짜 데이터 찰지게 넣어라!
        val dummyData = listOf(
            RecentCall(1, "김@삼@성", "010-1234-5678", "오후 1:11", "오늘", com.bro.signtalk.data.model.CallType.INCOMING),
            RecentCall(2, "", "010-9999-8888", "오전 10:05", "오늘", com.bro.signtalk.data.model.CallType.MISSED),
            RecentCall(3, "수@어 브@로", "010-5555-4444", "어제", "어제", com.bro.signtalk.data.model.CallType.OUTGOING)
        )

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        recyclerView.adapter = RecentCallsAdapter(dummyData) // 어@댑터 장착 완료!
    }
}