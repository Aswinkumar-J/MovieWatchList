package com.example.moviewatchlist.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.moviewatchlist.MainActivity
import com.example.moviewatchlist.databinding.ActivityRegisterBinding
import com.example.moviewatchlist.di.ServiceLocator
import com.example.moviewatchlist.data.repository.AuthRepository

import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authRepository = ServiceLocator.provideAuthRepository()
        viewModel = AuthViewModel(authRepository)

        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(name, email, password)
        }

        binding.loginPrompt.setOnClickListener {
            finish()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            binding.registerButton.isEnabled = false
                            binding.registerButton.text = ""
                            binding.loadingIndicator.visibility = View.VISIBLE
                        }
                        is AuthState.Authenticated -> {
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        is AuthState.Error -> {
                            binding.registerButton.isEnabled = true
                            binding.registerButton.text = "Register"
                            binding.loadingIndicator.visibility = View.GONE
                            Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthState.Idle -> {
                            binding.registerButton.isEnabled = true
                            binding.registerButton.text = "Register"
                            binding.loadingIndicator.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}
