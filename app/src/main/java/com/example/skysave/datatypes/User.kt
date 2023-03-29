package com.example.skysave.datatypes

import com.google.firebase.firestore.DocumentReference


data class User(val uid: String, val email: String, val alias: String, val files: List<Map<DocumentReference, Boolean>>) : java.io.Serializable