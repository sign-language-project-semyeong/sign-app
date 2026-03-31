package com.bro.signtalk.ui.recent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.BlockedNumberContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.CallType
import com.bro.signtalk.data.model.RecentCall

class RecentCallsAdapter(private var originalCalls: List<RecentCall>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isSelectMode = false
    val selectedItems = mutableSetOf<RecentCall>()
    var selectListener: OnSelectModeListener? = null

    interface OnSelectModeListener {
        fun onStartSelectMode()
        fun onSelectionChanged(count: Int)
        fun onEndSelectMode()
    }

    private var expandedPosition = -1
    private val items = mutableListOf<Any>()

    companion object {
        private const val TYPE_MAIN_HEADER = 0
        private const val TYPE_DATE_SECTION = 1
        private const val TYPE_CALL_ITEM = 2
    }

    init { updateItems(originalCalls) }

    fun updateItems(newCalls: List<RecentCall>) {
        originalCalls = newCalls
        items.clear()
        items.add("MAIN_HEADER")
        var lastDate = ""
        newCalls.forEach { call ->
            if (call.dateGroup != lastDate) {
                items.add(call.dateGroup)
                lastDate = call.dateGroup
            }
            items.add(call)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
    override fun getItemViewType(position: Int) = when (val item = items[position]) {
        is String -> if (item == "MAIN_HEADER") TYPE_MAIN_HEADER else TYPE_DATE_SECTION
        else -> TYPE_CALL_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MAIN_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_recent_header, parent, false))
            TYPE_DATE_SECTION -> SectionViewHolder(inflater.inflate(R.layout.item_contact_section, parent, false))
            else -> RecentCallViewHolder(inflater.inflate(R.layout.item_recent_call, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is RecentCallViewHolder && item is RecentCall) {
            val context = holder.itemView.context

            // 1. [데이터 세팅] 기본 정보 똬악!
            val displayName = if (item.name.isNullOrBlank()) item.phoneNumber else item.name
            holder.tvName.text = displayName
            holder.tvTime.text = item.callTime

            // 차단 상태 체크 및 UI 반영
            item.isBlocked = checkIsBlocked(context, item.phoneNumber)
            holder.ivBlockStatus.visibility = if (item.isBlocked) View.VISIBLE else View.GONE
            holder.tvName.setTextColor(if (item.isBlocked) android.graphics.Color.RED else android.graphics.Color.BLACK)

            val callIcon = when(item.type) {
                CallType.INCOMING -> R.drawable.ic_call_incoming
                CallType.OUTGOING -> R.drawable.ic_call_outgoing
                else -> R.drawable.ic_call_missed
            }
            holder.ivCallType.setImageResource(callIcon)

            // 2. [선택 모드] UI 처리
            if (isSelectMode) {
                val isSelected = selectedItems.contains(item)
                holder.itemView.alpha = if (isSelected) 1.0f else 0.4f
                holder.itemView.setBackgroundColor(if (isSelected) 0x1A000000.toInt() else 0)
            } else {
                holder.itemView.alpha = 1.0f
                holder.itemView.setBackgroundColor(0)
            }

            // 3. [확장 레이아웃] 데이터 채우기 및 리스너 연결
            val isExpanded = position == expandedPosition && !isSelectMode
            holder.layoutExpand.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                holder.tvDetailName.text = displayName
                holder.tvDetailPhone.text = item.phoneNumber
                holder.tvDetailType.text = when(item.type) {
                    CallType.INCOMING -> "수신 전화"
                    CallType.OUTGOING -> "발신 전화"
                    else -> "부재중 전화"
                }

                // [필살기] tvDetailTime에 날짜와 시간을 쫀득하게 합쳐라!
                holder.tvDetailTime.text = "${item.dateGroup}  ${item.callTime}"

                // [핵심] 버튼 리스너 멱살 잡고 연결! (이게 진짜 핵심이다 유남생?!?)
                // 2. [쌈뽕] 일반 통화 버튼 (기존 로직 보강)
                holder.btnCall.setOnClickListener {
                    val phoneNumber = item.phoneNumber
                    Log.d("RecentCall", "$phoneNumber 브로에게 찰진 음성 통화 쏜다!")

                    // [팩폭] 그냥 Intent만 쏘면 화면이 안 뜰 수 있으니 CallActivity로 똬@악!
                    val intent = Intent(context, com.bro.signtalk.ui.CallActivity::class.java).apply {
                        putExtra("receiver_phone", phoneNumber)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(intent)

                    // 시스템 엔진에도 신호 쏴라!
                    val mainActivity = context as? com.bro.signtalk.MainActivity
                    mainActivity?.makeCarrierCall(phoneNumber)                }

                holder.btnMessage.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${item.phoneNumber}"))
                    context.startActivity(intent)
                }

                holder.btnBlock.setOnClickListener {
                    if (item.isBlocked) showUnblockConfirmDialog(context, item, position)
                    else showBlockConfirmDialog(context, item, position)
                }


                // 1. [필살기] 영상 통화 버튼 (이제 준비 중 아님! ㅇㅇ.)
                holder.btnVideo.setOnClickListener {
                    val phoneNumber = item.phoneNumber
                    val displayName = if (item.name.isNullOrBlank()) phoneNumber else item.name

                    Log.d("RecentCall", "$phoneNumber 브로에게 쌈뽕한 영상 통화 쏜다!")

                    // [쫀득] 우리가 만든 비서실 소환해서 0.1초 만에 영상 통화 발신!
                    com.bro.signtalk.ui.CallNavigation.startVideoCall(context, phoneNumber, displayName)
                }
            }

            // 4. [클릭 이벤트] 리스트 클릭/롱클릭 처리
            holder.itemView.setOnClickListener {
                if (isSelectMode) {
                    toggleSelection(item, position)
                } else {
                    val prev = expandedPosition
                    expandedPosition = if (isExpanded) -1 else position
                    if (prev != -1) notifyItemChanged(prev)
                    notifyItemChanged(position)
                }
            }

            holder.itemView.setOnLongClickListener {
                if (!isSelectMode) {
                    isSelectMode = true
                    selectListener?.onStartSelectMode()
                    toggleSelection(item, position)
                }
                true
            }

        } else if (holder is SectionViewHolder) {
            holder.tvLabel.text = item as String
        }
    }

    private fun toggleSelection(call: RecentCall, position: Int) {
        if (selectedItems.contains(call)) selectedItems.remove(call)
        else selectedItems.add(call)

        notifyItemChanged(position)
        selectListener?.onSelectionChanged(selectedItems.size)
        if (selectedItems.isEmpty()) exitSelectMode()
    }

    fun exitSelectMode() {
        isSelectMode = false
        selectedItems.clear()
        selectListener?.onEndSelectMode()
        notifyDataSetChanged()
    }

    fun deleteSelectedItems(context: Context) {
        val resolver = context.contentResolver
        selectedItems.forEach { call ->
            try {
                resolver.delete(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    "${android.provider.CallLog.Calls.NUMBER} = ? AND ${android.provider.CallLog.Calls.DATE} = ?",
                    arrayOf(call.phoneNumber, call.timestamp.toString())
                )
            } catch (e: Exception) {
                Log.e("DeleteError", "성불 실패!: ${e.message}")
            }
        }
        val newCalls = originalCalls.filter { it !in selectedItems }
        updateItems(newCalls)
        exitSelectMode()
    }

    private fun showBlockConfirmDialog(context: Context, call: RecentCall, position: Int) {
        AlertDialog.Builder(context).setTitle("번호 차단할 거냐?!?")
            .setMessage("${call.phoneNumber}을 차단할 거냐 유남생?!?")
            .setPositiveButton("차단") { _, _ ->
                if (modifyBlockStatus(context, call.phoneNumber, true)) {
                    call.isBlocked = true
                    notifyItemChanged(position)
                }
            }.setNegativeButton("취소", null).show()
    }

    private fun showUnblockConfirmDialog(context: Context, call: RecentCall, position: Int) {
        AlertDialog.Builder(context).setTitle("차단 해제할 거냐?!?")
            .setMessage("${call.phoneNumber}을 사면해 줄 거냐 이말이야?!?")
            .setPositiveButton("해제") { _, _ ->
                if (modifyBlockStatus(context, call.phoneNumber, false)) {
                    call.isBlocked = false
                    notifyItemChanged(position)
                }
            }.setNegativeButton("취소", null).show()
    }

    private fun modifyBlockStatus(context: Context, number: String, shouldBlock: Boolean): Boolean {
        return try {
            val resolver = context.contentResolver
            if (shouldBlock) {
                val values = android.content.ContentValues().apply {
                    put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                }
                resolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
            } else {
                resolver.delete(BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?", arrayOf(number))
            }
            true
        } catch (e: Exception) {
            Toast.makeText(context, "권한 에러다 브로!", Toast.LENGTH_SHORT).show()
            false
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tv_section_label)
    }
    class RecentCallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvTime: TextView = view.findViewById(R.id.tv_contact_time)
        val ivCallType: ImageView = view.findViewById(R.id.iv_call_type)
        val ivBlockStatus: ImageView = view.findViewById(R.id.iv_block_status)
        val layoutExpand: View = view.findViewById(R.id.layout_expand)
        val tvDetailName: TextView = view.findViewById(R.id.tv_detail_name)
        val tvDetailPhone: TextView = view.findViewById(R.id.tv_detail_phone)
        val tvDetailType: TextView = view.findViewById(R.id.tv_detail_type_time)
        val tvDetailTime: TextView = view.findViewById(R.id.tv_detail_type_time)
        val btnCall: View = view.findViewById(R.id.btn_item_call)
        val btnMessage: View = view.findViewById(R.id.btn_item_message)
        val btnVideo: View = view.findViewById(R.id.btn_item_video)
        val btnBlock: View = view.findViewById(R.id.btn_item_block)
    }

    private fun checkIsBlocked(context: Context, number: String): Boolean {
        val normalizedNumber = number.replace(Regex("[^0-9]"), "")
        return try {
            val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
            val cursor = context.contentResolver.query(uri,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ? OR " +
                        "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                arrayOf(number, normalizedNumber), null)
            val blocked = (cursor?.count ?: 0) > 0
            cursor?.close()
            blocked
        } catch (e: Exception) { false }
    }
}