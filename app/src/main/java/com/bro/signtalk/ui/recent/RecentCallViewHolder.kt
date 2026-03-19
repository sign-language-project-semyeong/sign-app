package com.bro.signtalk.ui.recent

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R

class RecentCallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvName: TextView = view.findViewById(R.id.tv_name)
    val tvTime: TextView = view.findViewById(R.id.tv_time)
    val layoutExpand: LinearLayout = view.findViewById(R.id.layout_expand)

    // 버튼들도 여기서 똬악 잡아라 이말이야!
    val btnCall: View = view.findViewById(R.id.btn_item_call)
    val btnMessage: View = view.findViewById(R.id.btn_item_message)
    val btnVideo: View = view.findViewById(R.id.btn_item_video)
}