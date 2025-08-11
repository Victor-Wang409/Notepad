package cn.edu.tju.notepad

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.edit

class UserFragment : Fragment() {

    private lateinit var textViewUsername: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var buttonSettings: Button
    private lateinit var buttonAbout: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 为这个fragment填充布局
        val rootView = inflater.inflate(R.layout.fragment_user, container, false)

        // 初始化共享首选项
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // 初始化视图
        textViewUsername = rootView.findViewById(R.id.textViewUsername)
        textViewEmail = rootView.findViewById(R.id.textViewEmail)
        buttonSettings = rootView.findViewById(R.id.buttonSettings)
        buttonAbout = rootView.findViewById(R.id.buttonAbout)

        // 加载用户数据
        loadUserData()

        // 设置点击监听器
        setupClickListeners()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        // 刷新用户数据
        loadUserData()
    }

    private fun loadUserData() {
        // 获取存储的用户数据，如果不可用则使用默认值
        val username = sharedPreferences.getString("username", "未设置用户名") ?: "未设置用户名"
        val email = sharedPreferences.getString("email", "未设置邮箱") ?: "未设置邮箱"

        // 更新UI
        textViewUsername.text = username
        textViewEmail.text = email
    }

    private fun setupClickListeners() {
        // 设置按钮点击监听器
        buttonSettings.setOnClickListener {
            showUserSettingsDialog()
        }

        // 关于按钮点击监听器
        buttonAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showUserSettingsDialog() {
        // 创建用于编辑用户信息的对话框
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("个人信息设置")

        // 填充对话框布局
        val dialogView = LayoutInflater.from(requireActivity())
            .inflate(R.layout.dialog_user_settings, null)
        builder.setView(dialogView)

        // 获取对话框视图
        val editTextUsername = dialogView.findViewById<EditText>(R.id.editTextDialogUsername)
        val editTextEmail = dialogView.findViewById<EditText>(R.id.editTextDialogEmail)

        // 设置当前值
        editTextUsername.setText(sharedPreferences.getString("username", ""))
        editTextEmail.setText(sharedPreferences.getString("email", ""))

        // 设置按钮
        builder.setPositiveButton("保存") { _, _ ->
            // 获取输入值
            val username = editTextUsername.text.toString().trim()
            val email = editTextEmail.text.toString().trim()

            // 验证
            if (username.isEmpty()) {
                Toast.makeText(requireActivity(), "用户名不能为空", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // 保存到共享首选项
            sharedPreferences.edit {
            putString("username", username)
            putString("email", email)
        }

            // 更新UI
            loadUserData()

            Toast.makeText(requireActivity(), "保存成功", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("取消", null)

        // 显示对话框
        builder.create().show()
    }

    private fun showAboutDialog() {
        // 显示应用信息
        AlertDialog.Builder(requireActivity()).apply {
            setTitle("关于应用")
            setMessage("记事本应用\n版本: 1.0.0\n开发者: TJU")
            setPositiveButton("确定", null)
            create().show()
        }
    }
}