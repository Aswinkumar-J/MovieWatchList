package com.example.moviewatchlist.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser get() = firebaseAuth.currentUser

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(name: String, email: String, password: String) {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user
        if (user != null) {
            val userData = hashMapOf(
                "name" to name,
                "email" to email,
                "uid" to user.uid
            )
            firestore.collection("users").document(user.uid).set(userData).await()
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
