package com.bro.signtalk.ui.contacts

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.Contact
import com.google.android.material.bottomsheet.BottomSheetDialog

class ContactsAdapter(private var contacts: List<Contact>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() { // [주의] 공통 ViewHolder로 변경!

    private var expandedPosition = -1

    companion object {
        private const val TYPE_HEADER = 0 // 상단 "연락처" 글자 영역
        private const val TYPE_ITEM = 1   // 일반 연락처 아이템
    }

    // 1. 헤더가 추가됐으니 전체 개수는 데이터 개수 + 1이다 이말이야!
    override fun getItemCount(): Int = contacts.size + 1

    // 2. 0번 포지션만 헤더로 지정해라 유남생?!?
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            // [팩폭] 아까 만든 item_contact_header.xml 불러와라!
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
            ContactViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ContactViewHolder) {
            // [중요] 헤더가 0번을 먹었으니 데이터는 position - 1에서 가져와야 한다!
            val contactPosition = position - 1
            val contact = contacts[contactPosition]
            val isExpanded = contactPosition == expandedPosition

            holder.tvName.text = contact.name
            holder.tvNumber.text = contact.phoneNumber
            holder.tvProfile.text = if (contact.name.isNotEmpty()) contact.name.take(1) else "?"

            // 클릭 시 확장 로직 (포지션 계산 주의해라 브로!)
            holder.layoutExpand.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                val prev = expandedPosition
                expandedPosition = if (isExpanded) -1 else contactPosition
                if (prev != -1) notifyItemChanged(prev + 1) // 헤더 때문에 +1 필수!
                notifyItemChanged(position)
            }

            holder.itemView.setOnLongClickListener {
                showOptionsBottomSheet(holder.itemView.context, contact, contactPosition)
                true
            }
        }
        // 헤더는 딱히 바인딩 할 거 없으면 비워둬라 이말이야!
    }

    // --- ViewHolder 정의 ---
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProfile: TextView = view.findViewById(R.id.tv_profile_icon)
        val tvName: TextView = view.findViewById(R.id.tv_contact_name)
        val tvNumber: TextView = view.findViewById(R.id.tv_contact_number)
        val layoutExpand: View = view.findViewById(R.id.layout_expand)
        val btnCall: View = view.findViewById(R.id.btn_item_call)
        val btnMsg: View = view.findViewById(R.id.btn_item_message)
        val btnVideo: View = view.findViewById(R.id.btn_item_video)
        val btnSettings: View = view.findViewById(R.id.btn_item_settings)
    }

    // --- 나머지 삭제 로직 등은 기존과 동일 (포지션만 +1 주의!) ---
    private fun showOptionsBottomSheet(context: Context, contact: Contact, position: Int) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_contact_options, null)
        view.findViewById<View>(R.id.menu_delete).setOnClickListener {
            showDeleteConfirmDialog(context, contact, position)
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showDeleteConfirmDialog(context: Context, contact: Contact, position: Int) {
        val etInput = EditText(context).apply {
            hint = "'삭제' 입력해라 브로!"
            setPadding(50, 40, 50, 40)
        }
        AlertDialog.Builder(context)
            .setTitle("정말 ${contact.name}을 삭제할 거냐?!?")
            .setView(etInput)
            .setPositiveButton("확인") { _, _ ->
                if (etInput.text.toString() == "삭제") {
                    deleteContact(context, contact.id, position)
                }
            }.show()
    }

    private fun deleteContact(context: Context, contactId: String, position: Int) {
        val mutableContacts = contacts as? MutableList<Contact>
        mutableContacts?.let {
            it.removeAt(position)
            notifyItemRemoved(position + 1) // 헤더 때문에 +1!
            Toast.makeText(context, "성@불 완료!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getContacts(): List<Contact> = contacts
}