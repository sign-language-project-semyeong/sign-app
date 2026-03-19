package com.bro.signtalk.ui


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.Contact
import com.bro.signtalk.ui.contacts.ContactsAdapter
import com.bro.signtalk.ui.custom.SideIndexView // [주의] 임포트 확인해라!

class ContactsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.contacts_recycler)

        // 1. 권@한 체크 똬@악!
        checkContactPermissions()

        // 2. 옆@구리 바 멱@살 잡기!
        val sideIndexView = view.findViewById<SideIndexView>(R.id.side_index_view)
        sideIndexView.onIndexTouchListener = { letter, isVisible ->
            val tvOverlay = view?.findViewById<TextView>(R.id.tv_index_overlay) // 말풍선 멱살 잡기

            if (isVisible) {
                tvOverlay?.text = letter
                tvOverlay?.visibility = View.VISIBLE

                // 점프 로직은 이 안으로 드가야지!
                val adapter = recyclerView.adapter as? ContactsAdapter
                val contacts = adapter?.getContacts() ?: emptyList()
                val position = contacts.indexOfFirst { contact ->
                    if (letter == "★") contact.isFavorite
                    else getChosung(contact.name) == letter
                }
                if (position != -1) {
                    (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                }
            } else {
                tvOverlay?.visibility = View.GONE // 손 떼면 싹바가지 없게 사라지기!
            }
        }
    } // [핵심] 여기서 onViewCreated는 끝@이다 유남생?!?

    // --- 여기부터는 독립된 함수들이다 이말이야! ---

    private fun getChosung(name: String): String {
        val chosung = listOf("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
        val char = name.firstOrNull() ?: return "#"
        if (char in '가'..'힣') {
            val index = (char.code - 0xAC00) / 28 / 21
            return chosung[index]
        }
        return "#"
    }

    private fun checkContactPermissions() {
        val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissions(neededPermissions.toTypedArray(), 100)
        } else {
            displayContacts()
        }
    }

    private fun displayContacts() {
        val contacts = fetchContacts()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ContactsAdapter(contacts.toMutableList())
    }

    private fun fetchContacts(): List<Contact> {
        val contactList = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        val cursor = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null, null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

            while (it.moveToNext()) {
                val id = it.getString(idIndex) ?: ""
                val name = it.getString(nameIndex) ?: "이름 없음"
                val number = it.getString(numberIndex) ?: "번호 없음"
                val isFavorite = it.getInt(starredIndex) > 0
                contactList.add(Contact(id, name, number, isFavorite = isFavorite))
            }
        }
        return contactList.sortedWith(compareByDescending<Contact> { it.isFavorite }.thenBy { it.name })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            displayContacts()
        }
    }
}