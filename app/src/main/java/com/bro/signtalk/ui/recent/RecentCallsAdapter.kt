package com.bro.signtalk.ui.recent

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.RecentCall

class RecentCallsAdapter(private val calls: List<RecentCall>) :
    RecyclerView.Adapter<RecentCallViewHolder>() { // 우리가 만든 뷰홀더로 교체!

    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentCallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_call, parent, false)
        return RecentCallViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentCallViewHolder, position: Int) {
        val call = calls[position]
        val isExpanded = position == expandedPosition

        // 1. 기본 정보 세팅
        holder.tvName.text = if (call.name.isEmpty()) call.phoneNumber else call.name
        holder.tvTime.text = call.callTime

        // 2. 확장 레이아웃 가시성 제어 (이게 찐이지 유남생?!?)
        holder.layoutExpand.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // 3. 아이템 클릭 시 쫀득하게 펼치기
        holder.itemView.setOnClickListener {
            val prevExpandedPosition = expandedPosition
            expandedPosition = if (isExpanded) -1 else position

            // 효율적으로 바뀐 놈들만 다시 그려라 이말이야!
            if (prevExpandedPosition != -1) notifyItemChanged(prevExpandedPosition)
            notifyItemChanged(position)
        }

        // 4. 확장된 버튼들 클릭 리스너 연결!
        holder.btnCall.setOnClickListener {
            Log.d("RecentCall", "${call.phoneNumber}로 일반 전@화 쏜다 이말이야!")
            // 여기에 Telecom Manager 호출 로직 나중에 추가하자!
        }

        holder.btnVideo.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, com.bro.signtalk.ui.CallActivity::class.java)
            context.startActivity(intent)
            Log.d("RecentCall", "수@어 영@상통화 화면 소환 완@료 유남생?!?")
        }
    }

    override fun getItemCount() = calls.size
}