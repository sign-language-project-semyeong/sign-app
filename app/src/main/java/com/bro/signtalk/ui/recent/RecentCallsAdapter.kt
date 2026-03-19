package com.bro.signtalk.ui.recent

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.RecentCall

class RecentCallsAdapter(private val calls: List<RecentCall>) :
    RecyclerView.Adapter<RecentCallViewHolder>() {

    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentCallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_call, parent, false)
        return RecentCallViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentCallViewHolder, position: Int) {
        val call = calls[position]
        val isExpanded = position == expandedPosition

        // 1. [핵심] 연락처에서 삭제된 사람은 번호만 똬악!
        // 이름이 비어있으면 번호를 이름 자리에 박아버려라 이말이야 유남생?!?
        holder.tvName.text = if (call.name.isNullOrBlank()) {
            call.phoneNumber // 삭제된 놈은 번호로 박제!
        } else {
            call.name // 살아있는 놈은 이름으로!
        }

        holder.tvTime.text = call.callTime

        // 2. 확장 레이아웃 가시성 (쫀득하게!)
        holder.layoutExpand.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // 3. 클릭 시 펼치기 (효율적인 리프레시!)
        holder.itemView.setOnClickListener {
            val prev = expandedPosition
            expandedPosition = if (isExpanded) -1 else position
            if (prev != -1) notifyItemChanged(prev)
            notifyItemChanged(position)
        }

        // 4. [추가 요청] 롱클릭 시 삭제/차단 다이얼로그 소환!
        holder.itemView.setOnLongClickListener {
            // 여기에 삭제/차단 다이얼로그 로직 나중에 찰지게 박자!
            Toast.makeText(holder.itemView.context, "최근 기록 롱@클릭 유남생?!?", Toast.LENGTH_SHORT).show()
            true
        }

        // 5. 버튼 리스너 (수어 영상통화 등)
        holder.btnVideo.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, com.bro.signtalk.ui.CallActivity::class.java)
            context.startActivity(intent)
            Log.d("RecentCall", "수@어 영상통화 소환 완료!")
        }
    }

    override fun getItemCount() = calls.size
}