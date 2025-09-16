package com.example.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smsforwarder.ui.theme.SMSForwarderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SMSForwarderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmsForwarderApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsForwarderApp() {
    val context = LocalContext.current
    var forwardNumber by remember { mutableStateOf("") }
    var newKeyword by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf(emptyList<String>()) }
    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (!hasPermissions) {
            Toast.makeText(context, "SMS 권한 필요", Toast.LENGTH_SHORT).show()
        }
    }

    // 초기 설정 로드
    LaunchedEffect(Unit) {
        val sharedPref = context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        forwardNumber = sharedPref.getString("forward_number", "") ?: ""
        keywords = sharedPref.getStringSet("keywords", emptySet())?.toList() ?: emptyList()

        hasPermissions = checkPermissions(context)
        if (!hasPermissions) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.SEND_SMS
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SMS 자동전달",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // 권한 상태
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (hasPermissions)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = if (hasPermissions) "✅ 권한 OK" else "❌ 권한 필요",
                modifier = Modifier.padding(12.dp)
            )
        }

        // 전화번호 설정
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("전달번호", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = forwardNumber,
                    onValueChange = { forwardNumber = it },
                    placeholder = { Text("01012345678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        saveForwardNumber(context, forwardNumber.trim())
                        Toast.makeText(context, "저장완료", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("저장")
                }
            }
        }

        // 키워드 추가
        Card {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("키워드", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        placeholder = { Text("키워드 입력") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val trimmed = newKeyword.trim()
                            if (trimmed.isNotEmpty() && trimmed !in keywords) {
                                val updated = keywords + trimmed
                                keywords = updated
                                saveKeywords(context, updated)
                                newKeyword = ""
                                Toast.makeText(context, "추가완료", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("추가")
                    }
                }
            }
        }

        // 키워드 목록
        if (keywords.isNotEmpty()) {
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("등록된 키워드 (${keywords.size}개)", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(keywords) { keyword ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = keyword)
                                TextButton(
                                    onClick = {
                                        val updated = keywords - keyword
                                        keywords = updated
                                        saveKeywords(context, updated)
                                        Toast.makeText(context, "삭제완료", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("×", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun checkPermissions(context: Context): Boolean {
    return arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS
    ).all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun saveForwardNumber(context: Context, number: String) {
    context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("forward_number", number)
        .apply()
}

private fun saveKeywords(context: Context, keywords: List<String>) {
    context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        .edit()
        .putStringSet("keywords", keywords.toSet())
        .apply()
}