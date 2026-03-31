package com.bro.signtalk.ui.recent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // [핵심] 이거 없으면 Toast에서 빨간 줄 뜬다 이말이야!
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bro.signtalk.R
import com.bro.signtalk.data.model.CallType
import com.bro.signtalk.data.model.RecentCall
import com.bro.signtalk.ui.DateUtils

// [팩폭] Fragment 상속 안 받으면 넌 그냥 일반 클래스일 뿐이다 브로!
class RecentCallsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecentCallsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recent_calls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recent_calls_recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        checkPermissionsAndLoad()
    }

    private fun checkPermissionsAndLoad() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED) {
            val realData = fetchCallLogs()
            adapter = RecentCallsAdapter(realData)
            recyclerView.adapter = adapter
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), 200)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadRecentCalls()
            Log.d("RecentCalls", "화면 복귀! 캘린더 날짜 쫀득하게 갱신 완료! ㅇㅇ.")
        }
    }

    private fun loadRecentCalls() {
        val calls = fetchCallLogs()
        adapter.updateItems(calls)
    }

    // [필살기] 중복 삭제하고 캘린더 엔진 꽂은 이 녀석 하나만 남겨라!
    private fun fetchCallLogs(): List<RecentCall> {
        val callList = mutableListOf<RecentCall>()
        val cursor = try {
            requireContext().contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                null, null, null,
                "${android.provider.CallLog.Calls.DATE} DESC"
            )
        } catch (e: SecurityException) { return emptyList() }

        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
            val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
            val typeIndex = it.getColumnIndex(android.provider.CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(android.provider.CallLog.Calls.DATE)

            while (it.moveToNext()) {
                val name = if (nameIndex != -1) it.getString(nameIndex) ?: "" else ""
                val number = if (numberIndex != -1) it.getString(numberIndex) ?: "번호 없음" else "번호 없음"
                val typeRaw = if (typeIndex != -1) it.getInt(typeIndex) else 1
                val timestamp = if (dateIndex != -1) it.getLong(dateIndex) else System.currentTimeMillis()

                val callType = when (typeRaw) {
                    android.provider.CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                    android.provider.CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    android.provider.CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    else -> CallType.INCOMING
                }

                // [쫀득] 아까 만든 DateUtils 엔진으로 캘린더 날짜 똬@악 낚아채기!
                val dateGroup = DateUtils.getDateGroupName(timestamp)

                callList.add(RecentCall(
                    phoneNumber = number,
                    name = name,
                    callTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREAN).format(timestamp),
                    type = callType,
                    dateGroup = dateGroup,
                    timestamp = timestamp
                ))
            }
        }
        return callList
    }
}