package com.bro.signtalk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bro.signtalk.R

class ContactsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // [수정] R.id가 아니라 R.layout이다 이말이야 유남생?!?
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }
}