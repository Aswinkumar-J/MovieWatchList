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
import com.example.moviewatchlist.databinding.ActivityLoginBinding
import com.example.moviewatchlist.di.ServiceLocator
import com.example.moviewatchlist.data.repository.AuthRepository

import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val authRepository = ServiceLocator.provideAuthRepository()
        viewModel = AuthViewModel(authRepository)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.registerPrompt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            binding.loginButton.isEnabled = false
                            binding.loginButton.text = ""
                            binding.loadingIndicator.visibility = View.VISIBLE
                        }
                        is AuthState.Authenticated -> {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                        is AuthState.Error -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = "Login"
                            binding.loadingIndicator.visibility = View.GONE
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthState.Idle -> {
                            binding.loginButton.isEnabled = true
                            binding.loginButton.text = "Login"
                            binding.loadingIndicator.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
}
